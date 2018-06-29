package controllers

import _root_.services._
import actions.Fallbacks._
import actions.{ActionRefiners, CommonActions, OAuthActions, Subscriber, SubscriptionRequest, TouchpointActionRefiners, TouchpointCommonActions}
import com.gu.googleauth.GoogleAuthConfig
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import com.gu.monitoring.SafeLogger
import model.EmbedSerializer._
import model.Eventbrite.{EBCode, EBEvent}
import model.RichEvent.{RichEvent, _}
import model._
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.EventbriteService._
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
        SafeLogger.info(s"Buy action invoked for event: ${event.id}")
        block(request)
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
      // One conditional now covers Live Events AND Masterclasses...

      // Logged out readers and non-members will go via the onUnauthenticated route
      // They, and any ineligible-tier Members will get a "SOLD OUT" EventBrite page if the event has membership-only tickets.
      // Previously ineligible-tier Members would get a suggested upgrade page.
      case Some(event@(_: RichEvent)) =>
        BuyAction(id, onUnauthenticated = redirectNonMemberToEventbrite(event)(_)).async { implicit request =>
          redirectMemberToEventbrite(event)
        }
      case _ =>
        // We seem to have a crawler(?) hitting the buy urls for past events
        SafeLogger.info(s"User hit the buy url for event $id - neither a GuLiveEvent or Masterclass could be retrieved, returning 404...")
        CachedAction(NotFound)
    }

  private def eventCookie(event: RichEvent) = s"mem-event-${event.id}"

  private def addEventBriteGACrossDomainParam(uri: Uri)(implicit req: RequestHeader): Uri = {
    // https://www.eventbrite.co.uk/support/articles/en_US/Troubleshooting/how-to-enable-cross-domain-and-ecommerce-tracking-with-google-universal-analytics
    req.cookies.get("_ga").map(_.value.replaceFirst("GA\\d+\\.\\d+\\.", "")).fold(uri)(value => uri & ("_eboga", value))
  }

  private def redirectMemberToEventbrite(event: RichEvent)(implicit req: SubscriptionRequest[AnyContent] with Subscriber): Future[Result] = {
    memberService.createEBCode(req.subscriber, event).map { codeOpt =>
      eventbriteRedirect(event, codeOpt)
    }
  }

  private def redirectNonMemberToEventbrite(event: RichEvent)(implicit req: RequestHeader): Result = {
    eventbriteRedirect(event, None)
  }

  def eventbriteRedirect(event: RichEvent, discountCodeOpt: Option[EBCode])(implicit req: RequestHeader) = {
    val eventUrl = discountCodeOpt.fold(Uri.parse(event.url))(c => event.url ? ("discount" -> c.code))
    Found(addEventBriteGACrossDomainParam(eventUrl)).withCookies(Cookie(eventCookie(event), "", Some(3600)))
  }

  @deprecated("Guardian Live Events are not configured to use this anymore, and host their own thankyou page on EventBrite.")
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
