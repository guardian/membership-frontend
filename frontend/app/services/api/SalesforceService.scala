package services.api

import com.gu.identity.play.{IdMinimalUser, IdUser}
import com.gu.membership.model.TierPlan
import com.gu.membership.salesforce.ContactId
import com.gu.membership.stripe.Stripe.Customer
import forms.MemberForm.JoinForm
import model.GenericSFContact
import monitoring.MemberMetrics

import scala.concurrent.Future

trait SalesforceService {
  def metrics: MemberMetrics
  def upsert(user: IdUser, userData: JoinForm): Future[ContactId]
  def updateMemberStatus(user: IdMinimalUser, tierPlan: TierPlan, customer: Option[Customer]): Future[ContactId]
  def getStripeCustomer(contact: GenericSFContact): Future[Option[Customer]]
}
