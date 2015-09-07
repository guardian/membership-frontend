package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}


class LandingPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def title = driver.findElement(By.cssSelector("h1"))

  private def becomeAFriendLink = driver.findElement(By.cssSelector(".qa-friend-join"))
  private def becomeASupporterLink = driver.findElement(By.cssSelector(".qa-package-supporter"))
  private def becomeAPartnerLink = driver.findElement(By.cssSelector(".qa-package-partner"))
  private def learnPatronLink = driver.findElement(By.id("qa-learn-patron"))


  def clickBecomeAFriend = {
    becomeAFriendLink.click()
    new JoinFriendPage(driver)
  }

 def clickBecomeASupporter = {
   becomeASupporterLink.click
   new JoinPatronPage(driver)
 }

  def clickBecomeAPartner = {
    becomeAPartnerLink.click()
    new JoinPartnerPage(driver)
  }

  def clickLearnPatron = {
    learnPatronLink.click()
    new PatronPage(driver)
  }

  def getTitle() = title.getText

  def isPageLoaded = becomeAFriendLink.isDisplayed && becomeAPartnerLink.isDisplayed

}
