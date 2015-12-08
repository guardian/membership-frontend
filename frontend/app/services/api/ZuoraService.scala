package services.api

import com.gu.membership.salesforce.ContactId
import com.gu.membership.stripe.Stripe
import com.gu.membership.zuora.soap.models.Results.SubscribeResult
import forms.MemberForm.{JoinForm, PaidMemberJoinForm}

import scala.concurrent.Future

trait ZuoraService {
  def createPaidSubscription(memberId: ContactId,
                             joinData: PaidMemberJoinForm,
                             customer: Stripe.Customer): Future[SubscribeResult]

  def createFreeSubscription(memberId: ContactId,
                             joinData: JoinForm): Future[SubscribeResult])

}
