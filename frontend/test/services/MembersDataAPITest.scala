package services

import com.gu.salesforce.Tier
import com.gu.salesforce.Tier.patron
import org.scalatest.FreeSpec
import play.api.libs.json._
import services.MembersDataAPI.Attributes

class MembersDataAPITest extends FreeSpec {
  "A tier can be deserialized" in {
    assertResult(JsSuccess(patron))(
      Json.parse("\"patron\"").validate[Tier](MembersDataAPI.tierReads)
    )
  }

  "Attributes can be deserialized" in {
    assertResult(
      JsSuccess(Attributes(patron, Some("1234567abcdef")))
    )(
      Json.parse(
        """
          |{
          |  "membershipNumber": "1234567abcdef",
          |  "tier": "Patron"
          |}
        """.stripMargin).validate[Attributes](MembersDataAPI.attributesReads)
    )
  }

  "Errprs can be serialized" in {
    assertResult(
      JsSuccess(MembersDataAPI.ApiError("Bad Request", "Detailed error message"))
    )(
      Json.parse(
        """
          |{
          |  "message": "Bad Request",
          |  "details": "Detailed error message"
          |}
        """.stripMargin
      ).validate[MembersDataAPI.ApiError](MembersDataAPI.errorReads)
    )
  }
}
