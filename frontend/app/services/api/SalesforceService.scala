package services.api

import com.gu.identity.play.{IdMinimalUser, IdUser}
import com.gu.salesforce.{ContactId, Tier}
import com.gu.stripe.Stripe.Customer
import forms.MemberForm.JoinForm
import model.GenericSFContact
import monitoring.MemberMetrics
import services.FrontendMemberRepository.UserId

import scala.concurrent.Future

trait SalesforceService {
  def getMember(userId: UserId): Future[Option[GenericSFContact]]
  def metrics: MemberMetrics
  def upsert(user: IdUser, userData: JoinForm): Future[ContactId]
  def updateMemberStatus(user: IdMinimalUser, tier: Tier, customer: Option[Customer]): Future[ContactId]
}
