package actions

import scala.concurrent.Future

import play.api.mvc.{Call, Result, Request, ActionBuilder}
import play.api.mvc.Results._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.gu.membership.salesforce._

import services.{MemberRepository, AuthenticationService}
import controllers.NoCache

trait MemberAction extends ActionBuilder[MemberRequest] {
  val authService: AuthenticationService

  def invokeBlock[A](request: Request[A], block: MemberRequest[A] => Future[Result]) = {
    lazy val seeMiniMembershipTierChooser = {
      val miniTierChooser: Call = controllers.routes.Joining.tierChooser()

      SeeOther(miniTierChooser.absoluteURL(secure = true)(request)).addingToSession("preJoinReturnUrl" -> request.uri)(request)
    }

    authService.authenticatedRequestFor(request).map { authRequest =>
      for {
        memberOpt <- MemberRepository.get(authRequest.user.id)
        result <- memberOpt.map { member =>
          block(MemberRequest[A](request, member, authRequest.user))
        }.getOrElse(Future.successful(seeMiniMembershipTierChooser))
      } yield NoCache(result)
    }.getOrElse(Future.successful(seeMiniMembershipTierChooser))
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
        memberOpt <- MemberRepository.get(authRequest.user.id)
        result <- paidMemberRequestFor(memberOpt, authRequest).map(block).getOrElse(Future.successful(Forbidden))
      } yield NoCache(result)
    }.getOrElse(Future.successful(Forbidden))
  }
}

object PaidMemberAction extends PaidMemberAction {
  val authService = AuthenticationService
}
