package com.gu.memsub
import com.gu.memsub.subsv2.{SubscriptionPlan => Plan}
import com.gu.salesforce.Contact

case class Subscriber[+T <: subsv2.Subscription[Plan.AnyPlan]](subscription: T, contact: Contact)

object Subscriber {
  type PaidMember = Subscriber[subsv2.Subscription[Plan.PaidMember]]
  type FreeMember = Subscriber[subsv2.Subscription[Plan.FreeMember]]
  type Member = Subscriber[subsv2.Subscription[Plan.Member]]
}