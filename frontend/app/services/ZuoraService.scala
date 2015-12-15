package services

import com.github.nscala_time.time.Imports._
import com.gu.config.{ProductFamily, Membership}
import com.gu.i18n.{Country, CountryGroup, Currency, GBP}
import com.gu.membership.model.{PaidTierPlan, TierPlan}
import com.gu.membership.salesforce.ContactId
import com.gu.membership.salesforce.Tier.{Partner, Patron}
import com.gu.membership.stripe.Stripe
import com.gu.membership.zuora.soap.Readers._
import com.gu.membership.zuora.soap.actions.Actions._
import com.gu.membership.zuora.soap.actions.subscribe.{Account => SoapSubscribeAccount, CreditCardReferenceTransaction, Subscribe}
import com.gu.membership.zuora.soap.models.Queries.{Feature, Usage}
import com.gu.membership.zuora.soap.models.Results.{CreateResult, AmendResult, SubscribeResult, UpdateResult}
import com.gu.membership.zuora.soap.models.{Queries => SoapQueries, PaymentSummary, SubscriptionStatus}
import com.gu.membership.zuora.soap.{DateTimeHelpers, AndFilter, OrFilter, SimpleFilter}
import com.gu.membership.zuora.{rest, soap}
import forms.MemberForm.{JoinForm, PaidMemberJoinForm}
import model.{FeatureChoice, MembershipCatalog}
import services.api.ZuoraService.FeatureId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ZuoraService(catalogService: => api.CatalogService,
                   // TODO replace with an enriched vanilla soap client
                   soapClient: => soap.ClientWithFeatureSupplier,
                   restClient: => rest.Client,
                   productFamily: => Membership) extends api.ZuoraService {

  override def getAccounts(contactId: ContactId): Future[Seq[SoapQueries.Account]] =
    soapClient.query[SoapQueries.Account](SimpleFilter("crmId", contactId.salesforceAccountId))

  override def getLatestRestSubscription(productFamily: ProductFamily,
                                         account: SoapQueries.Account): Future[Option[rest.Subscription]] =
    restClient.latestSubscriptionOpt(productFamily.productRatePlanIds, Set(account.id))

  override def getSubscriptionStatus(subscriptionNumber: String): Future[SubscriptionStatus] = for {
    subscriptionVersions <- subscriptionVersions(subscriptionNumber)
    amendments <- soapClient.query[SoapQueries.Amendment](OrFilter(subscriptionVersions.map(s => ("SubscriptionId", s.id)): _*))
  } yield findCurrentSubscriptionStatus(subscriptionVersions, amendments)

  override def getSubscriptionsByCasId(casId: String): Future[Seq[SoapQueries.Subscription]] =
    soapClient.query[SoapQueries.Subscription](SimpleFilter("CASSubscriberID__c", casId))

  override def createPaidSubscription(contactId: ContactId,
                             joinData: PaidMemberJoinForm,
                             customer: Stripe.Customer): Future[SubscribeResult] =
    for {
      catalog <- catalogService.membershipCatalog.get()
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

  override def createFreeSubscription(contactId: ContactId,
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

  def createCreditCardPaymentMethod(accountId: String, stripeCustomer: Stripe.Customer): Future[UpdateResult] =
    for {
      paymentMethod <- soapClient.authenticatedRequest(
        CreateCreditCardReferencePaymentMethod(accountId, stripeCustomer.card.id, stripeCustomer.id))
      result <- soapClient.authenticatedRequest(EnablePayment(accountId, paymentMethod.id))
    } yield result

  override def upgradeSubscription(subscriptionId: String,
                                   currentRatePlanId: String,
                                   newRatePlanId: String,
                                   featureIds: Seq[FeatureId],
                                   preview: Boolean = false): Future[AmendResult] =
    soapClient.authenticatedRequest(
      UpgradePlan(
        subscriptionId = subscriptionId,
        subscriptionRatePlanId = currentRatePlanId,
        newRatePlanId = newRatePlanId,
        featureIds = featureIds,
        preview = preview)
    )

  override def downgradePlan(subscriptionId: String,
                    currentRatePlanId: String,
                    futureRatePlanId: String,
                    effectiveFrom: DateTime): Future[AmendResult] =
    soapClient.authenticatedRequest(
      DowngradePlan(
        subscriptionId,
        currentRatePlanId,
        futureRatePlanId,
        effectiveFrom)
    )

  override def cancelPlan(subscriptionId: String,
                          ratePlanId: String,
                          cancelDate: DateTime) =
    soapClient.authenticatedRequest(
      CancelPlan(subscriptionId, ratePlanId, cancelDate)
    )

  implicit private def features: Future[Seq[Feature]] = soapClient.featuresSupplier.get()
  override def chooseFeature(choices: Set[FeatureChoice]): Future[Seq[FeatureId]] =
    features.map {
      _.filter(f => choices.map(_.zuoraCode).contains(f.code))
       .map(_.id)
    }

  override def getPaymentSummary(subscriptionNumber: String, accountCurrency: Currency): Future[PaymentSummary] =
    for {
      invoiceItems <- soapClient.query[SoapQueries.InvoiceItem](SimpleFilter("SubscriptionNumber", subscriptionNumber))
    } yield {
      val filteredInvoices = latestInvoiceItems(invoiceItems)
      PaymentSummary(filteredInvoices, accountCurrency)
    }

  override def getUsages(subscriptionNumber: String, unitOfMeasure: String, startDate: DateTime): Future[Seq[Usage]] =
    soapClient.query[Usage](
      AndFilter(
        ("StartDateTime", DateTimeHelpers.formatDateTime(startDate)),
        ("SubscriptionNumber", subscriptionNumber),
        ("UOM", unitOfMeasure))
    )

  override def createFreeEventUsage(accountId: String,
                                    subscriptionNumber: String,
                                    description: String,
                                    quantity: Int): Future[CreateResult] =
    soapClient.authenticatedRequest(CreateFreeEventUsage(accountId, description, quantity, subscriptionNumber))

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

  private def findRatePlanId(newTierPlan: TierPlan): Future[String] =
    catalogService.membershipCatalog.get().map(_.ratePlanId(newTierPlan))

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
  def findCurrentSubscriptionStatus(subscriptionVersions: Seq[SoapQueries.Subscription], amendments: Seq[SoapQueries.Amendment]): soap.models.SubscriptionStatus = {
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

  def latestInvoiceItems(items: Seq[SoapQueries.InvoiceItem]): Seq[SoapQueries.InvoiceItem] = {
    if(items.isEmpty)
      items
    else {
      val sortedItems = items.sortBy(_.chargeNumber)
      sortedItems.filter(_.subscriptionId == sortedItems.last.subscriptionId)
    }
  }
}
