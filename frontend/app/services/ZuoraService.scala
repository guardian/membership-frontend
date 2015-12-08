package services

import com.gu.i18n.{Country, CountryGroup, Currency, GBP}
import com.gu.membership.model.{Current, PaidTierPlan, TierPlan}
import com.gu.membership.salesforce.Tier.{Partner, Patron}
import com.gu.membership.salesforce.{ContactId, PaidTier}
import com.gu.membership.stripe.Stripe
import com.gu.membership.zuora.soap.actions.Actions.{CreateCreditCardReferencePaymentMethod, EnablePayment, UpgradePlan}
import com.gu.membership.zuora.soap.actions.subscribe.{Account => SoapSubscribeAccount, CreditCardReferenceTransaction, Subscribe}
import com.gu.membership.zuora.soap.models.Queries.PreviewInvoiceItem
import com.gu.membership.zuora.soap.models.Results.{AmendResult, SubscribeResult, UpdateResult}
import com.gu.membership.zuora.soap.models.{Queries => SoapQueries}
import com.gu.membership.zuora.soap.{OrFilter, SimpleFilter}
import com.gu.membership.zuora.{rest, soap}
import forms.MemberForm.{JoinForm, PaidMemberJoinForm}
import model.{FeatureChoice, MembershipCatalog, SFMember}

import scala.concurrent.Future

