package services.api

import com.gu.identity.play.{IdMinimalUser, IdUser}
import com.gu.salesforce.{ContactId, Tier}
import com.gu.stripe.Stripe.Customer
import forms.MemberForm.{CommonForm, JoinForm}
import model.GenericSFContact
import monitoring.{ContributorMetrics, MemberMetrics}
import services.FrontendMemberRepository.UserId

import scala.concurrent.Future
import scalaz.\/

trait SalesforceService {
  def getMember(userId: UserId): Future[String \/ Option[GenericSFContact]]
  def metrics: MemberMetrics
  def contributorMetrics: ContributorMetrics
  def upsert(user: IdUser, userData: CommonForm): Future[ContactId]
  def isAuthenticated: Boolean
}
