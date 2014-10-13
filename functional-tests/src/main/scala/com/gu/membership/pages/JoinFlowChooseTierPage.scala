package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 13/08/2014.
 */
class JoinFlowChooseTierPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def friendButton = driver.findElement(By.xpath("//div[2]/div/div/section/div/div[1]/div[2]/a"))

  private def patronButton = driver.findElement(By.xpath("//section/div/div[3]/div[2]/a"))

  private def partnerButton = driver.findElement(By.xpath("//section/div/div[2]/div[2]/a"))

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
