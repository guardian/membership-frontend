package model

import cats.syntax.either._
import com.gu.acquisition.model.{OphanIds, ReferrerAcquisitionData}
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import com.gu.memsub.{BillingPeriod, Price}
import com.gu.memsub.BillingPeriod.OneOffPeriod
import com.gu.salesforce.Tier
import com.gu.zuora.soap.models.Commands
import com.gu.zuora.soap.models.Commands.{CreditCardReferenceTransaction, PayPalReferenceTransaction}
import forms.MemberForm.{JoinForm, PaidMemberJoinForm}
import ophan.thrift.event._

case class MembershipAcquisitionData(
  amountPaidToday: Price,
  billingPeriod: BillingPeriod,
  paymentMethod: Option[Commands.PaymentMethod],
  form: JoinForm,
  tier: Tier,
  visitId: Option[String],
  browserId: Option[String],
  referrerAcquisitionData: Option[ReferrerAcquisitionData]) {

  import MembershipAcquisitionData._

  val product: Either[String, Product] = Either.fromOption(
    tierToProduct.get(tier),
    s"Won't create Acquisition event for ${tier.name} tier"
  )

  val paymentFrequency: PaymentFrequency = billingPeriodToPaymentFrequency(billingPeriod)

  val paymentProvider: Option[PaymentProvider] = paymentMethod.flatMap(paymentMethodToProvider)

  val pageviewId: Option[String] = form match {
    case paid: PaidMemberJoinForm => paid.pageviewId
    case _ => None
  }
}

object MembershipAcquisitionData {

  private def billingPeriodToPaymentFrequency(period: BillingPeriod): PaymentFrequency = period match {
    case BillingPeriod.Month => PaymentFrequency.Monthly
    case BillingPeriod.Quarter => PaymentFrequency.Quarterly
    case BillingPeriod.SixMonthsRecurring => PaymentFrequency.SixMonthly
    case BillingPeriod.Year => PaymentFrequency.Annually
    case _: OneOffPeriod => PaymentFrequency.OneOff
  }

  private def paymentMethodToProvider(method: Commands.PaymentMethod): Option[PaymentProvider] = method match {
    case _: CreditCardReferenceTransaction => Some(PaymentProvider.Stripe)
    case _: PayPalReferenceTransaction => Some(PaymentProvider.Paypal)
    case _ => None
  }

  private val tierToProduct: Map[Tier, Product] = Map(
    Tier.partner -> Product.MembershipPartner,
    Tier.patron -> Product.MembershipPatron,
    Tier.supporter -> Product.MembershipSupporter
  )

  implicit val acquisitionSubmissionBuilder = new AcquisitionSubmissionBuilder[MembershipAcquisitionData] {

    override def buildOphanIds(data: MembershipAcquisitionData): Either[String, OphanIds] =
      Right(OphanIds(data.pageviewId, data.visitId, data.browserId))

    override def buildAcquisition(data: MembershipAcquisitionData): Either[String, Acquisition] = for {
      product <- data.product
    } yield Acquisition(
      product,
      data.paymentFrequency,
      data.amountPaidToday.currency.iso,
      data.amountPaidToday.amount.toDouble,
      data.paymentProvider,
      data.referrerAcquisitionData.map(_.campaignCode.toSet),
      data.referrerAcquisitionData.map(data => AbTestInfo(data.abTest.toSet)),
      data.form.deliveryAddress.country.map(_.alpha2),
      data.referrerAcquisitionData.flatMap(_.referrerPageviewId),
      data.referrerAcquisitionData.flatMap(_.referrerUrl),
      data.referrerAcquisitionData.flatMap(_.componentId),
      data.referrerAcquisitionData.flatMap(_.componentType),
      data.referrerAcquisitionData.flatMap(_.source)
    )
  }
}
