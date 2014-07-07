package controllers

import model.Tier
import model.Tier.Tier
import model.{Member, Tier}
import play.api.data.Form
import play.api.data.Forms._

import scala.concurrent.Future

import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import actions.{MemberRequest, AuthRequest, MemberAction, AuthenticatedAction}
import services.{MemberService, StripeService}

case class UserAddressData(street: String, postCode: String, city: String, country: String)

case class PaymentDetailsForm(paymentType: String, stripeToken: String, deliveryAddress: UserAddressData, billingAddress: UserAddressData)

trait TierController extends Controller {

  val upgradeTierForm: Form[PaymentDetailsForm] = Form(
    mapping(
      "paymentType" -> nonEmptyText,
      "stripeToken" -> nonEmptyText,
      "deliveryAddress" -> mapping(
        "street" -> nonEmptyText,
        "city" -> nonEmptyText,
        "postCode" -> nonEmptyText,
        "country" -> nonEmptyText
      )(UserAddressData.apply)(UserAddressData.unapply),
      "billingAddress" -> mapping(
        "street" -> nonEmptyText,
        "city" -> nonEmptyText,
        "postCode" -> nonEmptyText,
        "country" -> nonEmptyText
      )(UserAddressData.apply)(UserAddressData.unapply)
    )(PaymentDetailsForm.apply)(PaymentDetailsForm.unapply)
  )

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

  // Upgrade flow =====================================

  def upgradeTo(tierName: String) = MemberAction { implicit request =>
    val changingToTier = Tier.withName(tierName.capitalize)
    request.member.tier match {
      case Tier.Friend => Ok(views.html.tier.upgrade.payment(changingToTier))
      case Tier.Partner => Ok(views.html.tier.upgrade.payment(changingToTier))
      case _ => NotFound
    }

  }

  def confirmUpgradeTo(tierName: String) = MemberAction.async { implicit request => // POST
    val changingToTier = Tier.withName(tierName.capitalize)
    if (changingToTier > request.member.tier) {
      val formValues = upgradeTierForm.bindFromRequest
      formValues.fold(_ => Future.successful(BadRequest), makePayment(changingToTier))
    } else {
      Future.successful(NotFound)
    }
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

  def makePayment(tier: Tier)(formData: PaymentDetailsForm)(implicit request: MemberRequest[_]) = {

    val futureCustomer =
      if (request.member.customerId == Member.NO_CUSTOMER_ID)
        StripeService.Customer.create(request.user.getPrimaryEmailAddress, formData.stripeToken)
      else
        StripeService.Customer.read(request.member.customerId)

    val planName = tier.toString + (if (formData.paymentType == "annual") "Annual" else "")

    for {
      customer <- futureCustomer
      subscription <- customer.subscription.map { subscription =>
        StripeService.Subscription.update(customer.id, subscription.id, planName, formData.stripeToken)
      }.getOrElse {
        StripeService.Subscription.create(customer.id, planName)
      }
    } yield {
      MemberService.put(request.member.copy(tier = tier, customerId = customer.id))
      Ok("")
    }

  }
}

object TierController extends TierController
