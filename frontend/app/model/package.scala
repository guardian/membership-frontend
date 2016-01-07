import com.gu.memsub.{Subscription, Free, Paid}
import com.gu.{salesforce => sf}

package object model {
  type PaidSubscription = Subscription with Paid
  type FreeSubscription = Subscription with Free
  type SFMember = sf.Contact[sf.Member, sf.PaymentMethod]
  type GenericSFContact = sf.Contact[sf.MemberStatus, sf.PaymentMethod]
  type PaidSFMember = sf.Contact[sf.PaidTierMember, sf.StripePayment]
  type FreeSFMember = sf.Contact[sf.FreeTierMember, sf.NoPayment]
}
