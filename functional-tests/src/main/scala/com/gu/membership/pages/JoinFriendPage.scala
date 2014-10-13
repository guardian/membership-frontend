package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 11/07/2014.
 */
class JoinFriendPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def firstNameTextbox = driver.findElement(By.id("name-first"))

  private def lastNameTextbox = driver.findElement(By.id("name-last"))

  private def postCodeTextbox = driver.findElement(By.id("postCode-deliveryAddress"))

  private def joinNowButton = driver.findElement(By.cssSelector(".action"))

  def enterFirstName(firstName: String) = {
    firstNameTextbox.sendKeys(firstName)
    this
  }

  def enterLastName(lastName: String) = {
    lastNameTextbox.sendKeys(lastName)
    this
  }

  def enterPostCode(postCode: String) = {
    postCodeTextbox.sendKeys(postCode)
    this
  }

  def clickJoinNow = {
    joinNowButton.click()
    new ThankYouPage(driver)
  }
}
