package com.gu.membership.pages

import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, JavascriptExecutor, WebDriver}

/**
 * Created by jao on 16/06/2014.
 */
class RegisterPage(driver: WebDriver) extends BasePage(driver) {

  private def emailTextbox = driver.findElement(By.id("user_primaryEmailAddress"))

  private def firstNameTextbox = driver.findElement(By.id("user_firstName"))

  private def lastNameTextbox = driver.findElement(By.id("user_secondName"))

  private def userNameTextbox = driver.findElement(By.id("user_publicFields_username"))

  private def passwordTextbox = driver.findElement(By.id("user_password"))

  private def submitButton = driver.findElement(By.cssSelector(".submit-input"))

  private def closeBetaButton = driver.findElement(By.cssSelector(".i-close-icon-white-small"))

  def enterFirstName(firstName: String) = {
    firstNameTextbox.sendKeys(firstName)
    this
  }

  def enterLastName(lastName: String) = {
    lastNameTextbox.sendKeys(lastName)
    this
  }

  def enterEmail(email: String): RegisterPage = {
    emailTextbox.sendKeys(email)
    this
  }

  def enterUserName(userName: String): RegisterPage = {
    userNameTextbox.sendKeys(userName)
    this
  }

  def enterPassword(password: String): RegisterPage = {
    passwordTextbox.sendKeys(password)
    this
  }

  def clickSubmit: CheckEmailPage = {
    try {
      closeBetaButton.click()
      new Actions(driver).moveToElement(submitButton).perform
      submitButton.click()
    } catch {
      case _ : Throwable => ; driver.asInstanceOf[JavascriptExecutor].executeScript("document.forms[0].submit()")
    }
    new WebDriverWait(driver, 30).until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".submit-input")))
    new CheckEmailPage(driver)
  }
}
