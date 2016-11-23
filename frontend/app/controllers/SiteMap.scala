package controllers

import com.gu.i18n.CountryGroup
import com.typesafe.scalalogging.LazyLogging
import model.ActiveCountryGroups
import play.api.mvc._
import utils.CountryGroupLang

import scala.xml.Elem



object SiteMap extends Controller with LazyLogging {

  def sitemap() = CachedAction { implicit request =>
    val foo = <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
                      xmlns:xhtml="http://www.w3.org/1999/xhtml">
      {supporterPages}
      <url>
        <loc>{routes.FrontPage.index.absoluteURL(secure = true)}</loc>
        <priority>0.8</priority>
      </url>
    </urlset>
    Ok(foo)
  }

  def supporterPages()(implicit req: RequestHeader): Iterable[Elem] = for {
    countryGroup <- CountryGroupLang.langByCountryGroup.keys
  } yield {
    <url>
      <loc>
        {routes.Info.supporterFor(countryGroup).absoluteURL(secure = true)}
      </loc>
      <priority>1.0</priority>
      {alternatePages()}
    </url>
  }

  def alternatePages()(implicit req: RequestHeader) = for {
    (countrySpecificGroup, lang) <- CountryGroupLang.langByCountryGroup
  } yield {
      <xhtml:link
      rel="alternate"
      hreflang={lang}
      href={routes.Info.supporterFor(countrySpecificGroup).absoluteURL(secure = true)}/>
  }
}
