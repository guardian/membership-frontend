package acceptance.util

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import scala.util.{Try, Failure, Success}

object Config {
  private def logger = LoggerFactory.getLogger(this.getClass)

  private val conf = ConfigFactory.load()

  val baseUrl = conf.getString("membership.url")

  val identityFrontendUrl = conf.getString("identity.webapp.url")

  val testUsersSecret = conf.getString("identity.test.users.secret")

  val webDriverRemoteUrl = Try(conf.getString("webDriverRemoteUrl")) match {
    case Success(url) => url
    case Failure(e) => ""
  }

  val screencastIdFile = conf.getString("screencastId.file")

  val waitTimeout: Int = conf.getInt("waitTimeout")

  val paypalBuyerEmail = conf.getString("paypal.sandbox.buyer.email")

  val paypalBuyerPassword = conf.getString("paypal.sandbox.buyer.password")

  def debug() { conf.root().render() }

  def printSummary(): Unit = {
    logger.info("Acceptance Test Configuration")
    logger.info("=============================")
    logger.info(s"Stage: ${conf.getString("stage")}")
    logger.info(s"Membership Frontend: ${baseUrl}")
    logger.info(s"Identity Frontend: ${identityFrontendUrl}")
    logger.info(s"Screencast = https://saucelabs.com/tests/${Driver.sessionId}")
  }
}
