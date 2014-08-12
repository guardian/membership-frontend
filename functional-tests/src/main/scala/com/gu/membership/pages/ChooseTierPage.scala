package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 11/08/2014.
 */
class ChooseTierPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def friendInput = driver.findElement(By.id("friend"))

  private def partnerInput = driver.findElement(By.id("partner"))

  private def patronInput = driver.findElement(By.id("patron"))

  private def chooseButton = driver.findElement(By.cssSelector(".submit-input.action--logged-in"))

  def clickFriend = {
    friendInput.click()
    this
  }

  def clickPartner = {
    partnerInput.click()
    this
  }

  def clickPatron = {
    patronInput.click()
    this
  }

  def clickChoose = {
    chooseButton.click()
    new JoinPatronPage(driver)
  }

  def isPageLoaded = {
    friendInput.isDisplayed && partnerInput.isDisplayed && patronInput.isDisplayed && chooseButton.isDisplayed
  }
}
