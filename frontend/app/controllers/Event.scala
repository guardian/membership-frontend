package controllers

import _root_.services._
import actions.ActionRefiners._
import actions.Fallbacks._
import actions.{ActionRefiners, CommonActions, OAuthActions, Subscriber, SubscriptionRequest, TouchpointActionRefiners, TouchpointCommonActions}
import com.gu.googleauth.GoogleAuthConfig
import com.gu.memsub.Subscriber.Member
import com.gu.memsub.util.Timing
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import com.gu.monitoring.SafeLogger
import model.EmbedSerializer._
import model.Eventbrite.{EBCode, EBEvent, EBOrder}
import model.RichEvent.{RichEvent, _}
import model._
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.EventbriteService._
import tracking._
import utils.ReferralData
import views.support.MembershipCompat._
import views.support.PageInfo

import scala.concurrent.{ExecutionContext, Future}

class Event(
  val wsClient: WSClient,
  implicit val eventbriteService: EventbriteCollectiveServices,
  implicit val touchpointBackends: TouchpointBackends,
  touchpointActionRefiners: TouchpointActionRefiners,
  touchpointCommonActions: TouchpointCommonActions,
  implicit val parser: BodyParser[AnyContent],
  override implicit val executionContext: ExecutionContext,
  googleAuthConfig: GoogleAuthConfig,
  commonActions: CommonActions,
  actionRefiners: ActionRefiners,
  override protected val controllerComponents: ControllerComponents
) extends OAuthActions(parser, executionContext, googleAuthConfig, commonActions)
  with BaseController
  with MemberServiceProvider
  with ActivityTracking
  {

  import touchpointActionRefiners._
  import touchpointCommonActions._
  import actionRefiners.authenticated
  import commonActions.{CachedAction, CorsPublicCachedAction, Cors, NoCacheAction}

  private def recordBuyIntention(eventId: String) = new ActionBuilder[Request, AnyContent] {

    override def parser = Event.this.parser

    override protected implicit def executionContext: ExecutionContext = Event.this.executionContext

    override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      eventbriteService.getEvent(eventId).map { event =>
        trackAnon(EventActivity("buyActionInvoked", None, EventData(event)))(request)
        Timing.record(event.service.wsMetrics, "buy-action-invoked") {
          block(request)
        }
      }.getOrElse(Future.successful(NotFound))
    }

  }

  private def BuyAction(id: String, onUnauthenticated: RequestHeader => Result = notYetAMemberOn(_)) = NoCacheAction andThen recordBuyIntention(id) andThen
    authenticated(onUnauthenticated = onUnauthenticated) andThen subscriptionRefiner(onNonMember = onUnauthenticated)

  def details(slug: String) = CachedAction { implicit request =>
    val eventOpt = for {
      id <- EBEvent.slugToId(slug)
      correctEvent <-(Eventbrite.HiddenEvents.get(id).toSeq :+ id).flatMap(eventbriteService.getEvent).headOption
    } yield {
      if (slug == correctEvent.slug) {
        trackAnon(EventActivity("viewEventDetails", None, EventData(correctEvent)))
        eventDetail(correctEvent)
      } else Redirect(routes.Event.details(correctEvent.slug))
    }

    eventOpt.getOrElse(Redirect(routes.WhatsOn.list))
  }

  /*
   * This endpoint is hit by Composer when embedding Membership events.
   * Changes here will need to be reflected in the flexible-content repo.
   * Note that Composer will index this data, which is in turn indexed by CAPI.
   * (eg. updates to event details will not be reflected post-embed)
   */
  def embedData(slug: String) = Cors.andThen(CachedAction) { implicit request =>
    val standardFormat = ISODateTimeFormat.dateTime.withZoneUTC

    val eventDataOpt = for {
      id <- EBEvent.slugToId(slug)
      event <- eventbriteService.getEvent(id)
    } yield EmbedData(
      title = event.name.text,
      image = event.socialImgUrl,
      venue = event.venue.name,
      location = event.venue.addressLine,
      price = event.internalTicketing.map(_.primaryTicket.priceText),
      identifier = event.metadata.identifier,
      start = event.start.toString(standardFormat),
      end = event.end.toString(standardFormat)
    )

    Ok(eventToJson(eventDataOpt))
  }

  /**
   * This endpoint is hit by .com to enhance an embedded event.
   */
  def embedCard(slug: String) = CorsPublicCachedAction { implicit request =>
    val eventOpt = for {
      id <- EBEvent.slugToId(slug)
      event <- eventbriteService.getEvent(id)
    } yield event

    Ok(eventOpt.fold {
      Json.obj("status" -> "error")
    } { event =>
      Json.obj("status" -> "success", "html" -> views.html.embeds.eventCard(event).toString())
    })
  }

  private def eventDetail(event: RichEvent)(implicit request: RequestHeader) = {
    val pageInfo = PageInfo(
      title = event.name.text,
      url = request.path,
      description = event.description.map(_.blurb),
      image = event.socialImgUrl,
      schemaOpt = Some(event.schema)
    )
    Ok(views.html.event.eventDetail(pageInfo, event))
  }

  def buy(id: String): Action[AnyContent] = eventbriteService.getEvent(id) match {
      case Some(event@(_: GuLiveEvent)) =>
        BuyAction(id).async { implicit request =>
          if (event.isBookableByTier(request.subscriber.subscription.plan.tier))
            redirectSignedInMemberToEventbrite(event)
          else suggestUserUpgrades
        }
      case Some(event@(_: MasterclassEvent)) =>
        BuyAction(id, onUnauthenticated = redirectAnonUserToEventbrite(event)(_)).async { implicit request =>
          redirectSignedInMemberToEventbrite(event)
        }
      case _ =>
        // We seem to have a crawler(?) hitting the buy urls for past events
        SafeLogger.info(s"User hit the buy url for event $id - neither a GuLiveEvent or Masterclass could be retrieved, returning 404...")
        CachedAction(NotFound)
    }

  private def suggestUserUpgrades(implicit request: SubReqWithSub[AnyContent]) =
    Future.successful(Redirect(routes.TierController.change()).addingToSession("preJoinReturnUrl" -> request.uri))

  private def eventCookie(event: RichEvent) = s"mem-event-${event.id}"

  private def addEventBriteGACrossDomainParam(uri: Uri)(implicit req: RequestHeader): Uri = {
    // https://www.eventbrite.co.uk/support/articles/en_US/Troubleshooting/how-to-enable-cross-domain-and-ecommerce-tracking-with-google-universal-analytics
    req.cookies.get("_ga").map(_.value.replaceFirst("GA\\d+\\.\\d+\\.", "")).fold(uri)(value => uri & ("_eboga", value))
  }

  private def redirectSignedInMemberToEventbrite(event: RichEvent)(implicit req: SubscriptionRequest[AnyContent] with Subscriber): Future[Result] =
    Timing.record(event.service.wsMetrics, s"user-sent-to-eventbrite-${req.subscriber.subscription.plan.tier}") {
      memberService.createEBCode(req.subscriber, event).map { codeOpt =>
        val memberData = MemberData(
          salesforceContactId = req.subscriber.contact.salesforceContactId,
          identityId = req.user.id,
          tier = req.subscriber.subscription.plan.tier,
          campaignCode = ReferralData.fromRequest.campaignCode
        )

        track(EventActivity("redirectToEventbrite", Some(memberData), EventData(event)), req.user)

        eventbriteRedirect(event, codeOpt)
      }
    }

  private def redirectAnonUserToEventbrite(event: RichEvent)(implicit req: RequestHeader): Result = {
    trackAnon(EventActivity("redirectToEventbrite", None, EventData(event)))

    eventbriteRedirect(event, None)
  }

  def eventbriteRedirect(event: RichEvent, discountCodeOpt: Option[EBCode])(implicit req: RequestHeader) = {
    val eventUrl = discountCodeOpt.fold(Uri.parse(event.url))(c => event.url ? ("discount" -> c.code))
    Found(addEventBriteGACrossDomainParam(eventUrl)).withCookies(Cookie(eventCookie(event), "", Some(3600)))
  }

  private def trackConversionToThankyou(event: RichEvent, order: Option[EBOrder],
                                        member: Option[Member])(implicit request: Request[_]) {

    val memberData = member.map(m => MemberData(
      salesforceContactId = m.contact.salesforceContactId,
      identityId = m.contact.identityId,
      tier = m.subscription.plan.tier,
      campaignCode = ReferralData.fromRequest.campaignCode))

    trackAnon(EventActivity("eventThankYou", memberData, EventData(event), order.map(OrderData)))(request)
  }

  def thankyou(id: String, orderIdOpt: Option[String]) = SubscriptionAction.async { implicit request =>
    orderIdOpt.fold {
      val resultOpt = for {
        oid <- request.flash.get("oid")
        event <- eventbriteService.getEvent(id)
      } yield {
        event.service.getOrder(oid).map { order =>
          val count = event.countComplimentaryTicketsInOrder(order)
          if (count > 0) {
            memberService.recordFreeEventUsage(request.subscriber.subscription, event, order, count)
          }

          trackConversionToThankyou(event, Some(order), Some(request.subscriber))

          Ok(views.html.event.thankyou(event, order))
            .discardingCookies(DiscardingCookie(eventCookie(event)))
        }
      }
      resultOpt.getOrElse(Future.successful(NotFound))
    } { orderId =>
      Future.successful(Redirect(routes.Event.thankyou(id, None)).flashing("oid" -> orderId))
    }
  }

  def thankyouPixel(id: String) = NoCacheAction { implicit request =>
    eventbriteService.getEvent(id).map { event =>
      // only log a conversion if the user came from a membership event page
      request.cookies.get(eventCookie(event)).foreach { _ =>
        trackConversionToThankyou(event, None, None)
      }
      NoContent.discardingCookies(DiscardingCookie(eventCookie(event)))
    }.getOrElse(NotFound)
  }

  def preview(id: String) = GoogleAuthenticatedStaffAction.async { implicit request =>
    eventbriteService.getPreviewEvent(id).map(eventDetail)
  }

  def previewMasterclass(id: String) = GoogleAuthenticatedStaffAction.async { implicit request =>
    eventbriteService.getPreviewMasterclass(id).map(eventDetail)
  }
}
