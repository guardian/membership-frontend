package utils

import actions._
import com.gu.i18n.CountryGroup
import controllers.IdentityRequest
import play.api.mvc.Request
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object RequestCountry {
  implicit class RequestWithFastlyCountry(r: Request[_]) {
    def getFastlyCountryCode = r.headers.get("X-GU-GeoIP-Country-Code").flatMap(CountryGroup.byFastlyCountryCode)
    def getFastlyCountry = r.headers.get("X-GU-GeoIP-Country-Code").flatMap(CountryGroup.countryByCode)
  }
  implicit class AuthenticatedRequestWithIdentity(r:AuthRequest[_])
  {
    def getIdentityCountryGroup = {
      implicit val identityRequest = IdentityRequest(r)
      r.touchpointBackend.identityService
        .getFullUserDetails(r.user)
        .map(
          _.privateFields
            .flatMap(_.country)
            .flatMap(CountryGroup.byCountryNameOrCode)
        )
    }
  }
}
