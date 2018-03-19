package wiring

import play.api.ApplicationLoader.Context
import play.api._

class AppLoader extends ApplicationLoader {

  override def load(context: Context): Application = {

    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }

    (new BuiltInComponentsFromContext(context) with AppComponents).application
  }
}
