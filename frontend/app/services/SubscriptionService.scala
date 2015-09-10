package services

import com.github.nscala_time.time.Imports._
import com.gu.membership.model._
import com.gu.membership.salesforce.MemberId
import com.gu.membership.salesforce.Tier.{Supporter, Partner, Patron}
import com.gu.membership.stripe.Stripe
import com.gu.membership.util.{FutureSupplier, Timing}
import com.typesafe.scalalogging.LazyLogging
import forms.MemberForm.JoinForm
import model.{FreeEventTickets, FeatureChoice, MembershipSummary}
import model.Zuora._
import model.ZuoraDeserializer._
import org.joda.time.DateTime
import services.zuora.Rest.{ProductCatalog, Product, ProductRatePlanCharge}
import services.zuora._
import ZuoraServiceHelpers.formatDateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SubscriptionServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

trait AmendSubscription {
  self: SubscriptionService =>

  private def checkForPendingAmendments(memberId: MemberId)(fn: SubscriptionStatus => Future[AmendResult]): Future[AmendResult] = {
    getSubscriptionStatus(memberId).flatMap { subscriptionStatus =>
      if (subscriptionStatus.futureVersionIdOpt.isEmpty) {
        fn(subscriptionStatus)
      } else {
        throw SubscriptionServiceError("Cannot amend subscription, amendments are already pending")
      }
    }
  }

  def cancelSubscription(memberId: MemberId, instant: Boolean): Future[AmendResult] = {
    checkForPendingAmendments(memberId) { subscriptionStatus =>
      val currentSubscriptionVersion = subscriptionStatus.currentVersion
      for {
        subscriptionDetails <- getSubscriptionDetails(currentSubscriptionVersion)
        cancelDate = if (instant) DateTime.now else subscriptionDetails.chargedThroughDate.getOrElse(DateTime.now())
        result <- zuoraSoapService.authenticatedRequest(CancelPlan(currentSubscriptionVersion.id, subscriptionDetails.ratePlanId, cancelDate))
      } yield result
    }
  }

  def downgradeSubscription(memberId: MemberId, newTierPlan: TierPlan): Future[AmendResult] = {

    //if the member has paid upfront so they should have the higher tier until charged date has completed then be downgraded
    //otherwise use customer acceptance date (which should be in the future)
    def effectiveFrom(subscriptionDetails: SubscriptionDetails) = subscriptionDetails.chargedThroughDate.getOrElse(subscriptionDetails.contractAcceptanceDate)


    checkForPendingAmendments(memberId) { subscriptionStatus =>
      val currentSubscriptionVersion = subscriptionStatus.currentVersion
      for {
        subscriptionDetails <- getSubscriptionDetails(subscriptionStatus.currentVersion)
        dateToMakeDowngradeEffectiveFrom = effectiveFrom(subscriptionDetails)
        productRatePlanIds <- productRatePlanIdSupplier.get()
        result <- zuoraSoapService.authenticatedRequest(DowngradePlan(currentSubscriptionVersion.id, subscriptionDetails.ratePlanId,
          productRatePlanIds(newTierPlan), dateToMakeDowngradeEffectiveFrom))
      } yield result
    }
  }

  def upgradeSubscription(memberId: MemberId, newTierPlan: TierPlan, preview: Boolean, featureChoice: Set[FeatureChoice]): Future[AmendResult] = {
    import SubscriptionService._

    checkForPendingAmendments(memberId) { subscriptionStatus =>
      val subscriptionId = subscriptionStatus.currentVersionId
      for {
        zuoraFeatures <- zuoraSoapService.featuresSupplier.get()
        ratePlan <- zuoraSoapService.queryOne[RatePlan](s"SubscriptionId='$subscriptionId'")
        choice = featuresPerTier(zuoraFeatures)(newTierPlan, featureChoice)
        productRatePlanIds <- productRatePlanIdSupplier.get()
        result <- zuoraSoapService.authenticatedRequest(UpgradePlan(subscriptionId, ratePlan.id, productRatePlanIds(newTierPlan), preview, choice))
      } yield result
    }
  }
}

