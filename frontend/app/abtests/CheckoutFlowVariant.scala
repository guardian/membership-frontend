package abtests

import play.api.mvc.RequestHeader

import scala.util.Random

/**
  * Created by santiago_fernandez on 25/11/2016.
  */
object CheckoutFlowVariant {
  val cookieName = "ab-checkout-flow"
  val runningTest = false
  val default = A

  val all = Seq[CheckoutFlowVariant](A,B)

  def deriveFlowVariant(implicit request: RequestHeader): CheckoutFlowVariant =
    if(runningTest) getFlowVariantFromRequestCookie(request).getOrElse(CheckoutFlowVariant.all(Random.nextInt(CheckoutFlowVariant.all.size)))
    else default

  def getFlowVariantFromRequestCookie(request: RequestHeader): Option[CheckoutFlowVariant] =
    if(runningTest)
      for {
        cookieValue <- request.cookies.get(cookieName)
        variant <- CheckoutFlowVariant.lookup(cookieValue.value)
      } yield variant
    else Some(default)

  case object A extends CheckoutFlowVariant {
    override val testId: String = "test-A"
    override val identitySkin: String = "members"
  }

  case object B extends CheckoutFlowVariant {
    override val testId: String = "test-B"
    override val identitySkin: String = "membersB"
  }

  def lookup(name: String): Option[CheckoutFlowVariant] = all.find(_.testId == name)
}

sealed trait CheckoutFlowVariant {
  val testId: String
  val identitySkin: String
}
