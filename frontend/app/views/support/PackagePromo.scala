package views.support

import com.gu.i18n.CountryGroup
import com.gu.salesforce.{FreeTier, PaidTier, Tier}
import io.lemonlabs.uri.Uri
import io.lemonlabs.uri.dsl._
import controllers.routes
import model.PackagePromo.CtaButton
import play.twirl.api.Html


object PackagePromo {

  implicit class AttrsToHtml(attrs: Map[String, String]) {
    // no escaping of the attribute values happens here as twirl does it
    def html: String = attrs.map { case (k, v) => s"""$k=$v""" }.mkString(" ")
  }
}
