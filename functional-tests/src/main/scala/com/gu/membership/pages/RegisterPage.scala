package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 16/06/2014.
 */
class RegisterPage(driver: WebDriver) extends BasePage(driver) {

  private def emailTextbox = driver.findElement(By.id("user_primaryEmailAddress"))

  private def userNameTextbox = driver.findElement(By.id("user_publicFields_username"))

  private def passwordTextbox = driver.findElement(By.id("user_password"))

  private def submitButton = driver.findElement(By.cssSelector(".submit-input"))

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
    submitButton.click()
    new CheckEmailPage(driver)
  }
}
