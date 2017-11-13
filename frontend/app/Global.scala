import configuration.Config
import loghandling.Logstash
import monitoring.{HealthMonitoringTask, SentryLogging}
import play.api.mvc.{EssentialAction, EssentialFilter}
import play.api.{Application, GlobalSettings}
import play.filters.csrf._
import services._

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    HealthMonitoringTask.start(app.actorSystem, play.api.libs.concurrent.Execution.Implicits.defaultContext, Config.stage, Config.appName)
    SentryLogging.init()
    Logstash.init(Config)
    GuardianLiveEventService.start()
    MasterclassEventService.start()
    GuardianContentService.start()
  }
}

// taken from http://dominikdorn.com/2014/07/playframework-2-3-global-csrf-protection-disable-csrf-selectively/
// play 2.5 removes the need for this by considering trusted CORS routes exempt from CSRF
private class ExcludingCSRFFilter(filter: CSRFFilter) extends EssentialFilter {

  override def apply(nextFilter: EssentialAction) = new EssentialAction {

    import play.api.mvc._

    override def apply(rh: RequestHeader) = {
      val chainedFilter = filter.apply(nextFilter)
      if (rh.tags.getOrElse("ROUTE_COMMENTS", "").contains("NOCSRF")) {
        nextFilter(rh)
      } else {
        chainedFilter(rh)
      }
    }
  }
}
