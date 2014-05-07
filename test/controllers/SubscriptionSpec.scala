package controllers

import play.api.test.{FakeRequest, PlaySpecification}

class SubscriptionSpec extends PlaySpecification {

  class TestController extends Subscription

  "SubscriptionPage" should {
    "display stripe form" in {
      val controller = new TestController()
      val result = controller.stripe.apply(FakeRequest())
      contentAsString(result).contains("Card Number") must beEqualTo(true)
    }

  }
}

