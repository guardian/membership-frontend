package monitoring

import com.gu.salesforce.Tier
import com.gu.zuora.soap.models.Commands.PaymentMethod
import com.amazonaws.services.cloudwatch.model.{Dimension, PutMetricDataResult}

import java.util.concurrent.Future

class MemberMetrics(val backendEnv: String) extends TouchpointBackendMetrics {

  val service = "Member"

  def putAttemptedSignUp(tier: Tier): Unit ={
    put(s"attempted-sign-ups-${tier.name}")
  }

  def putSignUp(tier: Tier) = {
    put(s"sign-ups-${tier.name}")
  }

  def putThankYou(tier: Tier): Unit = {
    put(s"sign-up-thank-you-${tier.name}")
  }

  def putUpgrade(tier: Tier) = {
    put(s"upgrade-${tier.name}")
  }

  def putDowngrade(tier: Tier) = {
    put(s"downgrade-${tier.name}")
  }

  def putCancel(tier: Tier) = {
    put(s"cancel-${tier.name}")
  }

  def putFailSignUp(tier: Tier) = {
    put(s"failed-sign-up-${tier.name}")
  }

  def putFailSignUpGatewayError(tier: Tier) = {
    put(s"failed-sign-up-gateway-error-${tier.name}")
  }

  def putFailSignUpStripe(tier: Tier) = {
    put(s"failed-sign-up-stripe-${tier.name}")
  }

  def putCreationOfPaidSubscription(paymentMethod: PaymentMethod) = {
    val paymentDimension = new Dimension().withName("PaymentMethod").withValue(paymentMethod.getClass.getSimpleName)

    put(s"create-paid-subscription", 1, paymentDimension)
  }

  private def put(metricName: String): Future[PutMetricDataResult] = {
    put(metricName, 1)
  }
}
