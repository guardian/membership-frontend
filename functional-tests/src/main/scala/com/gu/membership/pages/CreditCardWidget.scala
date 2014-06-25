package com.gu.membership.pages

import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait, Select}
import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 20/06/2014.
 */
class CreditCardWidget(driver: WebDriver) extends BasePage(driver) {

  private def cardNumberTextbox = driver.findElement(By.id("cc-num"))

  private def cardSecurityCodeTextbox = driver.findElement(By.id("cc-cvc"))

  private def cardExpirationMonthDropdown = driver.findElement(By.id("cc-exp-month"))

  private def cardExpirationYearDropdown = driver.findElement(By.id("cc-exp-year"))

  private def submitPaymentButton = driver.findElement(By.cssSelector(".submit-input"))

  private def updateCCButton = driver.findElement(By.cssSelector(".submit-input.js-membership-change-cc-submit"))

  private def errorMessage = driver.findElement(By.cssSelector(".form__error"))

  def enterCardNumber(number: String): CreditCardWidget = {
    new WebDriverWait(driver, 25).until(ExpectedConditions.visibilityOf(cardNumberTextbox))
    cardNumberTextbox.sendKeys(number)
    this
  }

  def enterCardSecurityCode(code: String): CreditCardWidget = {
    cardSecurityCodeTextbox.sendKeys(code)
    this
  }

  def enterCardExpirationMonth(month: String): CreditCardWidget = {
    new Select(cardExpirationMonthDropdown).selectByValue(month)
    this
  }

  def enterCardExpirationYear(year: String): CreditCardWidget = {
    new Select(cardExpirationYearDropdown).selectByValue(year)
    this
  }

  def focusOnCvc: CreditCardWidget = {
    cardSecurityCodeTextbox.click
    this
  }

  def getErrorMessage: String = {
    new WebDriverWait(driver, 25).until(ExpectedConditions.visibilityOf(errorMessage))
    errorMessage.getText
  }

  def clickUpdateCardDetails: CreditCardWidget = {
    updateCCButton.click()
    this
  }

  def clickSubmitPayment: ThankYouPage = {
    submitPaymentButton.click
    new ThankYouPage(driver)
  }

  def submitPayment(cardNumber: String, cardSecurityCode: String, cardExpirationMonth: String,
                    cardExpirationYear: String): ThankYouPage =
    enterCardNumber(cardNumber).enterCardSecurityCode(cardSecurityCode)
      .enterCardExpirationMonth(cardExpirationMonth).enterCardExpirationYear(cardExpirationYear).clickSubmitPayment
}
