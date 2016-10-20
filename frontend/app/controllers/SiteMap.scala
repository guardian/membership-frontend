package controllers

import com.gu.i18n.CountryGroup.NewZealand
import com.gu.i18n.{Country, CountryGroup}
import com.typesafe.scalalogging.LazyLogging
import model.ActiveCountryGroups
import play.api.mvc._

import scala.xml.Elem

object SiteMap extends Controller with LazyLogging {

  val countrySpecificGroups: Map[CountryGroup, Country] = (for {
    countryGroup: CountryGroup <- ActiveCountryGroups.all
    defaultCountry <- countryGroup.defaultCountry
  } yield countryGroup -> defaultCountry).toMap

  def sitemap() = CachedAction { implicit request =>

    val foo = <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
                      xmlns:xhtml="http://www.w3.org/1999/xhtml">
      {supporterPages}
      <url>
        <loc>{routes.FrontPage.index.absoluteURL()}</loc>
        <priority>0.8</priority>
      </url>
    </urlset>
    Ok(foo)
  }

  def supporterPages()(implicit req: RequestHeader): Iterable[Elem] = for {
    countryGroup <- countrySpecificGroups.keys
  } yield {
    <url>
      <loc>
        {routes.Info.supporterFor(countryGroup).absoluteURL()}
      </loc>
      <priority>1.0</priority>
      {alternatePages()}
    </url>
  }

  def alternatePages()(implicit req: RequestHeader) = for {
    (countrySpecificGroup, country) <- countrySpecificGroups
  } yield {
      <xhtml:link
      rel="alternate"
      hreflang={s"en-${country.alpha2.toLowerCase}"}
      href={routes.Info.supporterFor(countrySpecificGroup).absoluteURL()}/>
  }
}
