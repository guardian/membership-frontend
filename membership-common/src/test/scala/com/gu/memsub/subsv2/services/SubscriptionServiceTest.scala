package com.gu.memsub.subsv2.services


import com.github.nscala_time.time.Implicits._
import com.github.nscala_time.time.Imports.LocalTime
import com.gu.i18n.Currency.GBP
import com.gu.lib.DateDSL._
import com.gu.memsub
import com.gu.memsub.Benefit._
import com.gu.memsub.Subscription.{Id => _, _}
import com.gu.memsub.subsv2.Fixtures._
import com.gu.memsub.subsv2.SubscriptionPlan.AnyPlan
import com.gu.memsub.subsv2._
import com.gu.memsub.subsv2.reads.ChargeListReads._
import com.gu.memsub.subsv2.reads.SubPlanReads._
import com.gu.memsub.{Subscription => _, _}
import com.gu.salesforce.ContactId
import com.gu.zuora.ZuoraRestConfig
import com.gu.zuora.rest.SimpleClient
import io.lemonlabs.uri.dsl._
import okhttp3._
import org.joda.time.LocalDate
import org.specs2.mutable.Specification
import utils.Resource
import scalaz.Id._
import scalaz.{-\/, NonEmptyList, \/, \/-}

/**
  * This test just tests plumbing really
  * but at least it is /possible/ to test plumbing
  */
class SubscriptionServiceTest extends Specification {

  // this is the UAT prpId of friend, which we need for the catalog
  val prpId = ProductRatePlanId("2c92c0f94cc6ea05014cdb4b1d1f037d")
  val partnerPrpId = ProductRatePlanId("2c92c0f84c510081014c569327003593")
  val supporterPrpId = ProductRatePlanId("2c92c0f84bbfeca5014bc0c5a793241d")
  val digipackPrpId = ProductRatePlanId("2c92c0f94f2acf73014f2c908f671591")
  val gw6for6PrpId = ProductRatePlanId("2c92c0f965f212210165f69b94c92d66")
  val gw = ProductRatePlanId("2c92c0f965dc30640165f150c0956859")
  val now = 27 Sep 2016

  val cat = Map[ProductRatePlanId, CatalogZuoraPlan](
    prpId -> CatalogZuoraPlan(prpId, "foo", "", productIds.friend, None, List.empty, Map(ProductRatePlanChargeId("2c92c0f84cc6d9e5014cdb4c48b02d83") -> Friend), Status.current, None, Some("Membership")),
    partnerPrpId -> CatalogZuoraPlan(partnerPrpId,  "Partner", "", productIds.partner, None, List.empty, Map(ProductRatePlanChargeId("2c92c0f84c510081014c569327593595") -> Partner), Status.current, None, Some("type")),
    supporterPrpId -> CatalogZuoraPlan(supporterPrpId, "Supporter", "", productIds.supporter, None, List.empty, Map(ProductRatePlanChargeId("2c92c0f84c5100b6014c569b83b33ebd") -> Supporter), Status.current, None, Some("type")),
    digipackPrpId -> CatalogZuoraPlan(digipackPrpId, "Digipack", "", productIds.digipack, None, List.empty, Map(ProductRatePlanChargeId("2c92c0f94f2acf73014f2c91940a166d") -> Digipack), Status.current, None, Some("type")),
    gw6for6PrpId -> CatalogZuoraPlan(gw6for6PrpId, "GW Oct 18 - Six for Six - Domestic", "", productIds.weeklyDomestic, None, List.empty, Map(ProductRatePlanChargeId("2c92c0f865f204440165f69f407d66f1") -> Weekly), Status.current, None, Some("type")),
    gw -> CatalogZuoraPlan(gw, "GW Oct 18 - Quarterly - Domestic", "", productIds.weeklyDomestic, None, List.empty, Map(ProductRatePlanChargeId("2c92c0f865d273010165f16ada0a4346") -> Weekly), Status.current, None, Some("type"))
  )

