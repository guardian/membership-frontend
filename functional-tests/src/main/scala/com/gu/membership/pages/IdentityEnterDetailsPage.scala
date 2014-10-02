package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}
import org.openqa.selenium.support.ui.Select

/**
 * Created by jao on 30/09/2014.
 */
class IdentityEnterDetailsPage(driver: WebDriver) extends BasePage(driver) {

  private def firstNameTextbox = driver.findElement(By.id("firstName"))

  private def lastNameTextbox = driver.findElement(By.id("secondName"))

  private def address1Textbox = driver.findElement(By.id("address_line1"))

  private def address2Textbox = driver.findElement(By.id("address_line2"))

  private def townTextbox = driver.findElement(By.id("address_line3"))

  private def stateTextbox = driver.findElement(By.id("address_line4"))

  private def postCodeTextbox = driver.findElement(By.id("address_postcode"))

  private def countryDropdown = driver.findElement(By.id("address_country"))

  private def saveButton = driver.findElement(By.cssSelector("button[data-test-id='save-changes']"))

  def enterName(firstName: String, lastName: String) = {
    firstNameTextbox.clear()
    firstNameTextbox.sendKeys(firstName)
    lastNameTextbox.clear()
    lastNameTextbox.sendKeys(lastName)
    this
  }

  def enterAddress(line1: String, line2: String) = {
    address1Textbox.sendKeys(line1)
    address2Textbox.sendKeys(line2)
    this
  }

  def enterTown(town: String) = {
    townTextbox.sendKeys(town)
    this
  }

  def enterState(state: String) = {
    stateTextbox.sendKeys(state)
    this
  }

  def enterPostcode(postCode: String) = {
    postCodeTextbox.sendKeys(postCode)
    this
  }

  def enterCountry(country: String) = {
    new Select(countryDropdown).selectByValue(country)
    this
  }

  def clickSave = {
    saveButton.click
    this
  }
}
