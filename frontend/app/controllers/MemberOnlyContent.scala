package controllers

import com.gu.contentapi.client.model.v1.{MembershipTier => ContentAccess}
import com.gu.i18n.CountryGroup.UK
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import configuration.{Config, CopyConfig}
import model._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import services.{GuardianContentService, _}
import tracking.ActivityTracking
import views.support.PageInfo

object MemberOnlyContent extends Controller with ActivityTracking
                                 with LazyLogging {

  val contentApiService = GuardianContentService

  def membershipContent(referringContent: String, membershipAccess: String) = NoCacheAction.async { implicit request =>
    val accessOpt = ContentAccess.valueOf(membershipAccess)
    contentApiService.contentItemQuery(Uri.parse(referringContent).path.stripPrefix("/")).map { response =>

      val signInUrl = ((Config.idWebAppUrl / "signin") ? ("returnUrl" -> referringContent) ? ("skipConfirmation" -> "true")).toString

      implicit val countryGroup = UK
      val pageInfo = PageInfo(
        title = CopyConfig.copyTitleChooseTier,
        url = request.path,
        description = Some(CopyConfig.copyDescriptionChooseTier),
        customSignInUrl = Some(signInUrl)
      )

      response.content.map { c =>
        Ok(views.html.joiner.membershipContent(TouchpointBackend.Normal.catalog, pageInfo, accessOpt, signInUrl, CapiContent(c)))
          .withSession(request.session.copy(data = request.session.data + (Joiner.JoinReferrer -> referringContent)))
      }.getOrElse(Redirect(routes.Joiner.tierChooser()))
    }
  }
}
