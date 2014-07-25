package actions

import scala.concurrent.Future

import play.api.mvc.{Request, ActionBuilder}
import play.api.mvc.Results.Forbidden
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import services.{MemberService, AuthenticationService}
import controllers.NoCache
import play.api.mvc.Result
import model.{Tier, Member}

trait MemberAction extends ActionBuilder[MemberRequest] {
  val authService: AuthenticationService

  def invokeBlock[A](request: Request[A], block: MemberRequest[A] => Future[Result]) = {
    authService.authenticatedRequestFor(request).map { authRequest =>
      for {
        memberOpt <- MemberService.get(authRequest.user.id)
        result <- memberOpt.filter(_.tier > Tier.None).map { member =>
          block(MemberRequest[A](request, member, authRequest.user))
        }.getOrElse(Future.successful(Forbidden))
      } yield NoCache(result)
    }.getOrElse(Future.successful(Forbidden))
  }
}

object MemberAction extends MemberAction {
  val authService = AuthenticationService
}

trait PaidMemberAction extends ActionBuilder[PaidMemberRequest] {
  val authService: AuthenticationService

  def paidMemberRequestFor[A](memberOpt: Option[Member], authRequest: AuthRequest[A]): Option[PaidMemberRequest[A]] = {
    for {
      member <- memberOpt
      stripeCustomerId <- member.stripeCustomerId
    } yield PaidMemberRequest[A](authRequest, member, stripeCustomerId, authRequest.user)
  }

  def invokeBlock[A](request: Request[A], block: PaidMemberRequest[A] => Future[Result]) = {
    authService.authenticatedRequestFor(request).map { authRequest =>
      for {
        memberOpt <- MemberService.get(authRequest.user.id)
        result <- paidMemberRequestFor(memberOpt, authRequest).map(block).getOrElse(Future.successful(Forbidden))
      } yield NoCache(result)
    }.getOrElse(Future.successful(Forbidden))
  }
}

object PaidMemberAction extends PaidMemberAction {
  val authService = AuthenticationService
}