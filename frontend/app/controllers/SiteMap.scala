package controllers

import actions.CommonActions
import com.gu.i18n.CountryGroup
import model.ActiveCountryGroups
import play.api.mvc._
import utils.CountryGroupLang

import scala.xml.Elem

class SiteMap(commonActions: CommonActions, override protected val controllerComponents: ControllerComponents) extends BaseController {

  import commonActions.CachedAction

  def sitemap() = CachedAction { implicit request =>
    val foo = <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
                      xmlns:xhtml="http://www.w3.org/1999/xhtml">
      <url>
        <loc>{routes.WhatsOn.list().absoluteURL(secure = true)}</loc>
        <priority>1</priority>
      </url>
      <url>
        <loc>{routes.WhatsOn.masterclassesList().absoluteURL(secure = true)}</loc>
        <priority>1</priority>
      </url>
    </urlset>
    Ok(foo)
  }
}