class ZuoraService(catalog: () => Future[MembershipCatalog],
                   // TODO replace with an enriched vanilla soap client
                   soapClient: soap.ClientWithFeatureSupplier,
                   restClient: rest.Client) extends api.ZuoraService {

  def getSubscriptionsByCasId(casId: String): Future[Seq[SoapQueries.Subscription]] =
    soapClient.query[SoapQueries.Subscription](SimpleFilter("CASSubscriberID__c", casId))

  def currentSubscription(contact: ContactId): Future[model.Subscription] = ???

  def currentPaidSubscription(contact: ContactId): Future[model.PaidSubscription] = ???

  def createPaidSubscription(contactId: ContactId,
                             joinData: PaidMemberJoinForm,
                             customer: Stripe.Customer): Future[SubscribeResult] =
    for {
      catalog <- catalog()
      zuoraFeatures <- soapClient.featuresSupplier.get()
      ratePlanId <- findRatePlanId(joinData.plan)
      result <- soapClient.authenticatedRequest(Subscribe(
        account = SoapSubscribeAccount.stripe(
          contactId = contactId,
          currency = supportedAccountCurrency(catalog)(joinData.zuoraAccountAddress.country, joinData.plan),
          autopay = true),
        paymentMethodOpt = Some(CreditCardReferenceTransaction(customer)),
        ratePlanId = ratePlanId,
        firstName = joinData.name.first,
        lastName = joinData.name.last,
        address = joinData.zuoraAccountAddress,
        casIdOpt = None,
        paymentDelay = None,
        ipAddressOpt = None,
        featureIds = featuresPerTier(zuoraFeatures)(joinData.plan, joinData.featureChoice).map(_.id)))
    } yield result

  def createFreeSubscription(contactId: ContactId,
                             joinData: JoinForm): Future[SubscribeResult] =
    for {
      zuoraFeatures <- soapClient.featuresSupplier.get()
      ratePlanId <- findRatePlanId(joinData.plan)
      result <- soapClient.authenticatedRequest(Subscribe(
        account = SoapSubscribeAccount.stripe(contactId, GBP, autopay = false),
        paymentMethodOpt = None,
        ratePlanId = ratePlanId,
        firstName = joinData.name.first,
        lastName = joinData.name.last,
        address = joinData.deliveryAddress,
        casIdOpt = None,
        paymentDelay = None,
        ipAddressOpt = None,
        featureIds = Nil))
    } yield result

  def createPaymentMethod(contactId: ContactId,
                          customer: Stripe.Customer): Future[UpdateResult] =
    for {
      sub <- currentSubscription(contactId)
      paymentMethod <- soapClient.authenticatedRequest(
        CreateCreditCardReferencePaymentMethod(sub.accountId, customer.card.id, customer.id))
      result <- soapClient.authenticatedRequest(EnablePayment(sub.accountId, paymentMethod.id))
    } yield result

  def upgradeSubscription(contactId: ContactId,
                          newTierPlan: TierPlan,
                          preview: Boolean,
                          featureChoice: Set[FeatureChoice]): Future[AmendResult] =
    for {
      sub <- subWithNoPendingAmend(contactId)
      zuoraFeatures <- soapClient.featuresSupplier.get()
      newRatePlanId <- findRatePlanId(newTierPlan)
      choice = featuresPerTier(zuoraFeatures)(newTierPlan, featureChoice).map(_.id)
      result <- soapClient.authenticatedRequest(
        UpgradePlan(sub.id, sub.ratePlanId, newRatePlanId, preview, choice))
    } yield result

  def previewUpgradeSubscription(subscription: model.Subscription,
                                 contact: SFMember,
                                 newTier: PaidTier): Future[Seq[PreviewInvoiceItem]] =
    for {
      cat <- catalog()
      currentPlan = cat.unsafePaidTierPlan(subscription.productRatePlanId)
      newPlan = PaidTierPlan(newTier, currentPlan.billingPeriod, Current)
      subscriptionResult <- upgradeSubscription(contact, newPlan, preview = true, Set.empty)
    } yield subscriptionResult.invoiceItems

  private def findRatePlanId(newTierPlan: TierPlan): Future[String] = {
    catalog().map(_.ratePlanId(newTierPlan))
  }

  // TODO move into catalog
  private def supportedAccountCurrency(catalog: MembershipCatalog)(country: Country, plan: PaidTierPlan): Currency =
    CountryGroup
      .byCountryCode(country.alpha2).map(_.currency)
      .filter(catalog.paidTierPlanDetails(plan).currencies)
      .getOrElse(GBP)

  private def featuresPerTier(zuoraFeatures: Seq[SoapQueries.Feature])(plan: TierPlan, choice: Set[FeatureChoice]): Seq[SoapQueries.Feature] = {
    def byChoice(choice: Set[FeatureChoice]) =
      zuoraFeatures.filter(f => choice.map(_.zuoraCode).contains(f.code))

    plan.tier match {
      case Patron => byChoice(FeatureChoice.all)
      case Partner => byChoice(choice).take(1)
      case _ => Nil
    }
  }

  private def subWithNoPendingAmend(contactId: ContactId): Future[model.Subscription] =
    for {
      sub <- currentSubscription(contactId)
      status <- getSubscriptionStatus(sub.number)
    } yield {
      if (status.futureVersionIdOpt.isEmpty) {
        sub
      } else throw SubscriptionServiceError("Cannot amend subscription, amendments are already pending")
    }

  private def getSubscriptionStatus(memberId: ContactId): Future[soap.models.SubscriptionStatus] =
    currentSubscription(memberId).flatMap(sub => getSubscriptionStatus(sub.number))

  private def getSubscriptionStatus(subscriptionNumber: String): Future[soap.models.SubscriptionStatus] = for {
    subscriptionVersions <- subscriptionVersions(subscriptionNumber)
    amendments <- soapClient.query[SoapQueries.Amendment](OrFilter(subscriptionVersions.map(s => ("SubscriptionId", s.id)): _*))
  } yield findCurrentSubscriptionStatus(subscriptionVersions, amendments)

  private def subscriptionVersions(subscriptionNumber: String): Future[Seq[SoapQueries.Subscription]] = for {
    subscriptions <- soapClient.query[SoapQueries.Subscription](SimpleFilter("Name", subscriptionNumber))
  } yield subscriptions

  /**
    * A Zuora subscription may have many versions as it is amended, some of which can be in the future (ie. downgrading
    * from a paid tier - because we don't refund that user, the downgrade is instead set to the point in the future when
    * their paid period ends).
    *
    * The Zuora API does not explicitly tell you what the *current* subscription version is. You have to work it out,
    * by looking at the 'amendments', finding the first amendment that has yet occurred. That amendment will give you the
    * id of the subscription it modified - and THAT will be the *current* subscription version.
    */
  private def findCurrentSubscriptionStatus(subscriptionVersions: Seq[SoapQueries.Subscription], amendments: Seq[SoapQueries.Amendment]): soap.models.SubscriptionStatus = {
    val firstAmendmentWhichHasNotYetOccurredOpt = // this amendment *will have modified the current subscription*
      sortAmendments(subscriptionVersions, amendments).find(_.contractEffectiveDate.isAfterNow)

    val latestSubVersion = subscriptionVersions.maxBy(_.version)

    firstAmendmentWhichHasNotYetOccurredOpt.fold(soap.models.SubscriptionStatus(latestSubVersion, None, None)) { amendmentOfCurrentSub =>
      val currentSubId = amendmentOfCurrentSub.subscriptionId
      val currentSubVersion = subscriptionVersions.find(_.id == currentSubId).get
      soap.models.SubscriptionStatus(currentSubVersion, Some(latestSubVersion), Some(amendmentOfCurrentSub.amendType))
    }
  }

  /**
    * @param amendments which are returned by the Zurora API in an unpredictable order
    * @return amendments which are sorted by the subscription version number they point to (the sub they amended)
    */
  private def sortAmendments(subscriptions: Seq[SoapQueries.Subscription], amendments: Seq[SoapQueries.Amendment]): Seq[SoapQueries.Amendment] = {
    val versionsNumberBySubVersionId = subscriptions.map { sub => (sub.id, sub.version) }.toMap
    amendments.sortBy { amendment => versionsNumberBySubVersionId(amendment.subscriptionId) }
  }
}
