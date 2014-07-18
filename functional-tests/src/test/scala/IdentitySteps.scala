import com.gu.automation.support.{Config, TestLogger}
import com.gu.membership.pages.LoginPage
import org.openqa.selenium.WebDriver

/**
 * Created by jao on 16/07/2014.
 */
case class IdentitySteps(implicit driver: WebDriver, logger: TestLogger) {

  def ISignUp = {
    driver.get(Config().getUserValue("identity"))
    val user = System.currentTimeMillis().toString + "@testme.com"
    val password = scala.util.Random.alphanumeric.take(10).mkString

    new LoginPage(driver).clickRegister.enterEmail(user)
      .enterPassword(password).enterUserName(user).clickSubmit.clickCompleteRegistration
    this
  }

  def ILogIn = {
    driver.get(Config().getUserValue("identity"))
  }
}
