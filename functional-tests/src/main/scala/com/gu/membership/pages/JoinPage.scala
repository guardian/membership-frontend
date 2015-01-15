package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 29/05/2014.
 */
class JoinPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def becomeAFriendLink = driver.findElement(By.cssSelector("li:nth-of-type(1) .action__label"))

  private def becomeAPartnerLink = driver.findElement(By.cssSelector("li:nth-of-type(2) .action__label"))

  private def becomeAPatronLink = driver.findElement(By.cssSelector("li:nth-of-type(3) .action__label"))

  def clickBecomeAFriend = {
    becomeAFriendLink.click
    new JoinFriendPage(driver)
  }

  def clickBecomeAPatron = {
    becomeAPatronLink.click
    new JoinPatronPage(driver)
  }

  def clickBecomeAPartner = {
    becomeAPartnerLink.click
    new JoinPartnerPage(driver)
  }

  def isPageLoaded = becomeAFriendLink.isDisplayed && becomeAPatronLink.isDisplayed
}
