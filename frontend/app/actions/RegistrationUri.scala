package actions

import com.gu.membership.salesforce.Tier
import com.netaporter.uri.Uri
import configuration.Config
import play.api.mvc.RequestHeader

object RegistrationUri {
  val REFERRER_HEADER: String = "referer"
  val CAMPAIGN_SOURCE: String = "MEM"

  val referingPageCodes = Map(
    controllers.routes.Info.supporter().toString() -> "SUPUK",
    controllers.routes.Info.supporterUSA().toString() -> "SUPUS",
    controllers.routes.Info.offersAndCompetitions().toString()-> "COMP",
    controllers.routes.WhatsOn.list().toString() -> "EVT",
    "/" -> "HOME"
  )

  val campaignTierCodes = Map[String, String](
    controllers.routes.Joiner.joinPaid(Tier.Supporter).toString() -> "SUP",
    controllers.routes.Joiner.joinPaid(Tier.Partner).toString() -> "PAR",
    controllers.routes.Joiner.joinPaid(Tier.Patron).toString() -> "PAT",
    controllers.routes.Joiner.joinPaid(Tier.Friend).toString() -> "FRI"
  )

  def parse(request: RequestHeader) = {
    val redirectUrl: String = Config.idWebAppRegisterUrl(request.uri)
    val campaignCode = extractCampaignCode(request.headers.get(REFERRER_HEADER), request.path)
    redirectUrl.concat(s"&INTCMP=$campaignCode")
  }

  private def extractCampaignCode(refererOpt: Option[String], path: String) : String = {

    val referingCode = refererOpt.flatMap(getReferingPageCode)
    val campaignTier: Option[String] = campaignTierCodes.get(path)

    (Seq(CAMPAIGN_SOURCE) ++ referingCode ++ campaignTier).mkString("_")
  }

  def getReferingPageCode(referer: String): Option[String] = {
    val refererUrl = Uri.parse(referer)
    referingPageCodes.get(refererUrl.path)
  }

}
