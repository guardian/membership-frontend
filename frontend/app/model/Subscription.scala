package model

import com.gu.i18n.Currency
import com.gu.membership.model.{FreeTierPlan, PaidTierPlan, Price, TierPlan}
import com.gu.membership.salesforce.ContactId
import com.gu.membership.zuora.rest
import com.gu.membership.zuora.soap.models.Queries.Account
import org.joda.time.LocalDate

private[model] case class CommonSubscription(
  number: String,
  id: String,
  accountId: String,
  accountCurrency: Currency,
  contactId: ContactId,
  productRatePlanId: String,
  ratePlanId: String,
  startDate: LocalDate,
  termEndDate: LocalDate
) {
  def this(other: CommonSubscription) = this(
    number = other.number,
    id = other.id,
    accountId = other.accountId,
    accountCurrency = other.accountCurrency,
    contactId = other.contactId,
    productRatePlanId = other.productRatePlanId,
    ratePlanId = other.ratePlanId,
    startDate = other.startDate,
    termEndDate = other.termEndDate
  )
}

trait PaymentStatus[+T <: TierPlan] { self: CommonSubscription =>
  def features: Set[FeatureChoice]
  def userHasBeenInvoiced: Boolean
  def isPaid: Boolean
  def isInTrialPeriod: Boolean
  def plan: T
}

trait Free extends PaymentStatus[FreeTierPlan] { self: CommonSubscription =>
  override def features = Set.empty[FeatureChoice]
  override def userHasBeenInvoiced = false
  override def isPaid = false
  override def isInTrialPeriod = false
}

trait Paid extends PaymentStatus[PaidTierPlan] { self: CommonSubscription =>
  def defaultPaymentMethod: Option[String]
  def recurringPrice: Price
  def firstPaymentDate: LocalDate
  def chargedThroughDate: Option[LocalDate]
  override def features: Set[FeatureChoice]
  override def userHasBeenInvoiced =
    chargedThroughDate.nonEmpty || LocalDate.now().isAfter(firstPaymentDate.minusDays(1))
  override def isPaid = true
  override def isInTrialPeriod = !userHasBeenInvoiced
}

object Subscription {
  def accountCurrency(account: Account): Currency = Currency.fromString(account.currency).getOrElse {
    throw new IllegalArgumentException(s"Cannot parse currency ${account.currency} for account ${account.id}")
  }

  def apply(catalog: MembershipCatalog)(contactId: ContactId,
                                        account: Account,
                                        sub: rest.Subscription): CommonSubscription with PaymentStatus[TierPlan] = {
    val rp = sub.currentRatePlanUnsafe()
    val productRatePlanId = rp.productRatePlanId
    val tierPlan = catalog.unsafeTierPlan(productRatePlanId)
    val subscription = CommonSubscription(
      number = sub.subscriptionNumber,
      id = sub.id,
      accountId = account.id,
      accountCurrency = accountCurrency(account),
      contactId = contactId,
      productRatePlanId = productRatePlanId,
      ratePlanId = rp.id,
      startDate = sub.contractEffectiveDate.toLocalDate,
      termEndDate = sub.termEndDate.toLocalDate
    )

    tierPlan match {
      case p: FreeTierPlan => new CommonSubscription(subscription) with Free {
        override def plan = p
      }
      case p: PaidTierPlan => new CommonSubscription(subscription) with Paid {
        private val charge = rp.currentChargeUnsafe()
        override def defaultPaymentMethod = account.defaultPaymentMethodId
        override def chargedThroughDate = charge.chargedThroughDate.map(_.toLocalDate)
        override def recurringPrice = charge.unsafePrice
        override def firstPaymentDate = sub.customerAcceptanceDate.toLocalDate
        override def features = rp.subscriptionProductFeatures.map(f => FeatureChoice.byId(f.featureCode)).toSet
        override def plan = p
      }
    }
  }
}
