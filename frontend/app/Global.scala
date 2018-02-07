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

