package acceptance.util

import java.net.URL
import com.typesafe.config.ConfigFactory
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}
import org.openqa.selenium.{Platform, WebDriver}
import org.slf4j.LoggerFactory
import scala.util.Try

object Config {
  def logger = LoggerFactory.getLogger(this.getClass)

  private val conf = ConfigFactory.load()

  val baseUrl = conf.getString("membership.url")
  val profileUrl = conf.getString("identity.webapp.url")
  val testUsersSecret = conf.getString("identity.test.users.secret")

  lazy val driver: WebDriver = {
    Try { new URL(conf.getString("webDriverRemoteUrl")) }.toOption.map { url =>
      val capabilities = DesiredCapabilities.firefox()
      capabilities.setCapability("platform", Platform.WIN8)
      capabilities.setCapability("name", "membership-frontend: https://github.com/guardian/membership-frontend")
      new RemoteWebDriver(url, capabilities)
    }.getOrElse {
      new FirefoxDriver()
    }
  }

  def webDriverSessionId(): String = {
    Config.driver match {
      case remoteDriver: RemoteWebDriver => {
        remoteDriver.getSessionId.toString
      }
      case _ => "unknown session ID"
    }
  }

  def stage(): String = {
    conf.getString("stage")
  }

  def debug() = {
    conf.root().render()
  }

  def printSummary(): Unit = {
    logger.info("Acceptance Test Configuration")
    logger.info("=============================")
    logger.info(s"Stage: ${stage}")
    logger.info(s"Membership Frontend: ${Config.baseUrl}")
    logger.info(s"Identity Frontend: ${conf.getString("identity.webapp.url")}")
    logger.info(s"WebDriver Session ID = ${Config.webDriverSessionId}")
  }
}
