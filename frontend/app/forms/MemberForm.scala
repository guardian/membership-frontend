package forms

import com.gu.i18n._
import com.gu.memsub.BillingPeriod._
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.PromoCode
import com.gu.memsub.{Address, BillingPeriod, FullName}
import com.gu.salesforce.Tier._
import com.gu.salesforce.{PaidTier, Tier}
import model.{FeatureChoice, FreePlanChoice, PaidPlanChoice, PlanChoice}
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{Form, FormError, Mapping}

object MemberForm {
  case class NameForm(first: String, last: String) extends FullName

  case class SupportForm(
    name: String,
    currency: Currency,
    amount: BigDecimal,
    email: String,
    token: String,
    marketing: Boolean,
    postCode: Option[String]
  )

  case class PaymentForm(billingPeriod: BillingPeriod, token: String)

  case class MarketingChoicesForm(gnm: Option[Boolean], thirdParty: Option[Boolean])

  trait JoinForm {
    val name: NameForm
    val deliveryAddress: Address
    val marketingChoices: MarketingChoicesForm
    val password: Option[String]
    val planChoice: PlanChoice
  }

  case class FriendJoinForm(name: NameForm, deliveryAddress: Address, marketingChoices: MarketingChoicesForm,
                            password: Option[String]) extends JoinForm {
    override val planChoice: FreePlanChoice = FreePlanChoice(friend)
  }

  case class StaffJoinForm(name: NameForm, deliveryAddress: Address, marketingChoices: MarketingChoicesForm,
                            password: Option[String]) extends JoinForm {
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
                                suppliedPromoCode: Option[PromoCode]
                               ) extends JoinForm {

    lazy val zuoraAccountAddress = billingAddress.getOrElse(deliveryAddress)
    override val planChoice: PaidPlanChoice = PaidPlanChoice(tier, payment.billingPeriod)
  }

  case class AddressDetails(deliveryAddress: Address, billingAddress: Option[Address])

  trait MemberChangeForm {
    val featureChoice: Set[FeatureChoice]
    val addressDetails: Option[AddressDetails]
    val promoCode: Option[PromoCode]
  }

  case class PaidMemberChangeForm(password: String, featureChoice: Set[FeatureChoice], promoCode: Option[PromoCode]) extends MemberChangeForm {
    val addressDetails = None
  }

  case class FreeMemberChangeForm(payment: PaymentForm,
                                  deliveryAddress: Address,
                                  billingAddress: Option[Address],
                                  featureChoice: Set[FeatureChoice],
                                  promoCode: Option[PromoCode]
                                  ) extends MemberChangeForm {
    val addressDetails = Some(AddressDetails(deliveryAddress, billingAddress))
  }

  case class FeedbackForm(category: String, page: String, feedback: String, name: String, email: String)

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
      if (Seq("annual","subscriberOfferAnnual").contains(b)) year else month, _.noun),
    "token" -> nonEmptyText
  )(PaymentForm.apply)(PaymentForm.unapply)

  val feedbackMapping: Mapping[FeedbackForm] =   mapping(
    "category" -> nonEmptyText,
    "page" -> text,
    "feedback" -> nonEmptyText,
    "name" -> nonEmptyText,
    "email" -> email
  )(FeedbackForm.apply)(FeedbackForm.unapply)

  val friendJoinForm: Form[FriendJoinForm] = Form(
    mapping(
      "name" -> nameMapping,
      "deliveryAddress" -> nonPaidAddressMapping,
      "marketingChoices" -> marketingChoicesMapping,
      "password" -> optional(nonEmptyText)
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
      "promoCode" -> optional(promoCode)
    )(PaidMemberJoinForm.apply)(PaidMemberJoinForm.unapply)
  )

  val freeMemberChangeForm: Form[FreeMemberChangeForm] = Form(
    mapping(
      "payment" -> paymentMapping,
      "deliveryAddress" -> paidAddressMapping,
      "billingAddress" -> optional(paidAddressMapping),
      "featureChoice" -> productFeature,
      "promoCode" -> optional(promoCode)
  )(FreeMemberChangeForm.apply)(FreeMemberChangeForm.unapply)
  )

  val paidMemberChangeForm: Form[PaidMemberChangeForm] = Form(
    mapping(
      "password" -> nonEmptyText,
      "featureChoice" -> productFeature,
      "promoCode" -> optional(promoCode)
    )(PaidMemberChangeForm.apply)(PaidMemberChangeForm.unapply)
  )

  val feedbackForm: Form[FeedbackForm] = Form(
    mapping(
      "category" -> nonEmptyText,
      "page" -> text,
      "feedback" -> nonEmptyText,
      "name" -> nonEmptyText,
      "email" -> email
    )(FeedbackForm.apply)(FeedbackForm.unapply)
  )

  val supportForm: Form[SupportForm] = Form(
    mapping(
      "name" -> nonEmptyText,
      "currency" -> of[Currency],
      "amount" -> bigDecimal(10, 2),
      "email" -> email,
      "payment.token" -> nonEmptyText,
      "guardian-opt-in" -> boolean,
      "postcode" -> optional(nonEmptyText)
    )(SupportForm.apply)(SupportForm.unapply)
  )
}
