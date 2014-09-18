package forms

import model.Subscription.{PaidTierPlan, FriendTierPlan, TierPlan}
import play.api.data.{Mapping, Form}
import play.api.data.Forms._

import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier.Tier
import model.Countries

object MemberForm {
  case class AddressForm(lineOne: String, lineTwo: String, town: String, countyOrState: String,
                         postCode: String, country: String) {
    // Salesforce only has one address line field, so merge our two together
    val line = Seq(lineOne, lineTwo).filter(_.nonEmpty).mkString(", ")
  }

  case class NameForm(first: String, last: String)

  case class PaymentForm(annual: Boolean, token: String)

  case class MarketingChoicesForm(gnm: Option[Boolean], thirdParty: Option[Boolean])

  trait JoinForm {
    val name: NameForm
    val deliveryAddress: AddressForm
    val marketingChoices: MarketingChoicesForm
    val password: Option[String]
    val tierPlan: TierPlan
  }

  case class FriendJoinForm(name: NameForm, deliveryAddress: AddressForm, marketingChoices: MarketingChoicesForm,
                            password: Option[String] ) extends JoinForm {
    val tierPlan = FriendTierPlan
  }

  case class PaidMemberJoinForm(tier: Tier, name: NameForm, payment: PaymentForm, deliveryAddress: AddressForm,
                                billingAddress: Option[AddressForm], marketingChoices: MarketingChoicesForm,
                                password: Option[String]) extends JoinForm {
    val tierPlan = PaidTierPlan(tier, payment.annual)
  }

  case class PaidMemberChangeForm(payment: PaymentForm, deliveryAddress: AddressForm,
                                  billingAddress: Option[AddressForm])

  val countriesRequiringState = Seq(Countries.Canada, Countries.US).map(c => c.name -> c).toMap

  def verifyAddress(address: AddressForm): Boolean =
    countriesRequiringState.get(address.country).fold(true)(_.states.contains(address.countyOrState))

  case class FeedbackForm(category: String, page: String, feedback: String, name: String, email: String)

  val friendAddressMapping: Mapping[AddressForm] = mapping(
    "lineOne" -> text,
    "lineTwo" -> text,
    "town" -> text,
    "countyOrState" -> text,
    "postCode" -> nonEmptyText,
    "country" -> text.verifying(Countries.all.contains _)
  )(AddressForm.apply)(AddressForm.unapply).verifying(verifyAddress _)

  val paidAddressMapping: Mapping[AddressForm] = mapping(
    "lineOne" -> nonEmptyText,
    "lineTwo" -> text,
    "town" -> nonEmptyText,
    "countyOrState" -> text,
    "postCode" -> nonEmptyText,
    "country" -> text.verifying(Countries.all.contains _)
  )(AddressForm.apply)(AddressForm.unapply).verifying(verifyAddress _)

  val nameMapping: Mapping[NameForm] = mapping(
    "first" -> nonEmptyText,
    "last" -> nonEmptyText
  )(NameForm.apply)(NameForm.unapply)

  val marketingChoicesMapping: Mapping[MarketingChoicesForm] = mapping(
    "gnnMarketing" -> optional(boolean),
    "thirdParty" -> optional(boolean)
  )(MarketingChoicesForm.apply)(MarketingChoicesForm.unapply)

  val paymentMapping: Mapping[PaymentForm] = mapping(
    "type" -> nonEmptyText.transform[Boolean](_ == "annual", x => if (x) "annual" else "month"),
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
      "deliveryAddress" -> friendAddressMapping,
      "marketingChoices" -> marketingChoicesMapping,
      "password" -> optional(nonEmptyText)
    )(FriendJoinForm.apply)(FriendJoinForm.unapply)
  )

  val paidMemberJoinForm: Form[PaidMemberJoinForm] = Form(
    mapping(
      "tier" -> nonEmptyText.transform[Tier](Tier.withName, _.toString),
      "name" -> nameMapping,
      "payment" -> paymentMapping,
      "deliveryAddress" -> paidAddressMapping,
      "billingAddress" -> optional(paidAddressMapping),
      "marketingChoices" -> marketingChoicesMapping,
      "password" -> optional(nonEmptyText)
    )(PaidMemberJoinForm.apply)(PaidMemberJoinForm.unapply)
  )

  val paidMemberChangeForm: Form[PaidMemberChangeForm] = Form(
    mapping(
      "payment" -> paymentMapping,
      "deliveryAddress" -> paidAddressMapping,
      "billingAddress" -> optional(paidAddressMapping)
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
}
