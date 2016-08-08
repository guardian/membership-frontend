package services.api

import com.gu.identity.play.IdUser
import com.gu.salesforce.ContactId
import forms.MemberForm.JoinForm
import model.GenericSFContact
import monitoring.MemberMetrics
import services.FrontendMemberRepository.UserId

import scala.concurrent.Future

trait SalesforceService {
  def getMember(userId: UserId): Future[Option[GenericSFContact]]
  def metrics: MemberMetrics
  def upsert(user: IdUser, userData: JoinForm): Future[ContactId]
}
