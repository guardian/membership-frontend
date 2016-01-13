package controllers

import com.gu.memsub.services.CatalogService
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import services._
import views.support.Catalog

trait Staff extends Controller {
  val guLiveEvents = GuardianLiveEventService
  val localEvents = LocalEventService
  val masterclassEvents = MasterclassEventService

  def eventOverview = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.eventOverview.live(guLiveEvents.events, guLiveEvents.eventsDraft, request.path))
  }

  def eventOverviewLocal = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.eventOverview.local(localEvents.events, localEvents.eventsDraft, request.path))
  }

  def eventOverviewMasterclasses = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.eventOverview.masterclasses(masterclassEvents.events, masterclassEvents.eventsDraft, request.path))
  }

  def eventOverviewDetails = GoogleAuthenticatedStaffAction { implicit request =>
    Ok(views.html.eventOverview.details(request.path))
  }

  def catalogDiagnostics = GoogleAuthenticatedStaffAction.async { implicit request =>
    val Seq(testCat, normalCat) = Seq(TouchpointBackend.TestUser, TouchpointBackend.Normal).map { be =>
      CatalogService.makeMembershipCatalog(be.zuoraRestClient, be.membershipRatePlanIds)(Akka.system)
    }

    testCat.zip(normalCat).map((Catalog.Diagnostic.fromCatalogs _).tupled).map { diagnostic =>
      Ok(views.html.staff.catalogDiagnostic(diagnostic))
    }
  }
}

object Staff extends Staff
