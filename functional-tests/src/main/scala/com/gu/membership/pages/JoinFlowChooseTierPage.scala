package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 13/08/2014.
 */
class JoinFlowChooseTierPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def registerButton = driver.findElement(By.xpath("//div[2]/div/div/section/div[2]/div[2]/a"))

  private def signInButton = driver.findElement(By.xpath("//div[2]/div/div/section/div[2]/div[1]/a"))

  def clickRegisterButton = {
    registerButton.click()
    new RegisterPage(driver)
  }

  def clickFriendButton = {
    signInButton.click()
    new JoinFlowRegisterOrSignInPage(driver)
  }

  def isPageLoaded = registerButton.isDisplayed && signInButton.isDisplayed
}
