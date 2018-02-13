package services.checkout.identitystrategy

import cats.data.EitherT
import cats.implicits._
import com.gu.identity.play.CookieBuilder.cookiesFromDescription
import com.gu.identity.play.idapi.CreateIdUser
import com.gu.identity.play.{IdUser, PublicFields}
import configuration.Config
import controllers.IdentityRequest
import forms.MemberForm.{CommonForm, PaidMemberJoinForm}
import play.api.mvc.{Result, Results}
import services.IdentityService

import scala.concurrent.{ExecutionContext, Future}

object NewUser {
  def strategyFrom(form: CommonForm)(implicit idReq: IdentityRequest, identityService: IdentityService) = for {
    paidMemberJoinForm <- Option(form).collect { case p: PaidMemberJoinForm => p }
    password <- paidMemberJoinForm.password
  } yield NewUser(CreateIdUser(
    paidMemberJoinForm.email,
    password,
    PublicFields(displayName = Some(s"${form.name.first} ${form.name.last}")),
    Some(IdentityService.privateFieldsFor(form)))
  )
}

case class NewUser(creationCommand: CreateIdUser)(implicit idReq: IdentityRequest, identityService: IdentityService) extends Strategy {

  def ensureIdUser(checkoutFunc: (IdUser) => Future[Result])(implicit executionContext: ExecutionContext) = (for {
    userRegAndAuthResponse <- identityService.createUser(creationCommand)
    result <- EitherT.right[String](checkoutFunc(userRegAndAuthResponse.user))
  } yield result.withCookies(cookiesFromDescription(userRegAndAuthResponse.cookies.get, Some(Config.guardianShortDomain)): _*)
    ).valueOr { error => Results.InternalServerError(error) }

}
