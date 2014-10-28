package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 29/05/2014.
 */
class JoinPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def becomeAFriendLink = driver.findElement(By.cssSelector(".package:nth-of-type(1) .action"))

  private def becomeAPartnerLink = driver.findElement(By.cssSelector(".package:nth-of-type(2) .action"))

  private def becomeAPatronLink = driver.findElement(By.cssSelector(".package:nth-of-type(3) .action"))

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
}
