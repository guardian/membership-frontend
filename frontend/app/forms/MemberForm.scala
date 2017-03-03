package forms

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets

import com.gu.i18n._
import com.gu.memsub.BillingPeriod._
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.PromoCode
import com.gu.memsub.{Address, BillingPeriod, FullName}
import com.gu.salesforce.Tier._
import com.gu.salesforce.{PaidTier, Tier}
import model._
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError, Mapping}
import play.api.libs.json.{JsValue, Json}

object MemberForm {
  case class NameForm(first: String, last: String) extends FullName

  case class SupportForm(
    name: String,
    currency: Currency,
    amount: BigDecimal,
    email: String,
    token: String,
    marketing: Boolean,
    postCode: Option[String],
    abTests: JsValue,
    ophanId: String,
    cmp: Option[String],
    intcmp: Option[String]

  )

  case class PaymentForm(billingPeriod: BillingPeriod, stripeToken: Option[String], payPalBaid: Option[String])

  case class MarketingChoicesForm(gnm: Option[Boolean], thirdParty: Option[Boolean])

  trait CommonForm {
    val name: NameForm
    val password: Option[String]
    val marketingChoices: MarketingChoicesForm
  }

  trait JoinForm extends CommonForm {
    val deliveryAddress: Address
    val trackingPromoCode: Option[PromoCode]
    val planChoice: PlanChoice
  }

  trait PaidMemberForm {
    val featureChoice: Set[FeatureChoice]
    val zuoraAccountAddress : Address
    val trackingPromoCode: Option[PromoCode]
    val promoCode: Option[PromoCode]
    val payment: PaymentForm
  }

  trait ContributorForm extends CommonForm {
    val planChoice: ContributionPlanChoice
    val featureChoice: Set[FeatureChoice]
    val payment: PaymentForm
  }

  case class FriendJoinForm(name: NameForm, deliveryAddress: Address, marketingChoices: MarketingChoicesForm,
                            password: Option[String], trackingPromoCode: Option[PromoCode]) extends JoinForm {
    override val planChoice: FreePlanChoice = FreePlanChoice(friend)
  }

  case class StaffJoinForm(name: NameForm, deliveryAddress: Address, marketingChoices: MarketingChoicesForm,
                            password: Option[String]) extends JoinForm {
    val trackingPromoCode = None
    override val planChoice: FreePlanChoice = FreePlanChoice(staff)
  }

  case class PaidMemberJoinForm(tier: PaidTier,
                                name: NameForm,
                                payment: PaymentForm,
                                deliveryAddress: Address,
                                billingAddress: Option[Address],
                                marketingChoices: MarketingChoicesForm,
                                password: Option[String],
                                casId: Option[String],
                                subscriberOffer: Boolean,
                                featureChoice: Set[FeatureChoice],
                                trackingPromoCode: Option[PromoCode],
                                promoCode: Option[PromoCode]
                               ) extends JoinForm with PaidMemberForm {

    lazy val zuoraAccountAddress = billingAddress.getOrElse(deliveryAddress)
    override val planChoice: PaidPlanChoice = PaidPlanChoice(tier, payment.billingPeriod)
  }

  case class MonthlyContributorForm(
                                name: NameForm,
                                payment: PaymentForm,
                                marketingChoices: MarketingChoicesForm,
                                password: Option[String],
                                casId: Option[String],
                                subscriberOffer: Boolean,
                                featureChoice: Set[FeatureChoice]
                               ) extends ContributorForm {
    override val planChoice = ContributorChoice(payment.billingPeriod)
  }



  case class AddressDetails(deliveryAddress: Address, billingAddress: Option[Address])

  trait MemberChangeForm {
    val featureChoice: Set[FeatureChoice]
    val addressDetails: Option[AddressDetails]
    val trackingPromoCode: Option[PromoCode]
    val promoCode: Option[PromoCode]
  }

  case class PaidMemberChangeForm(password: String, featureChoice: Set[FeatureChoice], trackingPromoCode: Option[PromoCode], promoCode: Option[PromoCode]) extends MemberChangeForm {
    val addressDetails = None
  }

