package controllers

import actions.CommonActions
import com.gu.contentapi.client.model.ContentApiError
import com.gu.i18n.CountryGroup._
import io.lemonlabs.uri.Uri
import io.lemonlabs.uri.dsl._
import configuration.Config
import model._
import play.api.mvc.Results.InternalServerError
import play.api.mvc._
import services.{GuardianContentService, _}
import views.support.PageInfo

import scala.concurrent.{ExecutionContext, Future}

class MemberOnlyContent(contentApiService: GuardianContentService, commonActions: CommonActions, implicit val executionContext: ExecutionContext, override protected val controllerComponents: ControllerComponents) extends BaseController {

  import commonActions.CachedAction

  def membershipContentRedirect = Action { Redirect("/supporter") }

  def membershipContent(referringContentOpt: Option[String] = None) = CachedAction.async { implicit request =>
    referringContentOpt.fold(Future(Redirect((routes.Info.supporterRedirect(None))))){
        referringContent =>
        contentApiService.contentItemQuery(Uri.parse(referringContent).path.toString().stripPrefix("/")).map { response =>
          val signInUrl = ((Config.idWebAppUrl / "signin") ? ("returnUrl" -> ("https://theguardian.com/" + referringContent)) ? ("skipConfirmation" -> "true")).toString
          implicit val countryGroup = UK

          (for {
            content <- response.content
          } yield {
            if (content.fields.exists(_.membershipAccess.nonEmpty)) {
              val capiContent: CapiContent = CapiContent(content)
              val headline: String = capiContent.headline
              val pageInfo = PageInfo(
                title = headline,
                url = request.path,
                description = capiContent.trailText,
                customSignInUrl = Some(signInUrl),
                image = capiContent.mainPicture.map(_.defaultImage)
              )
              Ok(views.html.joiner.membershipContent(pageInfo, signInUrl, capiContent, s"Exclusive Members Content: $headline")).
                withSession(request.session + (DestinationService.JoinReferrer -> ("https://" + Config.guardianHost + "/" + referringContent)))
            } else {
              Redirect(("https://theguardian.com/" + referringContent))
            }
          }).getOrElse(
            Redirect(routes.Redirects.supportRedirect())
          )
      }.recoverWith {
          case ContentApiError(404, _, _) => Future.successful(NotFound(views.html.error404()))
          case ex => Future.successful(InternalServerError(views.html.error500(ex)))
        }
    }
  }
}
