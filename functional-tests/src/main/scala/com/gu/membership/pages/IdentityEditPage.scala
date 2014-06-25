package com.gu.membership.pages

import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 17/06/2014.
 */
class IdentityEditPage(driver: WebDriver) extends BasePage(driver) {

  private def membershipTab = driver.findElement(By.cssSelector("#tabs-account-profile-3-tab>a"))

  private def tierSpan = driver.findElement(By.cssSelector(".js-membership-tier"))

  private def startDateSpan = driver.findElement(By.cssSelector(".js-membership-start-date"))

  private def paymentCostSpan = driver.findElement(By.cssSelector(".js-membership-payment-cost"))

  private def nextPaymentSpan = driver.findElement(By.cssSelector(".js-membership-payment-next"))

  private def cardDetailsSpan = driver.findElement(By.cssSelector(".membership-tab__card-details"))

  private def changeButton = driver.findElement(By.cssSelector(".submit-input.js-membership-change-cc-open"))

  private def successFlashMessage = driver.findElement(By.cssSelector(".form__success"))

  val cardWidget = new CreditCardWidget(driver)

  def clickChangebutton: CreditCardWidget = {
    new WebDriverWait(driver, 25).until(ExpectedConditions.visibilityOf(changeButton))
    // FIXME: for some reason the JS doesn't work if you click immediately
    new Actions(driver).moveToElement(changeButton).perform
    Thread.sleep(1000)
    changeButton.click()
    cardWidget
  }

  def clickMembershipTab: IdentityEditPage = {
    new WebDriverWait(driver, 35).until(ExpectedConditions.visibilityOf(membershipTab))
    membershipTab.click()
    this
  }

  def isMembershipTabVisible: Boolean = {
    try {
      membershipTab.isDisplayed
    } catch {
      case e: Exception => false
    }
  }

  def getMembershipTier: String = tierSpan.getText

  def getStartDate: String = startDateSpan.getText

  def getPaymentCost: String = paymentCostSpan.getText

  def getNextPaymentDate: String = nextPaymentSpan.getText

  def getCardDetails: String = cardDetailsSpan.getText

  def isSuccessFlashMessagePresent: Boolean = {
    new WebDriverWait(driver, 25).until(ExpectedConditions.visibilityOf(successFlashMessage))
    successFlashMessage.isDisplayed
  }
}
