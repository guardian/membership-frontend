package actions

import com.gu.membership.salesforce.Tier
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
    val supporter = controllers.routes.Info.supporter().toString()
    val supporterUS = controllers.routes.Info.supporterUSA().toString()
    val competitions = controllers.routes.Info.offersAndCompetitions().toString()
    val events = controllers.routes.WhatsOn.list().toString()

    val campaignReferer = refererUrl.path match {
      case `supporter` => "SUPUK"
      case `supporterUS` => "SUPUS"
      case `competitions` => "COMP"
      case `events` => "EVT"
      case _ => "HOME"
    }

    val supporterTier = controllers.routes.Joiner.joinPaid(Tier.Supporter).toString()
    val partnerTier = controllers.routes.Joiner.joinPaid(Tier.Partner).toString()
    val patronTier = controllers.routes.Joiner.joinPaid(Tier.Patron).toString()
    val friendTier = controllers.routes.Joiner.joinPaid(Tier.Friend).toString()

    val campaignTier = path match {
      case `supporterTier` => Some("SUP")
      case `partnerTier` => Some("PAR")
      case `patronTier` => Some("PAT")
      case `friendTier` => Some("FRI")
      case _ => None
    }
    campaignTier.map {
      tier => Some(Seq(CAMPAIGN_SOURCE, campaignReferer, tier).mkString("_"))
    }
  }
}
