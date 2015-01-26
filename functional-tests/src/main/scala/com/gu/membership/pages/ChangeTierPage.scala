package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 08/07/2014.
 */
class ChangeTierPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def becomeAFriendButton = driver.findElement(By.id("qa-change-tier-friend"))

  private def becomeAPartnerButton = driver.findElement(By.id("qa-change-tier-partner"))

  private def becomeAPatronButton = driver.findElement(By.id("qa-change-tier-patron"))

  private def cancelLink = driver.findElement(By.id("qa-cancel-membership"))

  def clickBecomeAPartner = {
    becomeAPartnerButton.click()
    new UpgradePage(driver)
  }

  def clickBecomeAFriend = {
    becomeAFriendButton.click()
    new AreYouSurePage(driver)
  }

  def clickBecomeAPatron = {
   becomeAPatronButton.click()
    new UpgradePage(driver)
  }

  def clickCancelLink = {
    cancelLink.click()
    new CancelPage(driver)
  }

  def isPageLoaded = cancelLink.isDisplayed
}
