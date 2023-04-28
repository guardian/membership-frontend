package com.gu.zuora.soap

import com.gu.zuora.{ZuoraSoapConfig, ZuoraApiConfig}
import com.gu.zuora.soap.actions.Action
import com.gu.zuora.soap.actions.Actions.Login
import com.gu.zuora.soap.models.Results.Authentication
import com.gu.zuora.soap.models.Result
import io.lemonlabs.uri.dsl._
import org.specs2.mutable.Specification


class ActionTest extends Specification {

  case class TestResult() extends Result
  case class TestAction() extends Action[TestResult] {
    val body = <test></test>
  }

  implicit val auth = Some(Authentication("token", "http://example.com/"))

  "Action" should {
    "create a standard action" in {
      val xml = TestAction().xml(auth)
      (xml \ "Body" \ "test").length mustEqual 1
      (xml \ "Header" \ "SessionHeader" \ "session").text.trim mustEqual "token"
      (xml \ "Header" \ "CallOptions" \ "useSingleTransaction").length mustEqual 0
    }

    "create an un-authenticated action" in {
      val action = new TestAction {}

      val xml = action.xml(None)
      (xml \ "Header" \ "SessionHeader").length mustEqual 0
    }

    "create a single transaction action" in {
      val action = new TestAction {
        override val singleTransaction = true
      }

      val xml = action.xml(auth)
      (xml \ "Header" \ "CallOptions" \ "useSingleTransaction").text mustEqual "true"
    }

    "create an un-authenticated, single transaction action" in {
      val action = new TestAction {
        override val singleTransaction = true
      }

      val xml = action.xml(None)
      (xml \ "Header" \ "SessionHeader").length mustEqual 0
      (xml \ "Header" \ "CallOptions" \ "useSingleTransaction").text mustEqual "true"
    }

    "not reveal login details in sanitized output" in {
      val action = Login(ZuoraSoapConfig("TEST", "http://example.com" / "test", "secret", "secret"))
      action.sanitized must not contain "secret"
    }
  }


}
