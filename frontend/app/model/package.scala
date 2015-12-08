import com.gu.membership.model.TierPlan

package object model {
  type Subscription = CommonSubscription with PaymentStatus[TierPlan]
  type PaidSubscription = CommonSubscription with Paid
  type FreeSubscription = CommonSubscription with Free
}
