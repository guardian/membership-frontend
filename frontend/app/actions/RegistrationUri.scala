package actions

import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup.RestOfTheWorld
import com.gu.salesforce.Tier
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import configuration.Config
import play.api.mvc.RequestHeader
import tracking.RedirectWithCampaignCodes.internalCampaignCode
import scala.util.Try

object RegistrationUri {
  val REFERRER_HEADER: String = "referer"
  val CAMPAIGN_SOURCE: String = "MEM"

  val supporterReferingPageCodes =  Map(CountryGroup.allGroups.map { countryGroup =>
    // sadly 'SUPIN' is the refering page code in use - but our country-group code is 'int'
    val suffix = if (countryGroup == RestOfTheWorld) "IN" else countryGroup.id.toUpperCase

    controllers.routes.Info.supporterFor(countryGroup).path -> s"SUP$suffix"
  }: _*)

  val referingPageCodes = supporterReferingPageCodes ++ Map(
    controllers.routes.Info.offersAndCompetitions().path-> "COMP",
    controllers.routes.WhatsOn.list().path -> "EVT",
    "/" -> "HOME"
  )

  val campaignTierCodes = Map[String, String](
    controllers.routes.Joiner.joinPaid(Tier.supporter).path -> "SUP",
    controllers.routes.Joiner.joinPaid(Tier.partner).path -> "PAR",
    controllers.routes.Joiner.joinPaid(Tier.patron).path -> "PAT",
    controllers.routes.Joiner.joinFriend().path -> "FRI"
  )

  def parse(request: RequestHeader): String = {
    val base = Uri.parse(Config.idWebAppRegisterUrl(request.uri))
    val campaignCode = extractCampaignCode(request.headers.get(REFERRER_HEADER), request.path)
    val redirectUrl = base & (internalCampaignCode -> campaignCode)
    redirectUrl.toString
  }

  private def extractCampaignCode(refererOpt: Option[String], path: String) : String = {

    val referingCode = refererOpt.flatMap(getReferingPageCode)
    val campaignTier: Option[String] = campaignTierCodes.get(path)

    (Seq(CAMPAIGN_SOURCE) ++ referingCode ++ campaignTier).mkString("_")
  }

  def getReferingPageCode(referer: String): Option[String] = {
    Try(Uri.parse(referer))
      .toOption
      .flatMap(refererUrl => referingPageCodes.get(refererUrl.path))
  }

}
