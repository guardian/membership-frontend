package services.zuora

import com.gu.membership.zuora.ZuoraApiConfig
import com.netaporter.uri.dsl._
import model.Zuora.{Authentication, ZuoraResult}
import org.specs2.mutable.Specification

class ZuoraActionTest extends Specification {

  implicit val auth = Some(Authentication("token", "http://example.com/"))

  "ZuoraAction" should {
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
      val action = Login(ZuoraApiConfig("TEST", "http://example.com" / "test", "secret", "secret"))
      action.sanitized must not contain "secret"
    }
  }

  case class TestResult() extends ZuoraResult

  case class TestAction() extends ZuoraAction[TestResult] {
    val body = <test></test>
  }

}
