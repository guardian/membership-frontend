package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 14/08/2014.
 */
class JoinFlowRegisterOrSignInPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def signInButton = driver.findElement(By.cssSelector(".signin__login a"))

  private def registerButton = driver.findElement(By.cssSelector(".signin__register a"))

  def clickSignIn = {
    signInButton.click()
    new LoginPage(driver)
  }

  def clickRegister = {
    registerButton.click()
    new RegisterPage(driver)
  }

  def isPageLoaded = {
    registerButton.isDisplayed && signInButton.isDisplayed
  }
}
