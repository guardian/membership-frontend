package controllers

import com.gu.i18n.CountryGroup
import com.typesafe.scalalogging.LazyLogging
import model.ActiveCountryGroups
import play.api.mvc._

import scala.xml.Elem

object SiteMap extends Controller with LazyLogging {

  def sitemap() = CachedAction { implicit request =>
    val foo = <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
                      xmlns:xhtml="http://www.w3.org/1999/xhtml">
      {supporterPages}
      <url>
        <loc>{routes.FrontPage.index.absoluteURL(secure = true)}</loc>
        <priority>0.8</priority>
        {alternatePage(routes.FrontPage.index.absoluteURL(secure = true), "x-default")}
      </url>
    </urlset>
    Ok(foo)
  }

  private def supporterPages()(implicit req: RequestHeader): Iterable[Elem] = for {
    countryGroup <- ActiveCountryGroups.all
  } yield {
    <url>
      <loc>
        {routes.Info.supporterFor(countryGroup).absoluteURL(secure = true)}
      </loc>
      <priority>1.0</priority>
      {alternatePages(ActiveCountryGroups.all)}
      {alternatePage(routes.Info.supporterFor(countryGroup).absoluteURL(secure = true), "x-default")}
    </url>
  }

  private def alternatePages(alternateCountryGroups: Seq[CountryGroup])(implicit req: RequestHeader) = for {
    countryGroup <- alternateCountryGroups
  } yield {
      alternatePage(
        routes.Info.supporterFor(countryGroup).absoluteURL(secure = true),
        countryGroup.defaultCountry.map(c => s"en-${c.alpha2.toUpperCase}").getOrElse("en")
      )
  }

  private def alternatePage(href: String, hreflang: String) =
      <xhtml:link rel="alternate" hreflang={hreflang} href={href} />
}
