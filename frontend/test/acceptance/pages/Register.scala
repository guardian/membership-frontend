package acceptance.pages

import acceptance.util.{TestUser, Browser, Config}
import Config._
import org.scalatest.selenium.Page
import java.net.URLEncoder

case class Register(testUser: TestUser) extends Page with Browser {
  private val returnUrlParam = URLEncoder.encode(s"${baseUrl}/join/supporter/enter-details", "UTF-8")
  val url = s"${identityFrontendUrl}/register?returnUrl=${returnUrlParam}&skipConfirmation=true&clientId=members"

  def fillInPersonalDetails() { RegisterFields.fillIn() }

  def submit() { clickOn(submitButton) }

  def pageHasLoaded: Boolean = pageHasElement(submitButton)

  private object RegisterFields {
    val firstName = id("register_field_firstname")
    val lastName = id("register_field_lastname")
    val email = id("register_field_email")
    val password = id("register_field_password")

    def fillIn() {
      setValue(firstName, testUser.username)
      setValue(lastName, testUser.username)
      setValue(email, s"${testUser.username}@gu.com")
      setValue(password, testUser.username)
    }
  }

  private val submitButton = id("register_submit")
}
