package controllers

import actions.AnyMemberTierRequest
import actions.Fallbacks._
import actions.Functions._
import com.github.nscala_time.time.Imports._
import com.gu.membership.salesforce.{Member, Tier}
import com.gu.membership.util.Timing
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import configuration.CopyConfig
import model.EmbedSerializer._
import model.Eventbrite.{EBEvent, EBOrder}
import model.RichEvent.{RichEvent, _}
import model.{EmbedData, EventPortfolio, Eventbrite, PageInfo}
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services.{EventbriteService, GuardianLiveEventService, LocalEventService, MasterclassEventService, MemberService}
import services.EventbriteService._
import tracking._
import utils.CampaignCode.extractCampaignCode
import utils.TestUsers.isTestUser

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

    eventOpt.getOrElse(Redirect(routes.Event.list))
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
    Ok(views.html.event.masterclass(pageInfo, masterclassEvents.events))
  }

  def masterclassesByTag(rawTag: String, rawSubTag: String = "") = CachedAction { implicit request =>
    val tag = MasterclassEvent.decodeTag( if(rawSubTag.nonEmpty) rawSubTag else rawTag )
    val pageInfo = PageInfo(
      CopyConfig.copyTitleMasterclasses,
      request.path,
      Some(CopyConfig.copyDescriptionMasterclasses)
    )
    Ok(views.html.event.masterclass(
      pageInfo,
      masterclassEvents.getTaggedEvents(tag),
      MasterclassEvent.decodeTag(rawTag),
      MasterclassEvent.decodeTag(rawSubTag)
    ))
  }

  private def chronologicalSort(events: Seq[model.RichEvent.RichEvent]) = {
    events.sortWith(_.event.start < _.event.start)
  }

  def list = CachedAction { implicit request =>

    val archivedEvents =
      guLiveEvents.getEventsArchive.toList.flatten ++ localEvents.getEventsArchive.toList.flatten

    Ok(views.html.event.guardianLive(
      EventPortfolio(
        guLiveEvents.getFeaturedEvents,
        chronologicalSort(guLiveEvents.getEvents ++ localEvents.getEvents),
        chronologicalSort(archivedEvents).reverse,
        guLiveEvents.getPartnerEvents
      ),
      PageInfo(
        CopyConfig.copyTitleEvents,
        request.path,
        Some(CopyConfig.copyDescriptionEvents)
      ))
    )
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
        Seq.empty,
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

      memberService.createEBCode(request.member, event).map { code =>
        val eventUrl = code.fold(Uri.parse(event.url))(c => event.url ? ("discount" -> c.code))
        val memberData = MemberData(request.member.salesforceContactId, request.user.id, request.member.tier.name, campaignCode = extractCampaignCode(request))

        track(EventActivity("redirectToEventbrite", Some(memberData), EventData(event)))(request.user)

        Found(eventUrl)
          .withCookies(Cookie(eventCookie(event), "", Some(3600)))
      }
    }

  private def trackConversionToThankyou(request: Request[_], event: RichEvent, order: Option[EBOrder],
                                        member: Option[Member]) {
    val memberData = member.map(m => MemberData(m.salesforceContactId, m.identityId, m.tier.name, campaignCode=extractCampaignCode(request)))
    trackAnon(EventActivity("eventThankYou", memberData, EventData(event), order.map(OrderData)))(request)
  }

  def thankyou(id: String, orderIdOpt: Option[String]) = MemberAction.async { implicit request =>
    orderIdOpt.fold {
      val resultOpt = for {
        oid <- request.flash.get("oid")
        event <- EventbriteService.getEvent(id)
      } yield {
        event.service.getOrder(oid).map { order =>
          val count = memberService.countComplimentaryTicketsInOrder(event, order)
          if (count > 0 && isTestUser(request.user)) {
            memberService.recordFreeEventUsage(request.member, event, order, count)
          }

          trackConversionToThankyou(request, event, Some(order), Some(request.member))

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
