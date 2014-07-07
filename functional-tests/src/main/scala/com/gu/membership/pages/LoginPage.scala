package com.gu.membership.pages

import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.{By, Keys, StaleElementReferenceException, WebDriver}

class LoginPage(driver: WebDriver) extends BasePage(driver) {

  private def emailTextbox = driver.findElement(By.id("email"))

  private def passwordTextbox = driver.findElement(By.id("password"))

  private def submitButton = driver.findElement(By.cssSelector(".form-field>button"))

  private def registerNowLink = driver.findElement(By.cssSelector(".u-underline"))

  def enterEmail(email: String): LoginPage = {
    emailTextbox.sendKeys(email)
    this
  }

  def enterPassword(password: String): LandingPage = {
    try {
      passwordTextbox.sendKeys(password + Keys.ENTER)
    } catch {
      case e: StaleElementReferenceException => // do nothing - the exception is thrown incorrectly
    }
    new LandingPage(driver)
  }

  def clickSubmit: LandingPage = {
    submitButton.click
    new LandingPage(driver)
  }

  def clickRegister: RegisterPage = {
    new Actions(driver).moveToElement(registerNowLink).perform
    registerNowLink.click()
    new RegisterPage(driver)
  }

  def login(email: String, password: String): LandingPage = enterEmail(email).enterPassword(password)

  def isPageLoaded: Boolean = (emailTextbox.isDisplayed && passwordTextbox.isDisplayed && submitButton.isDisplayed)
}
