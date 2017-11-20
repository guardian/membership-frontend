package model

import cats.syntax.either._
import com.gu.acquisition.model.{OphanIds, ReferrerAcquisitionData}
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import com.gu.memsub.{Product => _, _}
import com.gu.memsub.BillingPeriod.OneOffPeriod
import com.gu.salesforce.Tier
import ophan.thrift.event._

case class MembershipAcquisitionData(
  amountPaidToday: Price,
  billingPeriod: BillingPeriod,
  paymentMethod: Option[PaymentMethod],
  tier: Tier,
  countryCode: Option[String],
  pageviewId: Option[String],
  visitId: Option[String],
  browserId: Option[String],
  referrerAcquisitionData: Option[ReferrerAcquisitionData]) {

  import MembershipAcquisitionData._

  val product: Either[String, Product] = Either.fromOption(
    tierToProduct.get(tier),
    s"Won't create Acquisition event for ${tier.name} tier"
  )

  val paymentFrequency: PaymentFrequency = billingPeriodToPaymentFrequency(billingPeriod)

  val paymentProvider: Option[PaymentProvider] = paymentMethod.map(paymentMethodToProvider)
}

object MembershipAcquisitionData {

  private def billingPeriodToPaymentFrequency(period: BillingPeriod): PaymentFrequency = period match {
    case BillingPeriod.Month => PaymentFrequency.Monthly
    case BillingPeriod.Quarter => PaymentFrequency.Quarterly
    case BillingPeriod.SixMonthsRecurring => PaymentFrequency.SixMonthly
    case BillingPeriod.Year => PaymentFrequency.Annually
    case _: OneOffPeriod => PaymentFrequency.OneOff
  }

  private def paymentMethodToProvider(method: PaymentMethod): PaymentProvider = method match {
    case _: PaymentCard => PaymentProvider.Stripe
    case _: PayPalMethod => PaymentProvider.Paypal
    case _: GoCardless => PaymentProvider.Gocardless
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
      product = product,
      paymentFrequency = data.paymentFrequency,
      currency = data.amountPaidToday.currency.iso,
      amount = data.amountPaidToday.amount.toDouble,
      paymentProvider = data.paymentProvider,
      campaignCode = data.referrerAcquisitionData.map(_.campaignCode.toSet),
      abTests = data.referrerAcquisitionData.map(data => AbTestInfo(data.abTests.toSet.flatten ++ data.abTest)),
      countryCode = data.countryCode,
      referrerPageViewId = data.referrerAcquisitionData.flatMap(_.referrerPageviewId),
      referrerUrl = data.referrerAcquisitionData.flatMap(_.referrerUrl),
      componentId = data.referrerAcquisitionData.flatMap(_.componentId),
      componentTypeV2 = data.referrerAcquisitionData.flatMap(_.componentType),
      source = data.referrerAcquisitionData.flatMap(_.source),
      platform = Some(Platform.Membership)
    )
  }
}
