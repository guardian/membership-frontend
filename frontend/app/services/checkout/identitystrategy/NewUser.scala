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
import services.checkout.identitystrategy.Strategy.identityService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object NewUser {
  def strategyFrom(form: CommonForm)(implicit idReq: IdentityRequest) = for {
    paidMemberJoinForm <- Option(form).collect { case p: PaidMemberJoinForm => p }
    password <- paidMemberJoinForm.password
  } yield NewUser(CreateIdUser(
    paidMemberJoinForm.email,
    password,
    PublicFields(displayName = Some(s"${form.name.first} ${form.name.last}")),
    Some(IdentityService.privateFieldsFor(form))
  ))
}

case class NewUser(creationCommand: CreateIdUser)(implicit idReq: IdentityRequest) extends Strategy {

  def ensureIdUser(checkoutFunc: (IdUser) => Future[Result]) = (for {
    userRegAndAuthResponse <- identityService.createUser(creationCommand)
    result <- EitherT.right[Future, String, Result](checkoutFunc(userRegAndAuthResponse.user))
  } yield result.withCookies(cookiesFromDescription(userRegAndAuthResponse.cookies.get, Some(Config.guardianShortDomain)): _*)
    ).valueOr { error => Results.InternalServerError(error) }

}
