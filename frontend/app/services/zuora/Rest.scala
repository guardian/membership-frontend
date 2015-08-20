package services.zuora

import org.joda.time.DateTime

object Rest {
  sealed trait Response[+T] {
    def isSuccess: Boolean
    def get: T
    def toOption: Option[T]
  }

  case class Success[T](value: T) extends Response[T] {
    override def isSuccess = true
    override def get = value
    override def toOption = Some(value)
  }

  case class Error(code: String, message: String)

  case class Failure(processId: String, reasons: Seq[Error]) extends Response[Nothing] {
    override def isSuccess = false
    override def get = sys.error("Zuora.Rest.Failure.get called")
    override def toOption = None
  }

  sealed trait SubscriptionStatus
  case object Draft extends SubscriptionStatus
  case object PendingActivation extends SubscriptionStatus
  case object PendingAcceptance extends SubscriptionStatus
  case object Active extends SubscriptionStatus
  case object Cancelled extends SubscriptionStatus
  case object Expired extends SubscriptionStatus

  case class Subscription(id: String,
                          subscriptionNumber: String,
                          accountId: String,
                          termStartDate: DateTime,
                          termEndDate: DateTime,
                          contractEffectiveDate: DateTime,
                          ratePlans: Seq[Rest.RatePlan],
                          status: SubscriptionStatus) {
    val isActive: Boolean = status == Active
  }

  case class Feature(id: String, featureCode: String)
  case class RatePlan(subscriptionProductFeatures: List[Rest.Feature])
}
