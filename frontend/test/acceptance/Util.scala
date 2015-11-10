package acceptance

import acceptance.Config.baseUrl
import java.net.URL
import java.util.concurrent.TimeUnit
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, Cookie, WebDriver}
import org.scalatest.selenium.WebBrowser
import scala.collection.JavaConverters._
import scala.util.Try
import com.gu.identity.testing.usernames.TestUsernames
import com.github.nscala_time.time.Imports._

trait Util { this: WebBrowser =>
  implicit val driver: WebDriver

  private val defaultTimeOut = 90

  def resetDriver() = {
    driver.get("about:about")
    go.to(baseUrl)
    driver.manage().deleteAllCookies()
    driver.manage().timeouts().implicitlyWait(defaultTimeOut, TimeUnit.SECONDS)

    addTestUserCookies()
  }

  private def addTestUserCookies() = {
    val analyticsCookie = new Cookie("ANALYTICS_OFF_KEY", "true")
    driver.manage().addCookie(analyticsCookie)

    val testUserCookie =
      new Cookie.Builder("pre-signin-test-user", TestUser.specialString)
        .isHttpOnly(true).build()
    driver.manage().addCookie(testUserCookie)
  }

  protected def pageHasText(text: String, timeoutSecs: Int=defaultTimeOut): Boolean = {
    val pred = ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), text)
    Try {
      new WebDriverWait(driver, timeoutSecs).until(pred)
    }.isSuccess
  }

  protected def pageHasElement(q: Query, timeoutSecs: Int=defaultTimeOut): Boolean = {
    val pred = ExpectedConditions.visibilityOfElementLocated(q.by)
    Try {
      new WebDriverWait(driver, timeoutSecs).until(pred)
    }.isSuccess
  }

  protected def currentHost: String = new URL(currentUrl).getHost

  def cookiesSet: Set[Cookie] = driver.manage().getCookies.asScala.toSet
}

object TestUser {
  private val testUsers = TestUsernames(
    com.gu.identity.testing.usernames.Encoder.withSecret(Config.testUsersSecret),
    recency = 2.days.standardDuration
  )
  val specialString = testUsers.generate()

  def isValid(username: String): Boolean = {
    testUsers.isValid(specialString)
  }
}
