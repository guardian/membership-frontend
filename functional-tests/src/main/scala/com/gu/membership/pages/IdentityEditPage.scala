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

  private def startDateSpan = driver.findElement(By.cssSelector(".js-membership-join-date"))

  private def paymentCostSpan = driver.findElement(By.cssSelector(".js-membership-payment-cost"))

  private def nextPaymentSpan = driver.findElement(By.cssSelector(".js-membership-payment-next"))

  private def cardDetailsSpan = driver.findElement(By.cssSelector(".membership-tab__card-details"))

  private def changeCardButton = driver.findElement(By.cssSelector(".submit-input.js-membership-change-cc-open"))

  private def successFlashMessage = driver.findElement(By.cssSelector(".form__success"))

  private def changeTierButton = driver.findElement(By.xpath("id('tabs-account-profile-3')/div/ul[1]/li[1]/div[2]/div/a"))

  val cardWidget = new CreditCardWidget(driver)

  def clickChangebutton = {
    new WebDriverWait(driver, 25).until(ExpectedConditions.visibilityOf(changeCardButton))
    // FIXME: for some reason the JS doesn't work if you click immediately
    new Actions(driver).moveToElement(changeCardButton).perform
    Thread.sleep(1000)
    changeCardButton.click()
    cardWidget
  }

  def clickMembershipTab = {
    new WebDriverWait(driver, 35).until(ExpectedConditions.visibilityOf(membershipTab))
    membershipTab.click()
    this
  }

  def isMembershipTabVisible = {
    try {
      membershipTab.isDisplayed
    } catch {
      case e: Exception => false
    }
  }

  def clickChangeTier = {
    changeTierButton.click
    new ChangeTierPage(driver)
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
