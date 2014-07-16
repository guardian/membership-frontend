package com.gu.membership.pages

import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait, Select}
import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 20/06/2014.
 */
class CreditCardWidget(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def cardNumberTextbox = driver.findElement(By.id("cc-num"))

  private def cardSecurityCodeTextbox = driver.findElement(By.id("cc-cvc"))

  private def cardExpirationMonthDropdown = driver.findElement(By.id("cc-exp-month"))

  private def cardExpirationYearDropdown = driver.findElement(By.id("cc-exp-year"))

  private def submitPaymentButton = driver.findElement(By.cssSelector(".submit-input"))

  private def updateCCButton = driver.findElement(By.cssSelector(".submit-input.js-membership-change-cc-submit"))

  private def errorMessage = driver.findElement(By.cssSelector(".form__error"))

  def enterCardNumber(number: String) = {
    new WebDriverWait(driver, 25).until(ExpectedConditions.visibilityOf(cardNumberTextbox))
    cardNumberTextbox.sendKeys(number)
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

  def submitPayment(cardNumber: String, cardSecurityCode: String, cardExpirationMonth: String,
                    cardExpirationYear: String) =
    enterCardNumber(cardNumber).enterCardSecurityCode(cardSecurityCode)
      .enterCardExpirationMonth(cardExpirationMonth).enterCardExpirationYear(cardExpirationYear).clickSubmitPayment
}
