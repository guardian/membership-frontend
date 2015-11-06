package acceptance.pages

import acceptance.Config.profileUrl
import acceptance.Config.baseUrl
import acceptance.{TestUser, Util}
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.{WebBrowser, Page}

class Register(implicit val driver: WebDriver) extends Page with WebBrowser with Util {
  val url = s"${profileUrl}/register?returnUrl=${baseUrl}/&skipConfirmation=true"

  private object RegisterFields {
    val firstName = textField(id("user_firstName"))
    val lastName = textField(id("user_secondName"))
    val email = emailField(id("user_primaryEmailAddress"))
    val username = textField(id("user_publicFields_username"))
    val password = pwdField(id("user_password"))

    def fillIn(): Unit = {
      assert(pageHasElement(id("user_password")))

      firstName.value = TestUser.specialString
      lastName.value = TestUser.specialString
      email.value = s"${TestUser.specialString}@gu.com"
      username.value = TestUser.specialString
      password.value = TestUser.specialString
    }
  }

  def fillInPersonalDetails(): Unit = {
    RegisterFields.fillIn()
  }

  def submit(): Unit = {
    val selector = className("submit-input")
    assert(pageHasElement(selector))
    click.on(selector)
  }

  def pageHasLoaded(): Boolean = {
    pageHasElement(className("submit-input"))
  }
}