  def jsonResponse(path: String)(req: Request) =
    new Response.Builder()
      .request(req)
      .body(ResponseBody.create(
        MediaType.parse("application/json"),
        Resource.getJson(path).toString)
      )
      .message("test")
      .code(200)
      .protocol(Protocol.HTTP_2)
      .build()

  val soapClient: SubscriptionService.SoapClient[Id] = _ => List(
    memsub.Subscription.AccountId("foo"),
    memsub.Subscription.AccountId("bar")
  )

  val subscriptions: Request => Id[Response] = a => a.url().uri().getPath match {
    case "/subscriptions/accounts/foo" => jsonResponse("rest/plans/accounts/Friend.json")(a)
    case "/subscriptions/accounts/bar" => jsonResponse("rest/plans/accounts/Friend.json")(a)
    case "/subscriptions/1234" => jsonResponse("rest/plans/Friend.json")(a)
    case "/subscriptions/A-S00063478" => jsonResponse("rest/plans/Upgraded.json")(a)
    case "/subscriptions/A-lead-time" => jsonResponse("rest/cancellation/GW-6for6-lead-time.json")(a)
    case "/subscriptions/A-segment-6for6" => jsonResponse("rest/cancellation/GW-6for6-segment-6for6.json")(a)
    case "/subscriptions/GW-before-bill-run" => jsonResponse("rest/cancellation/GW-before-bill-run.json")(a)
    case "/subscriptions/GW-stale-chargeThroughDate" => jsonResponse("rest/cancellation/GW-stale-chargeThroughDate.json")(a)
    case _ => new Response.Builder().message("test").code(404).protocol(Protocol.HTTP_1_0).request(a).build()
  }

  val rc = new SimpleClient[Id](ZuoraRestConfig("TESTS", "https://localhost", "foo", "bar"), subscriptions)
  val service = new SubscriptionService[Id](Fixtures.productIds, cat, rc, soapClient)

