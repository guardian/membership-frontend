package monitoring

import com.gu.salesforce.Tier
import com.gu.zuora.soap.models.Commands.PaymentMethod
import com.amazonaws.services.cloudwatch.model.Dimension
import com.gu.memsub.subsv2.SubscriptionPlan
import controllers.Subscription
import model.{ContributionPlanChoice, PlanChoice}

class ContributorMetrics(val backendEnv: String) extends TouchpointBackendMetrics {

  val service = "Contributor"

  def putAttemptedSignUp: Unit ={
    put(s"contributor-attempted-sign-up")
  }

  def putSignUp: Unit ={
    put(s"contributor-sign-up")
  }

  def putThankYou: Unit ={
    put(s"contributor-sign-up-thank-you")
  }

  def putFailSignUp: Unit ={
    put(s"contributor-failed-sign-up")
  }

  def putFailSignUpGatewayError: Unit ={
    put(s"contributor-failed-sign-up-gateway-error")
  }

  def putFailSignUpStripe: Unit ={
    put(s"contributor-failed-sign-up-stripe")
  }

  private def put(metricName: String) {
    put(metricName, 1)
  }
}
