package controllers

import model.Tier
import play.api.data.Form
import play.api.data.Forms._

import scala.concurrent.Future

import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import actions.{MemberRequest, AuthRequest, MemberAction, AuthenticatedAction}
import services.{MemberService, StripeService}

case class Address(street: String, postCode: String, city: String, country: String)

case class PaymentDetailsForm(paymentType: String, stripeToken: String, billingAddress: Address, deliveryAddress: Address)

trait TierController extends Controller {

  case class AddressData(street: String, city: String)

  case class UserAddressData(name: String, address: AddressData)

  val userFormNested: Form[UserAddressData] = Form(
    mapping(
      "name" -> text,
      "address" -> mapping(
        "street" -> text,
        "city" -> text
      )(AddressData.apply)(AddressData.unapply)
    )(UserAddressData.apply)(UserAddressData.unapply)
  )

  val changeForm: Form[PaymentDetailsForm] = Form{ tuple(
    "paymentType" -> nonEmptyText,
    "stripeToken" -> nonEmptyText,
    "billingCity" -> nonEmptyText,
    "billingPostCode" -> nonEmptyText,
    "billingStreet" -> nonEmptyText,
    "billingCountry" -> nonEmptyText,
    "deliveryCity" -> nonEmptyText,
    "deliveryPostCode" -> nonEmptyText,
    "deliveryStreet" -> nonEmptyText,
    "deliveryCountry" -> nonEmptyText,
    "paymentType" -> nonEmptyText
  ) }

  def change() = MemberAction { implicit request =>
    Ok(views.html.tier.change(request.member.tier))
  }

  // Friend downgrade flow ====================================

  def changeFriend() = MemberAction { implicit request =>
    Ok(views.html.tier.downgrade.confirm())
  }

  def confirmFriend() = MemberAction.async { implicit request => // POST
    for {
      customer <- StripeService.Customer.read(request.member.customerId)
      cancelledOpt = customer.subscription.map { subscription =>
        StripeService.Subscription.delete(customer.id, subscription.id)
      }
      cancelled <- Future.sequence(cancelledOpt.toSeq)
    } yield {
      cancelled.headOption.map(_ => Redirect("/tier/change/friend/summary")).getOrElse(NotFound)
    }
  }

  def summaryFriend() = MemberAction.async { implicit request =>
    StripeService.Customer.read(request.member.customerId).map { customer =>
      val response = for {
        subscription <- customer.subscription
        card <- customer.card
      } yield Ok(views.html.tier.downgrade.summary(subscription, card))

      response.getOrElse(NotFound)
    }
  }

  // Partner upgrade flow =====================================

  def changeTo(tierName: String) = MemberAction { implicit request =>
    val changingToTier = Tier.withName(tierName.capitalize)
    request.member.tier match {
      case Tier.Friend => Ok(views.html.tier.upgrade.payment(changingToTier))
      case _ => NotFound
    }

  }

  def confirmPartner = MemberAction.async { implicit request => // POST
    request.member.tier match {
      case Tier.Friend => {

        val formValues = changeForm.bindFromRequest

        formValues.fold(_ => Future.successful(BadRequest), makePayment)

      }
      case _ => Future.successful(NotFound)
    }
  }

  def summaryPartner = MemberAction { implicit request =>
    Ok
  }

//  // Patron upgrade flow ======================================
//
//  def changePatron = AuthenticatedAction { implicit request =>
//    Ok(views.html.tier.upgrade.payment(Tier.Patron))
//  }

  def confirmPatron = MemberAction { implicit request => // POST
    Ok
  }

  def summaryPatron = MemberAction { implicit request =>
    Ok
  }

  def confirmCancel() = AuthenticatedAction { implicit request =>
    Ok(views.html.tier.cancel.confirm())
  }

  def cancelSummary() = MemberAction.async { implicit request =>
    StripeService.Customer.read(request.member.customerId).map { customer =>
      val response = for {
        subscription <- customer.subscription
        card <- customer.card
      } yield Ok(views.html.tier.cancel.summary(subscription, card))

      response.getOrElse(NotFound)
    }
  }

  def cancelTier() = AuthenticatedAction { implicit request =>
    Redirect("/tier/cancel/summary")
  }

  def makePayment(formData: (String, String, String, String, String, String, String, String, String, String, String))(implicit request: MemberRequest[_]) = {
    val paymentType = formData._1
    val stripeToken = formData._2

    val futureCustomerId =
      if (request.member.customerId.isEmpty)
        StripeService.Customer.create("will.franklin@theguardian.com", stripeToken).map(_.id)
      else
        Future.successful(request.member.customerId)

    for {
      customerId <- futureCustomerId
      subscription <- StripeService.Subscription.create(customerId, "Partner")
    } yield {
      MemberService.put(request.member.copy(tier = Tier.Partner, customerId = customerId))
      Ok("")
    }

  }
}

object TierController extends TierController
