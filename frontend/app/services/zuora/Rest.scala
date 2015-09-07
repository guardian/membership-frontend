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

  /*
   * Zuora product catalogue entry: https://knowledgecenter.zuora.com/BC_Developers/REST_API/B_REST_API_reference/Catalog
   */
  case class ProductCatalog(products: Seq[Product]) {
    def productsOfType(t: String): Seq[Product] = products.filter(_.`ProductType__c` == t)
    def productIdsOfType(t: String): Set[String] = productsOfType(t).map(_.id).toSet
  }


  case class ProductRatePlanCharge(model:String, billingPeriod:Option[String])
  case class ProductRatePlan(id: String, name: String, status: String, productRatePlanCharges: Seq[ProductRatePlanCharge]) {
    def isActive: Boolean = status.toLowerCase == "active"
  }
  case class Product(id: String,
                     name: String,
                     `ProductType__c`: String,
                     `Tier__c`: Option[String],
                     productRatePlans: Seq[ProductRatePlan]) {
  }

  case class Subscription(id: String,
                          subscriptionNumber: String,
                          accountId: String,
                          termStartDate: DateTime,
                          termEndDate: DateTime,
                          contractEffectiveDate: DateTime,
                          ratePlans: Seq[Rest.RatePlan],
                          status: SubscriptionStatus) {

    val isActive: Boolean = status == Active

    def hasWhitelistedProduct(productIds: Set[String]): Boolean =
      latestWhiteListedRatePlan(productIds).isDefined

    def latestWhiteListedRatePlan(productIds: Set[String]): Option[RatePlan] =
      ratePlans.find(_.isWhitelisted(productIds))
  }

  case class Feature(id: String, featureCode: String)
  case class RatePlan(id: String, productId: String, productRatePlanId: String, productName: String, subscriptionProductFeatures: List[Rest.Feature], ratePlanCharges: List[RatePlanCharge]) {
    def isWhitelisted(productIds: Set[String]): Boolean = productIds.contains(productId)
  }
  case class RatePlanCharge(name: String, id: String)
}
