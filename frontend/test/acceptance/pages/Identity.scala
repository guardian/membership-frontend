package acceptance.pages

import acceptance.util.{TestUser, Browser, Config}
import Config._
import org.scalatest.selenium.Page
import java.net.URLEncoder

trait IdentityStep extends Page with Browser {

  private val returnUrlParam = URLEncoder.encode(s"${baseUrl}/join/supporter/enter-details", "UTF-8")
  val url = s"${identityFrontendUrl}/signin?returnUrl=${returnUrlParam}&skipConfirmation=true&clientId=members"

  val submitButton: IdQuery

  def submit() { clickOn(submitButton) }

  def pageHasLoaded: Boolean = pageHasElement(submitButton)

}

case class FirstRegistrationStep(testUser: TestUser) extends IdentityStep {

  private val email = id("tssf-email")

  override val submitButton = id("tssf-submit")

  def fillInEmail() = setValue(email, s"${testUser.username}@gu.com", clear=true)

}

case class SecondRegistrationStep(testUser: TestUser) extends IdentityStep {

  def fillInPersonalDetails() { RegisterFields.fillIn() }

  private object RegisterFields {
    val firstName = id("register_field_firstname")
    val lastName = id("register_field_lastname")
    val password = id("register_field_password")

    def fillIn() {
      setValue(firstName, testUser.username, clear=true)
      setValue(lastName, testUser.username, clear=true)
      setValue(password, testUser.username, clear=true)
    }
  }

  override val submitButton = id("tssf-create-account")

}
