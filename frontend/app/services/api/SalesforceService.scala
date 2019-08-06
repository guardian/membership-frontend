package services.api

import model.IdMinimalUser
import com.gu.identity.model.{User => IdUser}
import com.gu.salesforce.{ContactId, Tier}
import com.gu.stripe.Stripe.Customer
import forms.MemberForm.CommonForm
import model.GenericSFContact
import monitoring.MemberMetrics
import services.FrontendMemberRepository.UserId
import scala.concurrent.Future
import scalaz.\/

trait SalesforceService {
  def getMember(userId: UserId): Future[String \/ Option[GenericSFContact]]
  def metrics: MemberMetrics
  def upsert(user: IdUser, userData: CommonForm): Future[ContactId]
  def updateMemberStatus(user: IdMinimalUser, tier: Tier, customer: Option[Customer]): Future[ContactId]
  def isAuthenticated: Boolean
}
