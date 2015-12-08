package services

import com.gu.identity.play.IdMinimalUser
import com.gu.membership.salesforce.{ContactId, PaidTier}
import com.gu.membership.stripe.{Stripe, StripeService}
import com.gu.membership.util.Timing
import controllers.IdentityRequest
import forms.MemberForm.{FreeMemberChangeForm, JoinForm, PaidMemberChangeForm, PaidMemberJoinForm}
import model.{NonPaidSFMember, PaidSFMember}
import play.api.Logger
import tracking.{ActivityTracking, EventActivity, EventData, MemberData}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class MemberService(
  identityService: IdentityService,
  salesforceService: api.SalesforceService,
  zuoraService: api.ZuoraService,
  stripeService: StripeService) extends api.MemberService with ActivityTracking {

  import EventbriteService._

  private val logger = Logger(getClass)

  override def createMember(user: IdMinimalUser,
                   formData: JoinForm,
                   identityRequest: IdentityRequest,
                   fromEventId: Option[String]): Future[ContactId] = {

    val tier = formData.plan.tier

    val createContact: Future[ContactId] =
      for {
        user <- identityService.getFullUserDetails(user, identityRequest)
        contactId <- salesforceService.upsert(user, formData)
      } yield contactId

    Timing.record(salesforceService.metrics, "createMember") {
      formData.password.foreach(
        identityService.updateUserPassword(_, identityRequest, user.id))

      formData.password.foreach(identityService.updateUserPassword(_, identityRequest, user.id))

      val contactId = formData match {
        case paid: PaidMemberJoinForm =>
          for {
            customer <- stripeService.Customer.create(user.id, paid.payment.token)
            cId <- createContact
            subscription <- zuoraService.createPaidSubscription(cId, paid, customer)
            updatedMember <- salesforceService.updateMemberStatus(user, formData.plan, Some(customer))
          } yield cId
        case _ =>
          for {
            cId <- createContact
            subscription <- zuoraService.createFreeSubscription(cId, formData)
            updatedMember <- salesforceService.updateMemberStatus(user, formData.plan, None)
          } yield cId
      }

      contactId.map { cId =>
        identityService.updateUserFieldsBasedOnJoining(user, formData, identityRequest)

        salesforceService.metrics.putSignUp(formData.plan)
        trackRegistration(formData, cId, user)
        cId
      }
    }.andThen {
      case Success(contactId) =>
        logger.debug(s"createMember() success user=${user.id} memberAccount=$contactId")
        fromEventId.flatMap(EventbriteService.getBookableEvent).foreach { event =>
          event.service.wsMetrics.put(s"join-${tier.name}-event", 1)
          val memberData = MemberData(contactId.salesforceContactId, user.id, tier.name)
          track(EventActivity("membershipRegistrationViaEvent", Some(memberData), EventData(event)), user)
        }
      case Failure(error: Stripe.Error) => logger.warn(s"Stripe API call returned error: '${error.getMessage()}' for user ${user.id}")
      case Failure(error) =>
        logger.error(s"Error in createMember() user=${user.id}", error)
        salesforceService.metrics.putFailSignUp(formData.plan)
    }
  }

  override def upgradeFreeSubscription(freeMember: NonPaidSFMember,
                              newTier: PaidTier,
                              form: FreeMemberChangeForm,
                              identityRequest: IdentityRequest): Future[ContactId] = ???

  override def upgradePaidSubscription(paidMember: PaidSFMember,
                              newTier: PaidTier,
                              form: PaidMemberChangeForm,
                              identityRequest: IdentityRequest): Future[ContactId] = ???

}