object SubscriptionService {
  /**
   * A Zuora subscription may have many versions as it is amended, some of which can be in the future (ie. downgrading
   * from a paid tier - because we don't refund that user, the downgrade is instead set to the point in the future when
   * their paid period ends).
   *
   * The Zuora API does not explicitly tell you what the *current* subscription version is. You have to work it out,
   * by looking at the 'amendments', finding the first amendment that has yet occurred. That amendment will give you the
   * id of the subscription it modified - and THAT will be the *current* subscription version.
   */
  def findCurrentSubscriptionStatus(subscriptionVersions: Seq[Subscription], amendments: Seq[Amendment]): SubscriptionStatus = {
    val firstAmendmentWhichHasNotYetOccurredOpt = // this amendment *will have modified the current subscription*
      sortAmendments(subscriptionVersions, amendments).find(_.contractEffectiveDate.isAfterNow)

    val latestSubVersion = subscriptionVersions.maxBy(_.version)

    firstAmendmentWhichHasNotYetOccurredOpt.fold(SubscriptionStatus(latestSubVersion, None, None)) { amendmentOfCurrentSub =>
      val currentSubId = amendmentOfCurrentSub.subscriptionId
      val currentSubVersion = subscriptionVersions.find(_.id == currentSubId).get
      SubscriptionStatus(currentSubVersion, Some(latestSubVersion), Some(amendmentOfCurrentSub.amendType))
    }
  }

  /**
   * @param subscriptions
   * @param amendments which are returned by the Zurora API in an unpredictable order
   * @return amendments which are sorted by the subscription version number they point to (the sub they amended)
   */
  def sortAmendments(subscriptions: Seq[Subscription], amendments: Seq[Amendment]) = {
    val versionsNumberBySubVersionId = subscriptions.map { sub => (sub.id, sub.version) }.toMap
    amendments.sortBy { amendment => versionsNumberBySubVersionId(amendment.subscriptionId) }
  }

  def sortInvoiceItems(items: Seq[InvoiceItem]) = items.sortBy(_.chargeNumber)

  def sortPreviewInvoiceItems(items: Seq[PreviewInvoiceItem]) = items.sortBy(_.price)

  def sortSubscriptions(subscriptions: Seq[Subscription]) = subscriptions.sortBy(_.version)

  def featuresPerTier(zuoraFeatures: Seq[Feature])(tier: ProductRatePlan, choice: Set[FeatureChoice]): Seq[Feature] = {
    def byChoice(choice: Set[FeatureChoice]) =
      zuoraFeatures.filter(f => choice.map(_.zuoraCode).contains(f.code))

    tier match {
      case PaidTierPlan(Patron, _) => byChoice(FeatureChoice.all)
      case PaidTierPlan(Partner, _) => byChoice(choice).take(1)
      case _ => Seq[Feature]()
    }
  }
}

