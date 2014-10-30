package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 13/08/2014.
 */
class JoinFlowChooseTierPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def friendButton = driver.findElement(By.cssSelector(".package:nth-of-type(1) .action"))

  private def patronButton = driver.findElement(By.cssSelector(".package:nth-of-type(2) .action"))

  private def partnerButton = driver.findElement(By.cssSelector(".package:nth-of-type(3) .action"))

  def clickPatronButton = {
    patronButton.click
    new JoinFlowRegisterOrSignInPage(driver)
  }

  def clickPartnerButton = {
    partnerButton.click
    new JoinFlowRegisterOrSignInPage(driver)
  }

  def clickFriendButton = {
    friendButton.click()
    new JoinFlowRegisterOrSignInPage(driver)
  }

  def isPageLoaded = friendButton.isDisplayed
}