  "Current Plan" should {

    def contributorPlan(startDate: LocalDate, endDate: LocalDate): SubscriptionPlan.Contributor = PaidSubscriptionPlan[Product.Contribution, PaidCharge[Benefit.Contributor.type, BillingPeriod]](
      RatePlanId("idContributor"),
      ProductRatePlanId("prpi"),
      "Contributor",
      "desc",
      "Contributor",
      "Contribution",
      Product.Contribution,
      List.empty,
      PaidCharge(Contributor, BillingPeriod.Month, PricingSummary(Map(GBP -> Price(5.0f, GBP))), ProductRatePlanChargeId("foo"), SubscriptionRatePlanChargeId("noo")),
      None,
      startDate,
      endDate
    )
    def friendPlan(startDate: LocalDate, endDate: LocalDate): SubscriptionPlan.Friend = FreeSubscriptionPlan[Product.Membership, FreeCharge[Benefit.Friend.type]](
      RatePlanId("idFriend"), ProductRatePlanId("prpi"), "Friend", "desc", "Friend", "Membership", Product.Membership, FreeCharge(Friend, Set(GBP)), startDate, endDate
    )
    def legacyFriendPlan(startDate: LocalDate, endDate: LocalDate): SubscriptionPlan.Friend = FreeSubscriptionPlan[Product.Membership, FreeCharge[Benefit.Friend.type]](
      RatePlanId("idLegacyFriend"), ProductRatePlanId("prpi"), "LegacyFriend", "desc","LegacyFriend",  "Membership", Product.Membership, FreeCharge(Friend, Set(GBP)), startDate, endDate
    )
    def partnerPlan(startDate: LocalDate, endDate: LocalDate): SubscriptionPlan.Partner = PaidSubscriptionPlan[Product.Membership, PaidCharge[Benefit.Partner.type, BillingPeriod]](
      RatePlanId("idPartner"),
      ProductRatePlanId("prpi"),
      "Partner",
      "desc",
      "Partner",
      "Membership",
      Product.Membership,
      List.empty,
      PaidCharge(Partner, BillingPeriod.Year, PricingSummary(Map(GBP -> Price(149.0f, GBP))), ProductRatePlanChargeId("foo"), SubscriptionRatePlanChargeId("noo")),
      None,
      startDate,
      endDate
    )
    def supporterPlan(startDate: LocalDate, endDate: LocalDate): SubscriptionPlan.Supporter = PaidSubscriptionPlan[Product.Membership, PaidCharge[Benefit.Supporter.type, BillingPeriod]](
      RatePlanId("idSupporter"),
      ProductRatePlanId("prpi"),
      "Supporter",
      "desc",
      "Supporter",
      "Membership",
      Product.Membership,
      List.empty,
      PaidCharge(Supporter, BillingPeriod.Year, PricingSummary(Map(GBP -> Price(49.0f, GBP))), ProductRatePlanChargeId("bar"), SubscriptionRatePlanChargeId("nar")),
      None,
      startDate,
      endDate
    )
    def digipackPlan(startDate: LocalDate, endDate: LocalDate): SubscriptionPlan.Digipack = PaidSubscriptionPlan[Product.ZDigipack, PaidCharge[Benefit.Digipack.type, BillingPeriod]](
      RatePlanId("idDigipack"),
      ProductRatePlanId("prpi"),
      "Digipack",
      "desc",
      "Digital Pack",
      "Digital Pack",
      Product.Digipack,
      List.empty,
      PaidCharge(Digipack, BillingPeriod.Year, PricingSummary(Map(GBP -> Price(119.90f, GBP))), ProductRatePlanChargeId("baz"), SubscriptionRatePlanChargeId("naz")),
      None,
      startDate,
      endDate
    )

    def toSubscription[P <: SubscriptionPlan.AnyPlan](isCancelled: Boolean)(plans: NonEmptyList[P]): Subscription[P] = {
      import com.gu.memsub.Subscription._
      Subscription(
        id = Id(plans.head.id.get),
        name = Name("AS-123123"),
        accountId = AccountId("accountId"),
        startDate = plans.head.start,
        acceptanceDate = plans.head.start,
        termStartDate = plans.head.start,
        termEndDate = plans.head.start + 1.year,
        casActivationDate = None,
        promoCode = None,
        isCancelled = isCancelled,
        hasPendingFreePlan = false,
        plans = CovariantNonEmptyList(plans.head, plans.tail.toList),
        readerType = ReaderType.Direct,
        gifteeIdentityId = None,
        autoRenew = true
      )
    }

    val referenceDate = 26 Oct 2016

    "tell you that you aren't a contributor immediately after your sub has been cancelled" in {
      val plans = NonEmptyList(contributorPlan(referenceDate, referenceDate + 1.year))
      val subs = toSubscription(isCancelled = true)(plans)
      val result = GetCurrentPlans(subs, referenceDate).leftMap(_.contains("cancelled"))
      result mustEqual -\/(true)
    }

    "tell you a downgraded plan to friend, friend is current if we're after the plan end date" in {
      val plans = NonEmptyList(friendPlan(referenceDate, referenceDate + 1.year), partnerPlan(referenceDate - 1.year, referenceDate))
      val subs = toSubscription(isCancelled = false)(plans)
      val result = GetCurrentPlans(subs, referenceDate + 1.day).map(_.head.id.get)
      result mustEqual \/-("idFriend")
    }

    "tell you a downgraded plan to friend is current if we're before the term end date" in {
      val plans = NonEmptyList(friendPlan(referenceDate, referenceDate + 1.year), partnerPlan(referenceDate - 1.year, referenceDate))
      val subs = toSubscription(isCancelled = false)(plans)
      val result = GetCurrentPlans(subs, referenceDate).map(_.head.id.get)
      result mustEqual \/-("idPartner")
    }

    "tell you a upgraded plan is current on the change date" in {
      val plans = NonEmptyList(supporterPlan(referenceDate.minusDays(4), referenceDate), partnerPlan(referenceDate, referenceDate + 1.year))
      val subs = toSubscription(isCancelled = false)(plans)
      val result = GetCurrentPlans(subs, referenceDate).map(_.head.id.get)
      result mustEqual \/-("idPartner")
    }

    "tell you are a supporter if you were a partner then friend then supporter" in {
      val plans = NonEmptyList(partnerPlan(referenceDate - 1.year, referenceDate), friendPlan(referenceDate, referenceDate + 1.year), supporterPlan(referenceDate, referenceDate + 1.year))
      val subs = toSubscription(isCancelled = false)(plans)
      val result = GetCurrentPlans(subs, referenceDate + 1.day).map(_.head.id.get)
      result mustEqual \/-("idSupporter")
    }

    "tell you you aren't a friend if your subscription is cancelled regardless of the date" in {
      val plans = NonEmptyList(friendPlan(referenceDate, referenceDate + 1.year))
      val subs = toSubscription(isCancelled = true)(plans)
      val result = GetCurrentPlans(subs, referenceDate).leftMap(_.contains("cancelled"))
      result mustEqual -\/(true)// not helpful
    }

    "tell you you are still a supporter if your subscription is cancelled but it's still before the date" in {
      val plans = NonEmptyList(supporterPlan(referenceDate, referenceDate + 1.year))
      val subs = toSubscription(isCancelled = true)(plans)
      val result = GetCurrentPlans(subs, referenceDate).map(_.head.id.get)
      result mustEqual \/-("idSupporter")
    }

    "tell you you are no longer a supporter if your subscription is after an end date" in {
      val plans = NonEmptyList(supporterPlan(referenceDate - 1.year, referenceDate))
      val subs = toSubscription(isCancelled = false)(plans)
      val result = GetCurrentPlans(subs, referenceDate + 1.day).leftMap(_.contains("ended"))
      result mustEqual -\/(true)// not helpful
    }

    "if you've cancelled and then signed up with a different tier, should return the new tier on day 1" in {
      val plans = NonEmptyList(partnerPlan(referenceDate, referenceDate - 1.year), supporterPlan(referenceDate + 1.day, referenceDate + 1.year))
      val subs = toSubscription(isCancelled = false)(plans)
      val result = GetCurrentPlans(subs, referenceDate + 1.day).map(_.head.id.get)
      result mustEqual \/-("idSupporter")
    }

    "if you have both legacy and current Friend rate plans on your sub, should return the sub with the new Friends rate plan" in {
      val plans = NonEmptyList(friendPlan(referenceDate, referenceDate + 1.year), legacyFriendPlan(referenceDate - 3.months, referenceDate + 9.months))
      val subs = toSubscription(isCancelled = false)(plans)
      val result = GetCurrentPlans(subs, referenceDate).map(_.head.id.get)
      result mustEqual \/-("idFriend")
    }

    "if you're in a free trial of digipack, tell you you're already a digipack subscriber" in {

      val firstPayment = referenceDate + 14.days
      val digipackSub = toSubscription(isCancelled = false)(NonEmptyList(digipackPlan(firstPayment, referenceDate + 1.year)))
                          .copy(termStartDate = referenceDate, startDate = referenceDate)

      GetCurrentPlans(digipackSub, referenceDate).map(_.head.id.get) mustEqual \/-("idDigipack")
    }

  }

  "Subscription service" should {
    val contact = new ContactId {
      def salesforceContactId = "foo"

      def salesforceAccountId = "bar"
    }
    "Allow you to fetch one of two subscriptions across product families" in {
      val subs = service.either[SubscriptionPlan.Member, SubscriptionPlan.ContentSubscription](contact)
      subs.toOption.flatMap(_.flatMap(_.swap.toOption).map(_.name)) mustEqual Some(memsub.Subscription.Name("subscriptionNumber")) // what is in the config file
    }

    "Allow you to fetch one of two subscriptions" in {
      val subs = service.either[SubscriptionPlan.Friend, SubscriptionPlan.Digipack](contact)
      subs.toOption.flatMap(_.flatMap(_.swap.toOption).map(_.name)) mustEqual Some(memsub.Subscription.Name("subscriptionNumber")) // what is in the config file
    }

  }
}
