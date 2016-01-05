package model

import com.gu.membership.{FreeMembershipPlan, PaidMembershipPlan, MembershipPlan, MembershipCatalog}
import com.gu.memsub.{BillingPeriod, Status, Subscription}
import com.gu.salesforce.{FreeTier, PaidTier, Tier}

object SubscriptionOps {
  implicit class WithTier(subscription: Subscription) {
    def plan(implicit catalog: MembershipCatalog): MembershipPlan[Status, Tier] =
      catalog.unsafeFind(subscription.productRatePlanId)
  }

  implicit class WithPaidTIer(subscription: PaidSubscription) {
    def paidPlan(implicit catalog: MembershipCatalog): PaidMembershipPlan[Status, PaidTier, BillingPeriod] =
      catalog.unsafeFindPaid(subscription.productRatePlanId)
  }

  implicit class WithFreePlan(subscription: FreeSubscription) {
    def freePlan(implicit catalog: MembershipCatalog): FreeMembershipPlan[Status, FreeTier] =
      catalog.unsafeFindFree(subscription.productRatePlanId)
  }
}
