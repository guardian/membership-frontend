package controllers

import actions.{Subscriber, SubscriptionRequest}
import actions.Fallbacks._
import actions.ActionRefiners._
import com.gu.memsub.Subscriber.Member
import com.gu.salesforce.Tier
import com.gu.memsub.util.Timing
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import model.EmbedSerializer._
import model.Eventbrite.{EBEvent, EBOrder}
import model.RichEvent.{RichEvent, _}
import model._
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services.{EventbriteService, GuardianLiveEventService, LocalEventService, MasterclassEventService}
import services.EventbriteService._
import tracking._
import utils.CampaignCode
import views.support.PageInfo

import scala.concurrent.Future

trait Event extends Controller with MemberServiceProvider with ActivityTracking {

  val guLiveEvents: EventbriteService
  val localEvents: EventbriteService
  val masterclassEvents: EventbriteService

  private def recordBuyIntention(eventId: String) = new ActionBuilder[Request] {
    override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      EventbriteService.getEvent(eventId).map { event =>
        trackAnon(EventActivity("buyActionInvoked", None, EventData(event)))(request)
        Timing.record(event.service.wsMetrics, "buy-action-invoked") {
          block(request)
        }
      }.getOrElse(Future.successful(NotFound))
    }
  }

  private def BuyAction(id: String) = NoCacheAction andThen recordBuyIntention(id) andThen
    authenticated(onUnauthenticated = notYetAMemberOn(_)) andThen subscriptionRefiner()

  def details(slug: String) = CachedAction { implicit request =>
    val eventOpt = for {
      id <- EBEvent.slugToId(slug)
      correctEvent <-(Eventbrite.HiddenEvents.get(id).toSeq :+ id).flatMap(EventbriteService.getEvent).headOption
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


  def buy(id: String) = BuyAction(id).async { implicit request =>
    EventbriteService.getEvent(id).map {
      case event@(_: GuLiveEvent | _: LocalEvent) =>
        if (event.isBookableByTier(request.subscriber.subscription.plan.tier))
          redirectToEventbrite(event)
        else
          Future.successful(Redirect(routes.TierController.change()).addingToSession("preJoinReturnUrl" -> request.uri))
      case event@(_: MasterclassEvent) =>
        redirectToEventbrite(event)
    }.getOrElse(Future.successful(NotFound))
  }

  private def eventCookie(event: RichEvent) = s"mem-event-${event.id}"

  private def addEventBriteGACrossDomainParam(uri: Uri)(implicit request: Request[AnyContent]): Uri = {
    // https://www.eventbrite.co.uk/support/articles/en_US/Troubleshooting/how-to-enable-cross-domain-and-ecommerce-tracking-with-google-universal-analytics
    request.cookies.get("_ga").map(_.value.replaceFirst("GA\\d+\\.\\d+\\.", "")).fold(uri)(value => uri & ("_eboga", value))
  }

  private def redirectToEventbrite(event: RichEvent)(implicit request: SubscriptionRequest[AnyContent] with Subscriber): Future[Result] =
    Timing.record(event.service.wsMetrics, s"user-sent-to-eventbrite-${request.subscriber.subscription.plan.tier}") {

      memberService.createEBCode(request.subscriber, event).map { code =>
        val eventUrl = code.fold(Uri.parse(event.url))(c => event.url ? ("discount" -> c.code))

        val memberData = MemberData(
          salesforceContactId = request.subscriber.contact.salesforceContactId,
          identityId = request.user.id,
          tier = request.subscriber.subscription.plan.tier,
          campaignCode = CampaignCode.fromRequest)

        track(EventActivity("redirectToEventbrite", Some(memberData), EventData(event)),request.user)

        Found(addEventBriteGACrossDomainParam(eventUrl))
          .withCookies(Cookie(eventCookie(event), "", Some(3600)))
      }
    }

  private def trackConversionToThankyou(event: RichEvent, order: Option[EBOrder],
                                        member: Option[Member])(implicit request: Request[_]) {

    val memberData = member.map(m => MemberData(
      salesforceContactId = m.contact.salesforceContactId,
      identityId = m.contact.identityId,
      tier = m.subscription.plan.tier,
      campaignCode = CampaignCode.fromRequest))

    trackAnon(EventActivity("eventThankYou", memberData, EventData(event), order.map(OrderData)))(request)
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
    EventbriteService.getEvent(id).map { event =>
      // only log a conversion if the user came from a membership event page
      request.cookies.get(eventCookie(event)).foreach { _ =>
        trackConversionToThankyou(event, None, None)
      }
      NoContent.discardingCookies(DiscardingCookie(eventCookie(event)))
    }.getOrElse(NotFound)
  }

  def preview(id: String) = GoogleAuthenticatedStaffAction.async { implicit request =>
     EventbriteService.getPreviewEvent(id).map(eventDetail)
  }

  def previewLocal(id: String) = GoogleAuthenticatedStaffAction.async { implicit request =>
    EventbriteService.getPreviewLocalEvent(id).map(eventDetail)
  }

  def previewMasterclass(id: String) = GoogleAuthenticatedStaffAction.async { implicit request =>
   EventbriteService.getPreviewMasterclass(id).map(eventDetail)
  }
}

object Event extends Event {
  val guLiveEvents = GuardianLiveEventService
  val localEvents = LocalEventService
  val masterclassEvents = MasterclassEventService
}
