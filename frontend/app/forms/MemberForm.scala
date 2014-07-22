package forms

import play.api.data.{Mapping, Form}
import play.api.data.Forms._
import model.Tier.Tier
import model.Tier
import model.Tier.Tier

object MemberForm {
  case class AddressForm(lineOne: String, lineTwo: String, town: String, countyOrState: String,
                         postCode: String, country: String)

  case class NameForm(first: String, last: String)

  case class PaymentForm(`type`: String, token: String)

  case class FriendJoinForm(name: NameForm, deliveryAddress: AddressForm)

  case class PaidMemberJoinForm(tier: Tier, name: NameForm, payment: PaymentForm, deliveryAddress: AddressForm,
                                billingAddress: Option[AddressForm])

  case class PaidMemberChangeForm(payment: PaymentForm, deliveryAddress: AddressForm,
                                  billingAddress: Option[AddressForm])

  val friendAddressMapping: Mapping[AddressForm] = mapping(
    "lineOne" -> text,
    "lineTwo" -> text,
    "town" -> text,
    "countyOrState" -> text,
    "postCode" -> nonEmptyText,
    "country" -> nonEmptyText
  )(AddressForm.apply)(AddressForm.unapply)

  val paidAddressMapping: Mapping[AddressForm] = mapping(
    "lineOne" -> nonEmptyText,
    "lineTwo" -> text,
    "town" -> nonEmptyText,
    "countyOrState" -> text,
    "postCode" -> nonEmptyText,
    "country" -> nonEmptyText
  )(AddressForm.apply)(AddressForm.unapply)

  val nameMapping: Mapping[NameForm] = mapping(
    "first" -> nonEmptyText,
    "last" -> nonEmptyText
  )(NameForm.apply)(NameForm.unapply)

  val paymentMapping: Mapping[PaymentForm] = mapping(
    "type" -> nonEmptyText,
    "token" -> nonEmptyText
  )(PaymentForm.apply)(PaymentForm.unapply)

  val friendJoinForm: Form[FriendJoinForm] = Form(
    mapping(
      "name" -> nameMapping,
      "deliveryAddress" -> friendAddressMapping
    )(FriendJoinForm.apply)(FriendJoinForm.unapply)
  )

  val paidMemberJoinForm: Form[PaidMemberJoinForm] = Form(
    mapping(
      "tier" -> nonEmptyText.transform[Tier](Tier.withName, _.toString),
      "name" -> nameMapping,
      "payment" -> paymentMapping,
      "deliveryAddress" -> paidAddressMapping,
      "billingAddress" -> optional(paidAddressMapping)
    )(PaidMemberJoinForm.apply)(PaidMemberJoinForm.unapply)
  )

  val paidMemberChangeForm: Form[PaidMemberChangeForm] = Form(
    mapping(
      "payment" -> paymentMapping,
      "deliveryAddress" -> paidAddressMapping,
      "billingAddress" -> optional(paidAddressMapping)
    )(PaidMemberChangeForm.apply)(PaidMemberChangeForm.unapply)
  )
}
