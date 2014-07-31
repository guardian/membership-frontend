package com.gu.membership.pages

import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait, Select}
import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 20/06/2014.
 */
class CreditCardWidget(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def firstNameTextbox = driver.findElement(By.id("name-first"))

  private def lastNameTextbox = driver.findElement(By.id("name-last"))

  private def address1Textbox = driver.findElement(By.id("address-line-one"))

  private def address2Textbox = driver.findElement(By.id("address-line-two"))

  private def countyTextbox = driver.findElement(By.id("county-or-state"))

  private def postCodeTextbox = driver.findElement(By.id("postCode"))

  private def townTextbox = driver.findElement(By.id("town"))

  private def cardNumberTextbox = driver.findElement(By.id("cc-num"))

  private def cardSecurityCodeTextbox = driver.findElement(By.id("cc-cvc"))

  private def cardExpirationMonthDropdown = driver.findElement(By.id("cc-exp-month"))

  private def cardExpirationYearDropdown = driver.findElement(By.id("cc-exp-year"))

  private def submitPaymentButton = driver.findElement(By.cssSelector(".js-submit-input"))

  private def updateCCButton = driver.findElement(By.cssSelector(".submit-input.js-membership-change-cc-submit"))

  private def errorMessage = driver.findElement(By.cssSelector(".form__error"))

  def enterCardNumber(number: String) = {
    new WebDriverWait(driver, 25).until(ExpectedConditions.visibilityOf(cardNumberTextbox))
    cardNumberTextbox.sendKeys(number)
    this
  }

  def enterAddressLineTwo(address: String) = {
    address2Textbox.sendKeys(address)
    this
  }

  def enterCountry(country: String) = {
    countyTextbox.sendKeys(country)
    this
  }

  def enterFirstName(name: String) = {
    firstNameTextbox.sendKeys(name)
    this
  }

  def enterAddressLineOne(address: String) = {
    address1Textbox.sendKeys(address)
    this
  }

  def enterTown(town: String) = {
    townTextbox.sendKeys(town)
    this
  }

  def enterPostCode(postCode: String) = {
    postCodeTextbox.sendKeys(postCode)
    this
  }

  def enterLastName(surname: String) = {
    lastNameTextbox.sendKeys(surname)
    this
  }

  def enterCardSecurityCode(code: String) = {
    cardSecurityCodeTextbox.sendKeys(code)
    this
  }

  def enterCardExpirationMonth(month: String) = {
    new Select(cardExpirationMonthDropdown).selectByValue(month)
    this
  }

  def enterCardExpirationYear(year: String) = {
    new Select(cardExpirationYearDropdown).selectByValue(year)
    this
  }

  def focusOnCvc = {
    cardSecurityCodeTextbox.click
    this
  }

  def getErrorMessage = {
    new WebDriverWait(driver, 25).until(ExpectedConditions.visibilityOf(errorMessage))
    errorMessage.getText
  }

  def clickUpdateCardDetails = {
    updateCCButton.click()
    this
  }

  def clickSubmitPayment = {
    submitPaymentButton.click
    new ThankYouPage(driver)
  }

  def submitPayment(firstName: String, lastName: String, address1: String, address2: String, country: String,
                    town: String, postCode: String, cardNumber: String, cardSecurityCode: String,
                    cardExpirationMonth: String, cardExpirationYear: String): ThankYouPage = {
    enterFirstName(firstName).enterLastName(lastName)
    submitPayment(address1, address2, country, town, postCode, cardNumber, cardSecurityCode, cardExpirationMonth,
      cardExpirationYear)
  }

  def submitPayment(address1: String, address2: String, country: String, town: String, postCode: String,
                    cardNumber: String, cardSecurityCode: String, cardExpirationMonth: String,
                    cardExpirationYear: String): ThankYouPage = enterAddressLineOne(address1)
      .enterAddressLineTwo(address2).enterTown(town).enterCountry(country)
      .enterPostCode(postCode).enterCardNumber(cardNumber).enterCardSecurityCode(cardSecurityCode)
      .enterCardExpirationMonth(cardExpirationMonth).enterCardExpirationYear(cardExpirationYear).clickSubmitPayment
}
