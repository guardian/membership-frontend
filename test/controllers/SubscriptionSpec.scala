package controllers

import play.api.test.{FakeRequest, PlaySpecification}

class SubscriptionSpec extends PlaySpecification {

  class TestController extends Subscription

  "SubscriptionPage" should {
    "display stripe form" in {
      val controller = new TestController()
      val result = controller.stripe.apply(FakeRequest())
      contentAsString(result) must be contain "Card Number"
    }

  }
}

class SubscriptionIntegrationSpec extends PlaySpecification {

  class TestController extends Subscription

  "SubscriptionPage" should {
    "make a stripe payment" in {
      val controller = new TestController()
      val data = ("" -> "")
      val result = controller.stripeSubmit().apply(FakeRequest().withFormUrlEncodedBody(data))
      contentAsString(result) must be contain "Card Number"

    }
  }

}
