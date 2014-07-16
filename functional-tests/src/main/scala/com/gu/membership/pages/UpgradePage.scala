package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 11/07/2014.
 */
class UpgradePage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def streetTextbox = driver.findElement(By.id("street"))

  private def cityTextbox = driver.findElement(By.id("city"))

  private def postCodeTextbox = driver.findElement(By.id("postCode"))

  private def submitButton = driver.findElement(By.cssSelector(".js-submit-input"))

  val creditCard = new CreditCardWidget(driver)

  def enterStreet(street: String) = {
    streetTextbox.sendKeys(street)
    this
  }

  def enterCity(city: String) = {
    cityTextbox.sendKeys(city)
    this
  }

  def enterPostCode(postCode: String) = {
    postCodeTextbox.sendKeys(postCode)
    this
  }

  def clickSubmit = {
    submitButton.click
    new ThankYouPage(driver)
  }
}
