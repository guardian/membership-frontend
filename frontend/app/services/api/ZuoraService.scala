package services.api

import com.gu.config.ProductFamily
import com.gu.i18n.Currency
import com.gu.membership.salesforce.ContactId
import com.gu.membership.stripe.Stripe
import com.gu.membership.zuora.rest
import com.gu.membership.zuora.soap.models.Queries.Usage
import com.gu.membership.zuora.soap.models.Results.{CreateResult, AmendResult, SubscribeResult, UpdateResult}
import com.gu.membership.zuora.soap.models.{Queries => SoapQueries, PaymentSummary, SubscriptionStatus}
import forms.MemberForm.{JoinForm, PaidMemberJoinForm}
import model.FeatureChoice
import org.joda.time.DateTime
import services.api.ZuoraService.FeatureId

import scala.concurrent.Future

object ZuoraService {
  type FeatureId = String
}

trait ZuoraService {
  def getAccounts(contactId: ContactId): Future[Seq[SoapQueries.Account]]

  def getLatestRestSubscription(productFamily: ProductFamily,
                                account: SoapQueries.Account): Future[Option[rest.Subscription]]

  def getSubscriptionsByCasId(casId: String): Future[Seq[SoapQueries.Subscription]]

  /**
    * @return the current and the future subscription version of the user if
    *         they have a pending amendment (Currently this is the case only of downgrades, as upgrades
    *         are effective immediately)
    */
  def getSubscriptionStatus(subscriptionNumber: String): Future[SubscriptionStatus]

  def createPaidSubscription(contactId: ContactId,
                             joinData: PaidMemberJoinForm,
                             customer: Stripe.Customer): Future[SubscribeResult]

  def createFreeSubscription(contactId: ContactId,
                             joinData: JoinForm): Future[SubscribeResult]

  def createCreditCardPaymentMethod(accountId: String, stripeCustomer: Stripe.Customer): Future[UpdateResult]

  def downgradePlan(subscriptionId: String,
                    currentRatePlanId: String,
                    futureRatePlanId: String,
                    effectiveFrom: DateTime): Future[AmendResult]

  def upgradeSubscription(subscriptionId: String,
                          currentRatePlanId: String,
                          newRatePlanId: String,
                          featureIds: Seq[FeatureId],
                          preview: Boolean): Future[AmendResult]

  def cancelPlan(subscriptionId: String,
                 ratePlanId: String,
                 cancelDate: DateTime): Future[AmendResult]

  def chooseFeature(choices: Set[FeatureChoice]): Future[Seq[FeatureId]]

  def getPaymentSummary(subscriptionNumber: String, accountCurrency: Currency): Future[PaymentSummary]

  def getUsages(subscriptionNumber: String, unitOfMeasure: String, startDate: DateTime): Future[Seq[Usage]]

  def createFreeEventUsage(accountId: String,
                           subscriptionNumber: String,
                           description: String,
                           quantity: Int): Future[CreateResult]
}
