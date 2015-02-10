package forms

import play.api.data.validation.Constraints
import play.api.data.{Mapping, Form}
import play.api.data.Forms._

import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier
import com.gu.membership.zuora.{Address, Country, Countries}

import model._

object MemberForm {
  case class NameForm(first: String, last: String)

  case class PaymentForm(annual: Boolean, token: String)

  case class MarketingChoicesForm(gnm: Option[Boolean], thirdParty: Option[Boolean])

  trait JoinForm {
    val name: NameForm
    val deliveryAddress: Address
    val marketingChoices: MarketingChoicesForm
    val password: Option[String]
    val plan: ProductRatePlan
  }

  case class FriendJoinForm(name: NameForm, deliveryAddress: Address, marketingChoices: MarketingChoicesForm,
                            password: Option[String] ) extends JoinForm {
    val plan = FriendTierPlan
  }

  case class StaffJoinForm(name: NameForm, deliveryAddress: Address, marketingChoices: MarketingChoicesForm,
                            password: Option[String] ) extends JoinForm {
    val plan = StaffPlan
  }

  case class PaidMemberJoinForm(tier: Tier, name: NameForm, payment: PaymentForm, deliveryAddress: Address,
                                billingAddress: Option[Address], marketingChoices: MarketingChoicesForm,
                                password: Option[String]) extends JoinForm {
    val plan = PaidTierPlan(tier, payment.annual)
  }

  trait MemberChangeForm {
    val deliveryAddress: Address
    val billingAddress: Option[Address]
  }

  case class PaidMemberChangeForm(deliveryAddress: Address, billingAddress: Option[Address]) extends MemberChangeForm
  case class FreeMemberChangeForm(payment: PaymentForm, deliveryAddress: Address, billingAddress: Option[Address]) extends MemberChangeForm

  case class FeedbackForm(category: String, page: String, feedback: String, name: String, email: String)

  val countryText = nonEmptyText.verifying(Countries.allCodes.contains _)
    .transform[Country](Countries.allCodes.apply, _.alpha2)

  val nonPaidAddressMapping: Mapping[Address] = mapping(
    "lineOne" -> text,
    "lineTwo" -> text,
    "town" -> text,
    "countyOrState" -> text,
    "postCode" -> text(maxLength=20),
    "country" -> countryText
  )(Address.apply)(Address.unapply).verifying(_.valid)

  val paidAddressMapping: Mapping[Address] = mapping(
    "lineOne" -> nonEmptyText,
    "lineTwo" -> text,
    "town" -> nonEmptyText,
    "countyOrState" -> text,
    "postCode" -> text(maxLength=20),
    "country" -> countryText
  )(Address.apply)(Address.unapply).verifying(_.valid)

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
      "tier" -> nonEmptyText.transform[Tier](Tier.slugMap, _.slug),
      "name" -> nameMapping,
      "payment" -> paymentMapping,
      "deliveryAddress" -> paidAddressMapping,
      "billingAddress" -> optional(paidAddressMapping),
      "marketingChoices" -> marketingChoicesMapping,
      "password" -> optional(nonEmptyText)
    )(PaidMemberJoinForm.apply)(PaidMemberJoinForm.unapply)
  )

  val freeMemberChangeForm: Form[FreeMemberChangeForm] = Form(
    mapping(
      "payment" -> paymentMapping,
      "deliveryAddress" -> paidAddressMapping,
      "billingAddress" -> optional(paidAddressMapping)
    )(FreeMemberChangeForm.apply)(FreeMemberChangeForm.unapply)
  )

  val paidMemberChangeForm: Form[PaidMemberChangeForm] = Form(
    mapping(
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
