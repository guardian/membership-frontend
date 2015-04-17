package controllers

import actions.AnyMemberTierRequest
import actions.Fallbacks._
import actions.Functions._
import com.github.nscala_time.time.Imports._
import com.gu.membership.salesforce.{Member, Tier}
import com.gu.membership.util.Timing
import com.netaporter.uri.dsl._
import configuration.{CopyConfig, Links}
import model.EmbedSerializer._
import model.Eventbrite.{EBEvent, EBOrder}
import model.RichEvent.{RichEvent, _}
import model.{EmbedData, EventPortfolio, Eventbrite, PageInfo, _}
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services.{EventbriteService, GuardianLiveEventService, LocalEventService, MasterclassEventService, MemberService, _}
import services.EventbriteService._
import tracking._

import scala.concurrent.Future

trait Event extends Controller with ActivityTracking {

  val guLiveEvents: EventbriteService
  val localEvents: EventbriteService
  val masterclassEvents: EventbriteService

  val memberService: MemberService

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
    authenticated(onUnauthenticated = notYetAMemberOn(_)) andThen memberRefiner()

  def details(slug: String) = CachedAction { implicit request =>
    val eventOpt = for {
      id <- EBEvent.slugToId(slug)
      event <- EventbriteService.getEvent(id)
    } yield {
      if (slug == event.slug) {
        trackAnon(EventActivity("viewEventDetails", None, EventData(event)))
        eventDetail(event)
      } else {
        Redirect(routes.Event.details(event.slug))
      }
    }

    eventOpt.getOrElse(Redirect(Links.membershipFront))
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
      event.name.text,
      request.path,
      event.description.map(_.blurb),
      event.socialImgUrl,
      Some(event.schema)
    )
    Ok(views.html.event.page(event, pageInfo))
  }

  def masterclasses = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleMasterclasses,
      request.path,
      Some(CopyConfig.copyDescriptionMasterclasses)
    )
    Ok(views.html.event.masterclass(EventPortfolio(Nil, masterclassEvents.events, None, None), pageInfo))
  }

  def masterclassesByTag(rawTag: String, rawSubTag: String = "") = CachedAction { implicit request =>
    val tag = MasterclassEvent.decodeTag( if(rawSubTag.nonEmpty) rawSubTag else rawTag )
    val pageInfo = PageInfo(
      CopyConfig.copyTitleMasterclasses,
      request.path,
      Some(CopyConfig.copyDescriptionMasterclasses)
    )
    Ok(views.html.event.masterclass(
      EventPortfolio(Nil, masterclassEvents.getTaggedEvents(tag), None, None),
      pageInfo,
      MasterclassEvent.decodeTag(rawTag),
      MasterclassEvent.decodeTag(rawSubTag)
    ))
  }

  private def chronologicalSort(events: Seq[model.RichEvent.RichEvent]) = {
    events.sortWith(_.event.start < _.event.start)
  }

  def whatsOn = GoogleAuthenticatedStaffAction { implicit request =>

    val pageInfo = PageInfo(
      CopyConfig.copyTitleEvents,
      request.path,
      Some(CopyConfig.copyDescriptionEvents)
    )

    val now = DateTime.now

    val events = EventCollections(
      // unstruct_event_1.eventSource:"viewEventDetails"
      trending=guLiveEvents.getEventsByIds(List(
        "15926171608",
        "16351430569",
        "15351696337",
        "15756402825",
        "16253355223",
        "16234171845",
        "16349419554",
        "15597340064",
        "16253236869",
        "16430264363"
      )),
      // unstruct_event_1.eventSource:"eventThankYou"
      topSelling=guLiveEvents.getEventsByIds(List(
        "16253236869",
        "16253494640",
        "16234171845",
        "16351430569",
        "15351696337",
        "16252865759",
        "16253323127",
        "15926171608",
        "15597340064",
        "16380673034"
      )),
      thisWeek=guLiveEvents.getEventsBetween(new Interval(now, now + 1.week)),
      nextWeek=guLiveEvents.getEventsBetween(new Interval(now + 1.week, now + 2.weeks)),
      recentlyCreated=guLiveEvents.getRecentlyCreated(now - 1.weeks),
      partnersOnly=guLiveEvents.getEvents.filter(_.internalTicketing.exists(_.isCurrentlyAvailableToPaidMembersOnly)),
      programmingPartnerEvents=guLiveEvents.getPartnerEvents
    )

    val latestArticles = GuardianContentService.membershipFrontContent.map(MembersOnlyContent)

    Ok(views.html.event.whatson(pageInfo, events, latestArticles))
  }

  def list = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleEvents,
      request.path,
      Some(CopyConfig.copyDescriptionEvents)
    )
    val pastEvents = (guLiveEvents.getEventsArchive ++ localEvents.getEventsArchive).headOption
      .map(chronologicalSort(_).reverse)
    Ok(views.html.event.guardianLive(
      EventPortfolio(
        guLiveEvents.getFeaturedEvents,
        chronologicalSort(guLiveEvents.getEvents ++ localEvents.getEvents),
        pastEvents,
        guLiveEvents.getPartnerEvents
      ),
      pageInfo))
  }

  def listFilteredBy(urlTagText: String) = CachedAction { implicit request =>
    val tag = urlTagText.replace('-', ' ')
    val pageInfo = PageInfo(
      CopyConfig.copyTitleEvents,
      request.path,
      Some(CopyConfig.copyDescriptionEvents)
    )
    Ok(views.html.event.guardianLive(
      EventPortfolio(
        Seq.empty,
        chronologicalSort(guLiveEvents.getTaggedEvents(tag) ++ localEvents.getTaggedEvents(tag)),
        None,
        None
      ),
      pageInfo))
  }

  def buy(id: String) = BuyAction(id).async { implicit request =>
    EventbriteService.getEvent(id).map { event =>
      event match {
        case _: GuLiveEvent | _: LocalEvent =>
          if (tierCanBuyTickets(event, request.member.tier)) redirectToEventbrite(request, event)
          else Future.successful(Redirect(routes.TierController.change()))

        case _: MasterclassEvent =>
          redirectToEventbrite(request, event)
      }
    }.getOrElse(Future.successful(NotFound))
  }

  private def tierCanBuyTickets(event: Eventbrite.EBEvent, tier: Tier): Boolean =
    event.internalTicketing.exists(_.salesDates.tierCanBuyTicket(tier))

  private def eventCookie(event: RichEvent) = s"mem-event-${event.id}"

  private def redirectToEventbrite(request: AnyMemberTierRequest[AnyContent], event: RichEvent): Future[Result] =
    Timing.record(event.service.wsMetrics, s"user-sent-to-eventbrite-${request.member.tier}") {
      for {
        discountOpt <- memberService.createDiscountForMember(request.member, event)
      } yield {
          val memberData = MemberData(request.member.salesforceContactId, request.user.id, request.member.tier.name)
          track(EventActivity("redirectToEventbrite", Some(memberData), EventData(event)))(request.user)
        Found(event.url ? ("discount" -> discountOpt.map(_.code)))
          .withCookies(Cookie(eventCookie(event), "", Some(3600)))
      }
    }

  private def trackConversionToThankyou(request: Request[_], event: RichEvent, order: Option[EBOrder],
                                        member: Option[Member]) {
    val memberData = member.map(m => MemberData(m.salesforceContactId, m.identityId, m.tier.name))
    trackAnon(EventActivity("eventThankYou", memberData, EventData(event), order.map(OrderData(_))))(request)
  }

  def thankyou(id: String, orderIdOpt: Option[String]) = MemberAction.async { implicit request =>
    orderIdOpt.fold {
      val resultOpt = for {
        oid <- request.flash.get("oid")
        event <- guLiveEvents.getBookableEvent(id)
      } yield {
        guLiveEvents.getOrder(oid).map { order =>
          trackConversionToThankyou(request, event, Some(order), Some(request.member))
          Ok(views.html.event.thankyou(event, order)).discardingCookies(DiscardingCookie(eventCookie(event)))
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
        trackConversionToThankyou(request, event, None, None)
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
  val memberService = MemberService
}