class SubscriptionService(val zuoraSoapService: ZuoraSoapService,
                          val zuoraRestService: ZuoraRestService) extends AmendSubscription with LazyLogging {

  import SubscriptionService._
  val membershipProductType = "Membership"
  val productRatePlanChargeModel = "FlatFee"

  val productRatePlanTiers = List(FriendTierPlan, StaffPlan,
    PaidTierPlan(Supporter, true), PaidTierPlan(Supporter, false),
    PaidTierPlan(Partner, true), PaidTierPlan(Partner, false),
    PaidTierPlan(Patron, true), PaidTierPlan(Patron, false))

  def membershipProducts = zuoraRestService.productCatalogSupplier.get().map(_.productsOfType(membershipProductType))

  val productRatePlanIdSupplier = new FutureSupplier[Map[ProductRatePlan, String]](
    for {
      membershipProductCatalog <- membershipProducts
    } yield (for {
      productPlan <- productRatePlanTiers
    } yield productPlan -> extractRatePlanIdFromCatalog(productPlan, membershipProductCatalog)).toMap
  )

  protected def extractRatePlanIdFromCatalog(productRatePlan: ProductRatePlan, products: Seq[Product]): String = {
      val zuoraRatePlanId = for {
        product <- products.find(_.`Tier__c`.contains(productRatePlan.salesforceTier))
        zuoraRatePlan <- productRatePlan match {
          case FriendTierPlan | StaffPlan => product.activeRatePlans.headOption
          case paidTierPlan: PaidTierPlan =>
            val charge = ProductRatePlanCharge(productRatePlanChargeModel, Some(paidTierPlan.billingPeriod))
            product.activeRatePlans.find(_.productRatePlanCharges.contains(charge))
        }
      } yield zuoraRatePlan.id

      zuoraRatePlanId.getOrElse(throw new scala.RuntimeException(s"Rate plan id could not be found for $productRatePlan"))
  }

  private def subscriptionVersions(subscriptionNumber: String): Future[Seq[Subscription]] = for {
    subscriptions <- zuoraSoapService.query[Subscription](s"Name = '$subscriptionNumber'")
  } yield subscriptions

  def accountWithLatestMembershipSubscription(member: MemberId): Future[(Account, Rest.Subscription)] = for {
    accounts <- zuoraSoapService.query[Account](s"crmId='${member.salesforceAccountId}'")
    accountAndSubscriptionOpts <- Future.traverse(accounts){ account =>
      zuoraRestService.lastSubscriptionWithProductOfTypeOpt(membershipProductType, Set(account.id)).map(account -> _)}
  } yield {
      accountAndSubscriptionOpts.collect { case (account, Some(subscription)) =>
        account -> subscription
      }.sortBy(_._2.termStartDate).lastOption.getOrElse(throw new SubscriptionServiceError(
        s"Cannot find a membership subscription for account ids ${accounts.map(_.id)}"))
    }

  def currentRatePlan(subscription: Rest.Subscription): Future[Option[Rest.RatePlan]] = subscription.ratePlans match {
    case onlyRatePlan :: Nil => Future.successful(Some(onlyRatePlan))
    case multipleRatePlans => Timing.record(zuoraSoapService.metrics, "currentRatePlan-for-multipleRatePlans") {
      for {
        subscriptionStatus <- getSubscriptionStatus(subscription)
        currentRatePlan <- getRatePlan(subscriptionStatus.currentVersion)
      } yield {
        logger.debug(s"Current ratePlan: $currentRatePlan, subscriptionStatus: $subscriptionStatus, multipleRatePlans: $multipleRatePlans")
        multipleRatePlans.find(_.productRatePlanId == currentRatePlan.productRatePlanId)
      }
    }
  }

  def memberTierFeatures(memberId: MemberId): Future[Seq[Rest.Feature]] = for {
    (_, subscription) <- accountWithLatestMembershipSubscription(memberId)
    productIds  <- membershipProducts.map(_.map(_.id).toSet)
  } yield subscription
      .latestWhiteListedRatePlan(productIds).toSeq
      .flatMap(_.subscriptionProductFeatures)

  /**
   * @return the current and the future subscription version of the user if
   *         they have a pending amendment (Currently this is the case only of downgrades, as upgrades
   *         are effective immediately)
   */
  def getSubscriptionStatus(memberId: MemberId): Future[SubscriptionStatus] =
    accountWithLatestMembershipSubscription(memberId).flatMap(accountWithSub =>
      getSubscriptionStatus(accountWithSub._2))

  def getSubscriptionStatus(subscription: Rest.Subscription): Future[SubscriptionStatus] = for {
    subscriptionVersions <- subscriptionVersions(subscription.subscriptionNumber)
    where = subscriptionVersions.map { sub => s"SubscriptionId='${sub.id}'" }.mkString(" OR ")
    amendments <- zuoraSoapService.query[Amendment](where)
  } yield findCurrentSubscriptionStatus(subscriptionVersions, amendments)

  def getSubscriptionDetails(subscription: Subscription): Future[SubscriptionDetails] = for {
    ratePlan <- getRatePlan(subscription)
    ratePlanCharge <- zuoraSoapService.queryOne[RatePlanCharge](s"RatePlanId='${ratePlan.id}'")
  } yield SubscriptionDetails(subscription, ratePlan, ratePlanCharge)

  def getRatePlan(subscription: Subscription): Future[RatePlan] =
    zuoraSoapService.queryOne[RatePlan](s"SubscriptionId='${subscription.id}'")

  def getCurrentSubscriptionDetails(memberId: MemberId): Future[SubscriptionDetails] = for {
    subscriptionStatus <- getSubscriptionStatus(memberId)
    subscriptionDetails <- getSubscriptionDetails(subscriptionStatus.currentVersion)
  } yield subscriptionDetails

  /*
   * If the member is entitled to complimentary tickets return its Zuora account's corresponding usage records count.
   * Returns none otherwise
   */
  def getUsageCountWithinTerm(subscription: Rest.Subscription, unitOfMeasure: String): Future[Option[Int]] = {
    val featuresF = memberTierFeatures(subscription)
    val startDate = formatDateTime(subscription.termStartDate)
    val whereClause = s"StartDateTime >= '$startDate' AND SubscriptionNumber = '${subscription.subscriptionNumber}' AND UOM = '$unitOfMeasure'"
    val usageCountF = zuoraSoapService.query[Usage](whereClause).map(_.size)
    for {
      features <- featuresF
      usageCount <- usageCountF
    } yield {
      val hasComplimentaryTickets = features.exists(_.featureCode == FreeEventTickets.zuoraCode)
      if (!hasComplimentaryTickets) None else Some(usageCount)
    }
  }

  def createPaymentMethod(memberId: MemberId, customer: Stripe.Customer): Future[UpdateResult] = for {
    (account, _) <- accountWithLatestMembershipSubscription(memberId)
    paymentMethod <- zuoraSoapService.authenticatedRequest(CreatePaymentMethod(account, customer))
    result <- zuoraSoapService.authenticatedRequest(EnablePayment(account, paymentMethod))
  } yield result

  def createSubscription(memberId: MemberId,
                         joinData: JoinForm,
                         customerOpt: Option[Stripe.Customer],
                         paymentDelay: Option[Period],
                         casId: Option[String]): Future[SubscribeResult] = for {

      zuoraFeatures <- zuoraSoapService.featuresSupplier.get()
      features = featuresPerTier(zuoraFeatures)(joinData.plan, joinData.featureChoice)
      productRatePlanIds <- productRatePlanIdSupplier.get()
      result <- zuoraSoapService.authenticatedRequest(Subscribe(memberId,
                                                      customerOpt,
                                                      productRatePlanIds(joinData.plan),
                                                      joinData.name,
                                                      joinData.deliveryAddress,
                                                      paymentDelay,
                                                      casId,
                                                      features))
    } yield result

  def getPaymentSummary(memberId: MemberId): Future[PaymentSummary] = {
    for {
      subscription <- getSubscriptionStatus(memberId)
      invoiceItems <- zuoraSoapService.query[InvoiceItem](s"SubscriptionId='${subscription.currentVersionId}'")
    } yield PaymentSummary(invoiceItems)
  }

  def getMembershipSubscriptionSummary(memberId: MemberId): Future[MembershipSummary] = {
    val latestSubF = for {
      (_, subscription) <- accountWithLatestMembershipSubscription(memberId)
      subscriptionVersions <- subscriptionVersions(subscription.subscriptionNumber)
    } yield subscriptionVersions.maxBy(_.version)

    def hasUserBeenInvoiced(memberId: MemberId) =
      for (subscription <- latestSubF)
      yield subscription.contractAcceptanceDate.isBeforeNow

    def getSummaryViaSubscriptionAmend(memberId: MemberId) = for {
      latestSubscription <- latestSubF
      subscriptionDetailsF = getSubscriptionDetails(latestSubscription)
      result <- zuoraSoapService.authenticatedRequest(SubscriptionDetailsViaAmend(latestSubscription.id, latestSubscription.contractAcceptanceDate))
      subscriptionDetails <- subscriptionDetailsF

    } yield {
      assert(result.invoiceItems.nonEmpty, "Subscription with delayed payment returning zero invoice items in SubscriptionDetailsViaAmend call")
      val firstPreviewInvoice = result.invoiceItems.sortBy(_.serviceStartDate).head

      MembershipSummary(latestSubscription.termStartDate, firstPreviewInvoice.serviceEndDate, None,
        subscriptionDetails.planAmount, firstPreviewInvoice.price, firstPreviewInvoice.serviceStartDate, firstPreviewInvoice.renewalDate )
    }

    def getSummaryViaInvoice(memberId: MemberId) = for (paymentSummary <- getPaymentSummary(memberId)) yield {
      MembershipSummary(paymentSummary.current.serviceStartDate, paymentSummary.current.serviceEndDate,
        Some(paymentSummary.totalPrice), paymentSummary.current.price, paymentSummary.current.price, paymentSummary.current.nextPaymentDate, paymentSummary.current.nextPaymentDate)
    }

    for {
      userInvoiced <- hasUserBeenInvoiced(memberId)
      summary <- if (userInvoiced) getSummaryViaInvoice(memberId) else getSummaryViaSubscriptionAmend(memberId)
    } yield summary
  }

  private def memberTierFeatures(subscription: Rest.Subscription): Future[Seq[Rest.Feature]] = for {
    ratePlanOpt <- currentRatePlan(subscription)
  } yield {
      val features = ratePlanOpt.toSeq.flatMap(_.subscriptionProductFeatures)
      logger.debug(
        s"Checking product features for for subscription ${subscription.subscriptionNumber}." ++
          s" Current product ${ratePlanOpt.map(_.productName)}, features: ${features.map(_.featureCode)}")
      features
    }

  def getSubscriptionsByCasId(casId: String): Future[Seq[Subscription]] =
    zuoraSoapService.query[Subscription](s"CASSubscriberID__c='$casId'")
}
