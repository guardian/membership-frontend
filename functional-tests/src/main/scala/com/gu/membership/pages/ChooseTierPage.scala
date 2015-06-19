package com.gu.membership.pages

import org.openqa.selenium.{JavascriptExecutor, By, WebDriver}

/**
 * Created by jao on 11/08/2014.
 */
class ChooseTierPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def friendInput = driver.findElement(By.cssSelector(".qa-friend-join"))

  private def partnerInput = driver.findElement(By.id("qa-partner"))

  private def patronInput = driver.findElement(By.id("qa-patron"))

  private def chooseButton = driver.findElement(By.cssSelector(".action"))

  def clickFriend = {
    friendInput.click()
    this
  }

  def clickPartner = {
    val y = partnerInput.getLocation
    driver.asInstanceOf[JavascriptExecutor].executeScript(s"window.scrollTo(0, $y)")
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