  case class FreeMemberChangeForm(payment: PaymentForm,
                                  deliveryAddress: Address,
                                  billingAddress: Option[Address],
                                  featureChoice: Set[FeatureChoice],
                                  trackingPromoCode: Option[PromoCode],
                                  promoCode: Option[PromoCode]
                                  ) extends MemberChangeForm with PaidMemberForm {
    val addressDetails = Some(AddressDetails(deliveryAddress, billingAddress))
    lazy val zuoraAccountAddress = billingAddress.getOrElse(deliveryAddress)
  }


  val abTestFormatter: Formatter[JsValue] = new Formatter[JsValue] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError],JsValue] = {
      val parse: JsValue = Json.parse(URLDecoder.decode(data(key),StandardCharsets.UTF_8.name()))
      Right(parse)
    }
    override def unbind(key: String, data: JsValue): Map[String,String] = Map()

  }

  implicit val productFeaturesFormatter: Formatter[Set[FeatureChoice]] = new Formatter[Set[FeatureChoice]] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Set[FeatureChoice]] = {
      val inputVal = data.getOrElse(key, "")
      Right(FeatureChoice.setFromString(inputVal))
    }

    override def unbind(key: String, choices: Set[FeatureChoice]): Map[String, String] =
      Map(key -> FeatureChoice.setToString(choices))
  }

  implicit val countryFormatter: Formatter[Country] = new Formatter[Country] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Country] = {
      val countryCode = data.get(key)
      lazy val formError = FormError(key, s"Cannot find a country by country code ${countryCode.getOrElse("")}")

      countryCode
        .flatMap(CountryGroup.countryByCode)
        .toRight[Seq[FormError]](Seq(formError))
    }

    override def unbind(key: String, value: Country): Map[String, String] =
      Map(key -> value.alpha2)
  }



  private val productFeature = of[Set[FeatureChoice]] as productFeaturesFormatter


  private val country: Mapping[String] =
    text.verifying { code => CountryGroup.countryByCode(code).isDefined }
      .transform(
        { code => CountryGroup.countryByCode(code).fold("")(_.name)},
        { name => CountryGroup.countryByNameOrCode(name).fold("")(_.alpha2)})

  implicit val promoCodeFormatter: Formatter[PromoCode] = new Formatter[PromoCode] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], PromoCode] =
      data.get(key).filter(_.nonEmpty).map(PromoCode).toRight(Seq(FormError(key, "Cannot find a promo code")))

    override def unbind(key: String, value: PromoCode) = Map(key -> value.get)
  }

  implicit val currencyFormatter = new Formatter[Currency] {
    type Result = Either[Seq[FormError], Currency]
    override def bind(key: String, data: Map[String, String]): Result =
      data.get(key).map(_.toUpperCase).flatMap(Currency.fromString).fold[Result](Left(Seq.empty))(currency => Right(currency))
    override def unbind(key: String, value: Currency): Map[String, String] =
      Map(key -> value.identifier)
  }

  val trackingPromoCode = of[PromoCode] as promoCodeFormatter
  val promoCode = of[PromoCode] as promoCodeFormatter

  val nonPaidAddressMapping: Mapping[Address] = mapping(
    "lineOne" -> text,
    "lineTwo" -> text,
    "town" -> text,
    "countyOrState" -> text,
    "postCode" -> text(maxLength=20),
    "country" -> country
  )(Address.apply)(Address.unapply).verifying(_.valid)

  val paidAddressMapping: Mapping[Address] = mapping(
    "lineOne" -> nonEmptyText,
    "lineTwo" -> text,
    "town" -> nonEmptyText,
    "countyOrState" -> text,
    "postCode" -> text(maxLength=20),
    "country" -> country
  )(Address.apply)(Address.unapply).verifying(_.valid)

  val nameMapping: Mapping[NameForm] = mapping(
    "first" -> nonEmptyText,
    "last" -> nonEmptyText
  )(NameForm.apply)(NameForm.unapply)

  val marketingChoicesMapping: Mapping[MarketingChoicesForm] = mapping(
    "gnnMarketing" -> optional(boolean),
    "thirdParty" -> optional(boolean)
  )(MarketingChoicesForm.apply)(MarketingChoicesForm.unapply)

  val  paymentMapping: Mapping[PaymentForm] = mapping(
    "type" -> nonEmptyText.transform[BillingPeriod](b =>
      if (Seq("annual","subscriberOfferAnnual").contains(b)) Year else Month, _.noun),
    "stripeToken" -> optional(text),
    "payPalBaid" -> optional(text)
  )(PaymentForm.apply)(PaymentForm.unapply)



  val friendJoinForm: Form[FriendJoinForm] = Form(
    mapping(
      "name" -> nameMapping,
      "deliveryAddress" -> nonPaidAddressMapping,
      "marketingChoices" -> marketingChoicesMapping,
      "password" -> optional(nonEmptyText),
      "trackingPromoCode" -> optional(trackingPromoCode)
    )(FriendJoinForm.apply)(FriendJoinForm.unapply)
  )

  val staffJoinForm: Form[StaffJoinForm] = Form(
    mapping(
      "name" -> nameMapping,
      "deliveryAddress" -> nonPaidAddressMapping,
      "marketingChoices" -> marketingChoicesMapping,
      "password" -> optional(nonEmptyText)
    )(StaffJoinForm.apply)(StaffJoinForm.unapply)
  )

  val paidMemberJoinForm: Form[PaidMemberJoinForm] = Form(
    mapping(
      "tier" -> nonEmptyText.transform[PaidTier](PaidTier.slugMap, _.slug),
      "name" -> nameMapping,
      "payment" -> paymentMapping,
      "deliveryAddress" -> paidAddressMapping,
      "billingAddress" -> optional(paidAddressMapping),
      "marketingChoices" -> marketingChoicesMapping,
      "password" -> optional(nonEmptyText),
      "casId" -> optional(nonEmptyText),
      "subscriberOffer" -> default(boolean, false),
      "featureChoice" -> productFeature,
      "trackingPromoCode" -> optional(trackingPromoCode),
      "promoCode" -> optional(promoCode)
    )(PaidMemberJoinForm.apply)(PaidMemberJoinForm.unapply)
  )

  val monthlyContributorForm: Form[MonthlyContributorForm] = Form(
    mapping(
      "name" -> nameMapping,
      "payment" -> paymentMapping,
      "marketingChoices" -> marketingChoicesMapping,
      "password" -> optional(nonEmptyText),
      "casId" -> optional(nonEmptyText),
      "subscriberOffer" -> default(boolean, false),
      "featureChoice" -> productFeature
    )(MonthlyContributorForm.apply)(MonthlyContributorForm.unapply)
  )

  val freeMemberChangeForm: Form[FreeMemberChangeForm] = Form(
    mapping(
      "payment" -> paymentMapping,
      "deliveryAddress" -> paidAddressMapping,
      "billingAddress" -> optional(paidAddressMapping),
      "featureChoice" -> productFeature,
      "trackingPromoCode" -> optional(trackingPromoCode),
      "promoCode" -> optional(promoCode)
  )(FreeMemberChangeForm.apply)(FreeMemberChangeForm.unapply)
  )

  val paidMemberChangeForm: Form[PaidMemberChangeForm] = Form(
    mapping(
      "password" -> nonEmptyText,
      "featureChoice" -> productFeature,
      "trackingPromoCode" -> optional(trackingPromoCode),
      "promoCode" -> optional(promoCode)
    )(PaidMemberChangeForm.apply)(PaidMemberChangeForm.unapply)
  )



  val supportForm: Form[SupportForm] = Form(
    mapping(
      "name" -> nonEmptyText,
      "currency" -> of[Currency],
      "amount" -> bigDecimal(10, 2),
      "email" -> email,
      "payment.token" -> nonEmptyText,
      "guardian-opt-in" -> boolean,
      "postcode" -> optional(nonEmptyText),
      "abTest" -> FieldMapping[JsValue]()(abTestFormatter),
      "ophanId" -> text,
      "cmp" -> optional(text),
      "intcmp" -> optional(text)
    )(SupportForm.apply)(SupportForm.unapply)
  )
}
