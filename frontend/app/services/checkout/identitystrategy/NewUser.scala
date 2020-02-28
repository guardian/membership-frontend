package services.checkout.identitystrategy

import cats.data.EitherT
import cats.implicits._
import model.{CookieBuilder, CreateIdUser}
import com.gu.identity.model.{PublicFields, User => IdUser}
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
    PublicFields(),
    Some(IdentityService.privateFieldsFor(form)))
  )
}

case class NewUser(creationCommand: CreateIdUser)(implicit idReq: IdentityRequest, identityService: IdentityService) extends Strategy {

  def ensureIdUser(checkoutFunc: (IdUser) => Future[Result])(implicit executionContext: ExecutionContext) = (for {
    userRegAndAuthResponse <- identityService.createUser(creationCommand)
    result <- EitherT.right[String](checkoutFunc(userRegAndAuthResponse.user))
  } yield result.withCookies(CookieBuilder.cookiesFromDescription(userRegAndAuthResponse.cookies.get, Some(Config.guardianShortDomain)): _*)
    ).valueOr { error => Results.InternalServerError(error) }

}
