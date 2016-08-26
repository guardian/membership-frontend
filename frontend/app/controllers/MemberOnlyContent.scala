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
import views.support.PageInfo

object MemberOnlyContent extends Controller with LazyLogging {

  val contentApiService = GuardianContentService

  def membershipContent(referringContent: String, membershipAccess: String) = CachedAction.async { implicit request =>
    val accessOpt = ContentAccess.valueOf(membershipAccess)
    contentApiService.contentItemQuery(Uri.parse(referringContent).path.stripPrefix("/")).map { response =>

      val signInUrl = ((Config.idWebAppUrl / "signin") ? ("returnUrl" -> ("https://theguardian.com/" + referringContent)) ? ("skipConfirmation" -> "true")).toString

      implicit val countryGroup = UK
      val pageInfo = PageInfo(
        title = CopyConfig.copyTitleChooseTier,
        url = request.path,
        description = Some(CopyConfig.copyDescriptionChooseTier),
        customSignInUrl = Some(signInUrl)
      )

      (for {
        content <- response.content
      } yield {
        if (content.fields.exists(_.membershipAccess.nonEmpty)) {
          Ok(views.html.joiner.membershipContent(pageInfo, accessOpt, signInUrl, CapiContent(content))).
            withSession(request.session + DestinationService.JoinReferrer -> ("https://theguardian.com/" + referringContent))
        } else {
          Redirect(("https://theguardian.com/" + referringContent))
        }
      }).getOrElse(
        Redirect(routes.Joiner.tierChooser())
      )
    }
  }
}
