package services

import org.specs2.mutable.Specification
import model.Zuora.{ZuoraObject, Authentication}
import scala.xml.XML

class ZuoraServiceTest extends Specification {
  "ZuoraService" should {
    "create a standard action" in {
      val xml = XML.loadString(TestAction().xml)
      (xml \ "Body" \ "test").length mustEqual 1
      (xml \ "Header" \ "SessionHeader" \ "session").text.trim mustEqual "token"
      (xml \ "Header" \ "CallOptions" \ "useSingleTransaction").length mustEqual 0
    }

    "create an un-authenticated action" in {
      val action = new TestAction {
        override val authRequired = false
      }

      val xml = XML.loadString(action.xml)
      (xml \ "Header" \ "SessionHeader").length mustEqual 0
    }

    "create a single transaction action" in {
      val action = new TestAction {
        override val singleTransaction = true
      }

      val xml = XML.loadString(action.xml)
      (xml \ "Header" \ "CallOptions" \ "useSingleTransaction").text mustEqual "true"
    }

    "create an un-authenticated, single transaction action" in {
      val action = new TestAction {
        override val authRequired = false
        override val singleTransaction = true
      }

      val xml = XML.loadString(action.xml)
      (xml \ "Header" \ "SessionHeader").length mustEqual 0
      (xml \ "Header" \ "CallOptions" \ "useSingleTransaction").text mustEqual "true"
    }
  }

  case class TestObject() extends ZuoraObject

  case class TestAction() extends ZuoraService.ZuoraAction[TestObject] {
    val body = <test></test>
  }

  object ZuoraService extends ZuoraService {
    val apiUrl = ""
    val apiUsername = ""
    val apiPassword = ""

    def authentication: Authentication = Authentication("token", "url")
  }

}
