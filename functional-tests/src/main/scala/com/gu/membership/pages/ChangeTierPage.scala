package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 08/07/2014.
 */
class ChangeTierPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def becomeAFriendButton = driver.findElement(By.xpath("//div[2]/div/div/main/div[2]/div[1]/section/div[1]/div/a"))

  private def becomeAPartnerButton = driver.findElement(By.xpath("//main/div[2]/div[1]/section/div[2]/div/div/a"))

  private def becomeAPatronButton = driver.findElement(By.xpath("//main/div[2]/div[1]/section/div[3]/div/div/a"))

  private def cancelLink = driver.findElement(By.xpath("//div[1]/main/div[2]/div[2]/ul/li[5]/a"))

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
}
