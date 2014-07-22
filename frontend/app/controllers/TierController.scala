package controllers

import model.Stripe.{PaymentDetails, Plan}
import model.Tier.Tier
import model.{Member, Tier}
import play.api.data.{Mapping, Form}
import play.api.data.Forms._

import scala.concurrent.Future

import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import actions._
import services.{MemberService, StripeService}
import actions.MemberRequest
import scala.Some

case class AddressForm(street: String, postCode: String, city: String, country: String)

case class UpgradeTierForm(paymentType: String, stripeToken: String, deliveryAddress: AddressForm, billingAddress: AddressForm)

trait DowngradeTier {
  self: TierController =>

  def downgradeToFriend() = PaidMemberAction { implicit request =>
    Ok(views.html.tier.downgrade.confirm())
  }

  def downgradeToFriendConfirm() = PaidMemberAction.async { implicit request => // POST
    for {
      cancelledSubscription <- MemberService.cancelAnySubscriptionPayment(request.member)
    } yield {
      cancelledSubscription.map(_ => Redirect("/tier/change/friend/summary")).getOrElse(NotFound)
    }
  }

  def downgradeToFriendSummary() = PaidMemberAction.async { implicit request =>
    StripeService.Customer.read(request.stripeCustomerId).map { customer =>
      val response = for {
        paymentDetails <- customer.paymentDetails
      } yield Ok(views.html.tier.downgrade.summary(paymentDetails))

      response.getOrElse(NotFound)
    }
  }
}

trait UpgradeTier {
  self: TierController =>

  val upgradeTierForm: Form[UpgradeTierForm] = Form(
    mapping(
      "paymentType" -> nonEmptyText,
      "stripeToken" -> nonEmptyText,
      "deliveryAddress" -> addressMapping(nonEmptyText),
      "billingAddress" -> addressMapping(text)
    )(UpgradeTierForm.apply)(UpgradeTierForm.unapply)
  )

  def addressMapping(textMapping: Mapping[String]): Mapping[AddressForm] = mapping(
    "street" -> textMapping,
    "city" -> textMapping,
    "postCode" -> textMapping,
    "country" -> textMapping
  )(AddressForm.apply)(AddressForm.unapply)

  def upgrade(tierStr: String) = MemberAction { implicit request =>
    val tier = Tier.routeMap(tierStr)

    if (request.member.tier < tier)
      Ok(views.html.tier.upgrade.upgradeForm(tier))
    else
      NotFound
  }

  def upgradeConfirm(tierStr: String) = MemberAction.async { implicit request =>
    val tier = Tier.routeMap(tierStr)

    if (request.member.tier < tier)
      upgradeTierForm.bindFromRequest.fold(_ => Future.successful(BadRequest), makePayment(tier))
    else
      Future.successful(NotFound)
  }

  def makePayment(tier: Tier)(formData: UpgradeTierForm)(implicit request: MemberRequest[_]) = {
    val futureCustomer =
      request.member.stripeCustomerId.fold {
        StripeService.Customer.create(request.user.getPrimaryEmailAddress, formData.stripeToken)
      } {
        StripeService.Customer.read // TODO: use stripeToken to update card
      }

    val planName = tier.toString + (if (formData.paymentType == "annual") Plan.ANNUAL_SUFFIX else "")

    for {
      customer <- futureCustomer
      subscription <- customer.paymentDetails.map { paymentDetails =>
        StripeService.Subscription.update(customer.id, paymentDetails.subscription.id, planName, formData.stripeToken)
      }.getOrElse {
        StripeService.Subscription.create(customer.id, planName)
      }
    } yield {
      MemberService.update(request.member.copy(tier = tier, stripeCustomerId = Some(customer.id)))
      Ok("")
    }
  }
}

trait CancelTier {
  self: TierController =>

  def cancelTier() = MemberAction { implicit request =>
    Ok(views.html.tier.cancel.confirm(request.member.tier))
  }

  def cancelTierConfirm() = MemberAction.async { implicit request =>
    for {
      cancelledSubscription <- MemberService.cancelAnySubscriptionPayment(request.member)
    } yield {
      MemberService.update(request.member.copy(optedIn=false))
      Redirect("/tier/cancel/summary")
    }
  }

  def cancelTierSummary() = MemberAction.async { implicit request =>
    val futurePaymentDetails: Future[Option[PaymentDetails]] = request.member.stripeCustomerId
      .map { stripeCustomerId =>
      StripeService.Customer.read(stripeCustomerId).map(_.paymentDetails)
    }.getOrElse(Future.successful(None))

    futurePaymentDetails.map(paymentDetails => Ok(views.html.tier.cancel.summary(paymentDetails)))
  }
}

trait TierController extends Controller with UpgradeTier with DowngradeTier with CancelTier {

  def change() = MemberAction { implicit request =>
    Ok(views.html.tier.change(request.member.tier))
  }
}

object TierController extends TierController
