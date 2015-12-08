import com.gu.membership.model.TierPlan
import com.gu.membership.{salesforce => sf}

package object model {
  type Subscription = CommonSubscription with PaymentStatus[TierPlan]
  type PaidSubscription = CommonSubscription with Paid
  type FreeSubscription = CommonSubscription with Free
  type SFMember = sf.Contact[sf.Member, sf.PaymentMethod]
  type NonPaidSFMember = sf.Contact[sf.Member, sf.NoPayment]
  type GenericSFContact = sf.Contact[sf.MemberStatus, sf.PaymentMethod]

  //FIXME change  to
  // type PaidSFMember = sf.Contact[sf.PaidTierMember, sf.StripePayment]
  type PaidSFMember = sf.Contact[sf.Member, sf.StripePayment]
}
