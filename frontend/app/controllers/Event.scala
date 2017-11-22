package controllers

import javax.inject.Inject

import _root_.services.{EventbriteService, GuardianLiveEventService, MasterclassEventService}
import actions.ActionRefiners._
import actions.Fallbacks._
import actions.{OAuthActions, Subscriber, SubscriptionRequest}
import com.gu.memsub.Subscriber.Member
import com.gu.memsub.util.Timing
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import model.EmbedSerializer._
import model.Eventbrite.{EBCode, EBEvent, EBOrder}
import model.RichEvent.{RichEvent, _}
import model._
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.EventbriteService._
import tracking._
import utils.ReferralData
import views.support.MembershipCompat._
import views.support.PageInfo

import scala.concurrent.Future

class Event @Inject()(override val wsClient: WSClient) extends Controller with MemberServiceProvider with OAuthActions with LazyLogging {

  private def recordBuyIntention(eventId: String) = new ActionBuilder[Request] {
    override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      EventbriteService.getEvent(eventId).map { event =>
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
      correctEvent <-(Eventbrite.HiddenEvents.get(id).toSeq :+ id).flatMap(EventbriteService.getEvent).headOption
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
      event <- EventbriteService.getEvent(id)
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
      event <- EventbriteService.getEvent(id)
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

  def buy(id: String): Action[AnyContent] = EventbriteService.getEvent(id) match {
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
        logger.info(s"User hit the buy url for event $id - neither a GuLiveEvent or Masterclass could be retrieved, returning 404...")
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
        eventbriteRedirect(event, codeOpt)
      }
    }

  private def redirectAnonUserToEventbrite(event: RichEvent)(implicit req: RequestHeader): Result = {
    eventbriteRedirect(event, None)
  }

  def eventbriteRedirect(event: RichEvent, discountCodeOpt: Option[EBCode])(implicit req: RequestHeader) = {
    val eventUrl = discountCodeOpt.fold(Uri.parse(event.url))(c => event.url ? ("discount" -> c.code))
    Found(addEventBriteGACrossDomainParam(eventUrl)).withCookies(Cookie(eventCookie(event), "", Some(3600)))
  }


  def thankyou(id: String, orderIdOpt: Option[String]) = SubscriptionAction.async { implicit request =>
    orderIdOpt.fold {
      val resultOpt = for {
        oid <- request.flash.get("oid")
        event <- EventbriteService.getEvent(id)
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
    EventbriteService.getEvent(id).map { event =>
      // only log a conversion if the user came from a membership event page
      NoContent.discardingCookies(DiscardingCookie(eventCookie(event)))
    }.getOrElse(NotFound)
  }

  def preview(id: String) = GoogleAuthenticatedStaffAction.async { implicit request =>
     EventbriteService.getPreviewEvent(id).map(eventDetail)
  }

  def previewMasterclass(id: String) = GoogleAuthenticatedStaffAction.async { implicit request =>
   EventbriteService.getPreviewMasterclass(id).map(eventDetail)
  }
}
