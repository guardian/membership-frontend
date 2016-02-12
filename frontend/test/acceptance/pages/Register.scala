package acceptance.pages

import acceptance.util.{TestUser, Browser, Config}
import Config.identityFrontendUrl
import Config.baseUrl
import org.scalatest.selenium.Page
import java.net.URLEncoder

class Register(testUser: TestUser) extends Page with Browser {
  val returnUrlParam = URLEncoder.encode(s"${baseUrl}/join/partner/enter-details", "UTF-8")
  val url = s"${identityFrontendUrl}/register?returnUrl=${returnUrlParam}&skipConfirmation=true"

  private object RegisterFields {
    val firstName = textField(id("register_field_firstname"))
    val lastName = textField(id("register_field_lastname"))
    val email = emailField(id("register_field_email"))
    val username = textField(id("register_field_username"))
    val password = pwdField(id("register_field_password"))
    def fillIn(): Unit = {
      assert(pageHasElement(id("register_field_password")))

      firstName.value = testUser.username
      lastName.value = testUser.username
      email.value = s"${testUser.username}@gu.com"
      username.value = testUser.username
      password.value = testUser.username
    }
  }

  def fillInPersonalDetails(): Unit = {
    RegisterFields.fillIn()
  }

  def submit(): Unit = {
    val selector = id("register_submit")
    assert(pageHasElement(selector))
    click.on(selector)
  }

  def pageHasLoaded(): Boolean = {
    pageHasElement(id("register_submit"))
  }
}
