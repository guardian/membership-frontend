package controllers

import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup._
import configuration.CopyConfig
import controllers.Redirects.redirectToSupporterPage
import forms.MemberForm._
import model._
import play.api.mvc.Controller
import services.{AuthenticationService, EmailService, GuardianContentService, TouchpointBackend}
import views.support.{Asset, PageInfo}
import com.netaporter.uri.dsl._
import com.netaporter.uri.Uri
import scala.concurrent.Future

object Giraffe extends Controller {
  def support = CachedAction { implicit request =>
    Ok(views.html.giraffe.support(PageInfo(
        title = "Support",
        url = request.path,
        description = Some("Support the Guardian")
    )))
  }
}
