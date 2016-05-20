package views.support

import com.gu.i18n.CountryGroup
import com.gu.salesforce.{FreeTier, PaidTier, Tier}
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import controllers.routes
import model.PackagePromo.CtaButton
import play.twirl.api.Html


object PackagePromo {

  /**
    * A default CTA button for a country & tier which goes to enter details
    */
  def forCountryTier(t: Tier, cg: CountryGroup, promoCode: Option[String]) = {

    val link = (t match {
      case p: PaidTier => Uri.parse(routes.Joiner.enterPaidDetails(p).url)
      case _: FreeTier => Uri.parse(routes.Joiner.enterFriendDetails().url)
    }) ? ("countryGroup" -> cg.id) & ("promoCode" -> promoCode)

    val attrs = Map[String, String](
      "data-metric-trigger" -> "click",
      "data-metric-category" -> "join",
      "data-metric-action" -> t.slug
    )

    CtaButton("Become a " + t.name, to = link, attributes = attrs)
  }

  implicit class AttrsToHtml(attrs: Map[String, String]) {
    // no escaping of the attribute values happens here as twirl does it
    def html: String = attrs.map { case (k, v) => s"""$k=$v""" }.mkString(" ")
  }
}
