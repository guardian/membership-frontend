package steps

import java.io.FileInputStream
import java.util.{Properties, Random}

import com.gu.automation.support.Config
import com.gu.identity.testing.usernames.{Encoder, TestUsernames}
import com.gu.membership.pages.{LoginPage, RegisterPage}
import org.openqa.selenium.{Cookie, JavascriptExecutor, WebDriver}

/**
 * Created by spike on 19/06/15.
 */
object CookieHandler {

  var loginCookie: Option[Cookie] = None
  var secureCookie: Option[Cookie] = None
  val surveyCookie = new Cookie("gu.test", "test")

  def login(driver: WebDriver) {
    driver.get(Config().getUserValue("identityReturn"))
    disableAnalytics(driver)
    new LoginPage(driver).clickRegister
    register(driver)
  }

  def register(driver: WebDriver) {
    driver.manage().addCookie(surveyCookie)
    val propertyName="identity.test.users.secret"

    val file: String = "/etc/gu/membership-keys.conf"

    val prop = new Properties()
    prop.load(new FileInputStream(file))

    val secret = prop.getProperty(propertyName).replace("\"","")

    val usernames = TestUsernames(Encoder.withSecret(secret))

    val salt: Array[Byte] = new Array[Byte](2)

    new Random().nextBytes(salt)
    val user = usernames.generate(salt)
    val password = scala.util.Random.alphanumeric.take(10).mkString
    val email = user + "@testme.com"
    new RegisterPage(driver).enterFirstName(user).enterLastName(user).enterEmail(email)
      .enterPassword(password).enterUserName(user).clickSubmit
  }

  def disableAnalytics(driver: WebDriver): Unit = {
    driver.asInstanceOf[JavascriptExecutor].executeScript("document.cookie = \"ANALYTICS_OFF_KEY=1; domain=.thegulocal.com; path=/; secure\"")
  }
}
