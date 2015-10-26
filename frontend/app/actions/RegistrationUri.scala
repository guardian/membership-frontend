package actions

import com.netaporter.uri.Uri
import configuration.Config
import play.api.mvc.RequestHeader

object RegistrationUri {
  val REFERRER_HEADER: String = "referer"
  val CAMPAIGN_SOURCE: String = "MEM"

  def parse(request: RequestHeader) = {
    val redirectUrl: String = Config.idWebAppRegisterUrl(request.uri)
    extractCampaignCode(request.headers(REFERRER_HEADER), request.path) match {
      case Some(campaignCode) => redirectUrl.concat(s"&INTCMP=${campaignCode.get}")
      case _ => redirectUrl
    }
  }

  private def extractCampaignCode(referer: String, path: String) = {
    val refererUrl = Uri.parse(referer)


//    println(controllers.routes.Joiner.tierChooser())

    val campaignReferer = refererUrl.path match {
      case "/supporter" => "SUPUK"
      case "/us/supporter" => "SUPUS"
      case "/offers-competitions" => "COMP"
      case "/events" => "EVT"
      case _ => "HOME"
    }
    val campaignTier = path match {
      case "/join/supporter/enter-details" => Some("SUP")
      case "/join/partner/enter-details" => Some("PAR")
      case "/join/patron/enter-details" => Some("PAT")
      case "/join/friend/enter-details" => Some("FRI")
      case _ => None
    }
    campaignTier.map {
      tier => Some(Seq(CAMPAIGN_SOURCE, campaignReferer, tier).mkString("_"))
    }
  }
}
