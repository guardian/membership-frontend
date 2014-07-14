package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 08/07/2014.
 */
class ChangeTierPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def becomeAFriendButton = driver.findElement(By.xpath("//main/div[2]/div[1]/section/div[1]/div/div/div/a"))

  private def becomeAPartnerButton = driver.findElement(By.xpath("//main/div[2]/div[1]/section/div[2]/div/div/div/a"))

  def clickBecomeAPartner = {
    becomeAPartnerButton.click()
    new UpgradePage(driver)
  }

  def clickBecomeAFriend = {
    becomeAFriendButton.click()
    new AreYouSurePage(driver)
  }
}
