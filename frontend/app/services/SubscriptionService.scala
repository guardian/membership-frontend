package services

import com.github.nscala_time.time.Imports._
import com.gu.membership.model._
import com.gu.membership.salesforce.MemberId
import com.gu.membership.salesforce.Tier.{Partner, Patron}
import com.gu.membership.stripe.Stripe
import com.typesafe.scalalogging.LazyLogging
import forms.MemberForm.JoinForm
import model.{FeatureChoice, MembershipSummary}
import model.Zuora._
import model.ZuoraDeserializer._
import org.joda.time.DateTime
import services.zuora.Rest.ProductRatePlanCharge
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
        subscriptionRatePlanId <- tierPlanRateIds(newTierPlan)
        result <- zuoraSoapService.authenticatedRequest(DowngradePlan(currentSubscriptionVersion.id, subscriptionDetails.ratePlanId,
          subscriptionRatePlanId, dateToMakeDowngradeEffectiveFrom))
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
        subscriptionRatePlanId <- tierPlanRateIds(newTierPlan)
        result <- zuoraSoapService.authenticatedRequest(UpgradePlan(subscriptionId, ratePlan.id, subscriptionRatePlanId, preview, choice))
      } yield result
    }
  }
}

object SubscriptionService {
  def sortAmendments(subscriptions: Seq[Subscription], amendments: Seq[Amendment]) = {
    val versionsById = subscriptions.map { sub => (sub.id, sub.version) }.toMap
    amendments.sortBy { amendment => versionsById(amendment.subscriptionId) }
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

  val tierPlanRateIds: ProductRatePlan => Future[String] = productRatePlan =>
    membershipProducts.map { products =>
      val zuoraRatePlanId = for {
        product <- products.find(_.`Tier__c`.contains(productRatePlan.salesforceTier))
        zuoraRatePlan <- productRatePlan match {
          case FriendTierPlan | StaffPlan => product.activeRatePlans.headOption
          case paidTierPlan: PaidTierPlan =>
            val charge = ProductRatePlanCharge(productRatePlanChargeModel, Some(paidTierPlan.billingPeriod))
            product.activeRatePlans.find(_.productRatePlanCharges.contains(charge))
        } if zuoraRatePlan.isActive
      } yield zuoraRatePlan.id

      zuoraRatePlanId.getOrElse(throw new RuntimeException(s"Rate plan id could not be found for $productRatePlan"))
    }

  def membershipProducts = zuoraRestService.productCatalogSupplier.get().map(_.productsOfType(membershipProductType))

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

  def memberTierFeatures(memberId: MemberId): Future[Seq[Rest.Feature]] = for {
    (_, subscription) <- accountWithLatestMembershipSubscription(memberId)
    productIds  <- membershipProducts.map(_.map(_.id).toSet)
  } yield subscription
      .latestWhiteListedRatePlan(productIds).toSeq
      .flatMap(_.subscriptionProductFeatures)

  /**
   * @return the current subscription version of the user, and the future subscription version, if
   *         they have a pending downgrade.
   */
  def getSubscriptionStatus(memberId: MemberId): Future[SubscriptionStatus] = for {
    (_, subscription) <- accountWithLatestMembershipSubscription(memberId)
    subscriptionVersions <- subscriptionVersions(subscription.subscriptionNumber)
    where = subscriptionVersions.map { sub => s"SubscriptionId='${sub.id}'" }.mkString(" OR ")
    amendments <- zuoraSoapService.query[Amendment](where)
  } yield {
      val latestSubscription = subscriptionVersions.maxBy(_.version)
      sortAmendments(subscriptionVersions, amendments)
        .find(_.contractEffectiveDate.isAfterNow)
        .fold(SubscriptionStatus(latestSubscription, None, None)) { futureAmendment =>
          if (subscription.id != latestSubscription.id) {
            logger.error(s"current subscription id ${subscription.id} is different than latest subscription version ${latestSubscription.id}")
          }
          val amendedSubscription = subscriptionVersions.find(_.id == futureAmendment.subscriptionId).get
          SubscriptionStatus(amendedSubscription, Some(latestSubscription), Some(futureAmendment.amendType))
      }
    }

  def getSubscriptionDetails(subscription: Subscription): Future[SubscriptionDetails] = for {
    ratePlan <- zuoraSoapService.queryOne[RatePlan](s"SubscriptionId='${subscription.id}'")
    ratePlanCharge <- zuoraSoapService.queryOne[RatePlanCharge](s"RatePlanId='${ratePlan.id}'")
  } yield SubscriptionDetails(subscription, ratePlan, ratePlanCharge)

  def getCurrentSubscriptionDetails(memberId: MemberId): Future[SubscriptionDetails] = for {
    subscriptionStatus <- getSubscriptionStatus(memberId)
    subscriptionDetails <- getSubscriptionDetails(subscriptionStatus.currentVersion)
  } yield subscriptionDetails

  def getUsageCountWithinTerm(memberId: MemberId, unitOfMeasure: String): Future[Int] = for {
    (_, subscription) <- accountWithLatestMembershipSubscription(memberId)
    startDate = formatDateTime(subscription.termStartDate)
    whereClause = s"StartDateTime >= '$startDate' AND SubscriptionID = '${subscription.id}' AND UOM = '$unitOfMeasure'"
    usages <- zuoraSoapService.query[Usage](whereClause)
  } yield usages.size

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
      ratePlanId <- tierPlanRateIds(joinData.plan)
      result <- zuoraSoapService.authenticatedRequest(Subscribe(memberId,
                                                      customerOpt,
                                                      ratePlanId,
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

  def getSubscriptionsByCasId(casId: String): Future[Seq[Subscription]] =  zuoraSoapService.query[Subscription](s"CASSubscriberID__c='$casId'")
}
