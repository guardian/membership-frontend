package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 14/08/2014.
 */
class JoinFlowRegisterOrSignInPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def signInButton = driver.findElement(By.xpath("//div[2]/div/div/section/div[2]/div[1]/a"))

  private def registerButton = driver.findElement(By.xpath("//div[2]/div/div/section/div[2]/div[2]/a"))

  def clickSignIn = {
    signInButton.click()
    new LoginPage(driver)
  }

  def clickRegister = {
    registerButton.click()
    new RegisterPage(driver)
  }
}
