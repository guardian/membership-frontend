package services.api

import com.gu.identity.play.IdMinimalUser
import com.gu.membership.salesforce.{ContactId, PaidTier}
import controllers.IdentityRequest
import forms.MemberForm.{FreeMemberChangeForm, JoinForm, PaidMemberChangeForm}
import model.{NonPaidSFMember, PaidSFMember}

import scala.concurrent.Future

trait MemberService {
  def createMember(user: IdMinimalUser,
                   formData: JoinForm,
                   identityRequest: IdentityRequest,
                   fromEventId: Option[String]): Future[ContactId]

  def upgradeFreeSubscription(freeMember: NonPaidSFMember,
                              newTier: PaidTier,
                              form: FreeMemberChangeForm,
                              identityRequest: IdentityRequest): Future[ContactId]

  def upgradePaidSubscription(paidMember: PaidSFMember,
                              newTier: PaidTier,
                              form: PaidMemberChangeForm,
                              identityRequest: IdentityRequest): Future[ContactId]
}
