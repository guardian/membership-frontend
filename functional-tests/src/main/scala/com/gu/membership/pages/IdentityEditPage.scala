package com.gu.membership.pages

import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 17/06/2014.
 */
class IdentityEditPage(driver: WebDriver) extends BasePage(driver) {

  private def membershipTab = driver.findElement(By.cssSelector("#tabs-account-profile-3-tab>a"))

  private def tierSpan = driver.findElement(By.id("qa-membership-tier"))

  private def startDateSpan = driver.findElement(By.id("qa-mem-join-date"))

  private def paymentCostSpan = driver.findElement(By.id("qa-mem-payment-cost"))

  private def nextPaymentSpan = driver.findElement(By.id("qa-mem-current-period-end"))

  private def cardDetailsSpan = driver.findElement(By.id("qa-card-details"))

  private def changeCardButton = driver.findElement(By.id("qa-change-card"))

  private def successFlashMessage = driver.findElement(By.id("qa-success-message"))

  private def changeTierButton = driver.findElement(By.id("qa-change-tier"))

  private def cancelledMembershipH2 = driver.findElement(By.id("qa-cancelled-membership"))

  private def accountDetailsTab = driver.findElement(By.cssSelector("#tabs-account-profile-2-tab>a"))

  private def upsellDiv = driver.findElement(By.cssSelector(".membership-up-sell"))

  val cardWidget = new CreditCardWidget(driver)

  def clickChangeButton = {
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

  def isAnUpsell = {
    try {
      membershipTab.isDisplayed
    } catch {
      case e: Exception => false
    }
  }

  def clickChangeTier = {
    new WebDriverWait(driver, 25).until(ExpectedConditions.visibilityOf(changeTierButton))
    changeTierButton.click
    new ChangeTierPage(driver)
  }

  def getMembershipTier: String = {
    new WebDriverWait(driver, 25).until(ExpectedConditions.visibilityOf(changeTierButton))
    tierSpan.getText
  }

  def getStartDate: String = startDateSpan.getText

  def getPaymentCost: String = paymentCostSpan.getText

  def getNextPaymentDate: String = nextPaymentSpan.getText

  def getCardDetails: String = cardDetailsSpan.getText

  def isSuccessFlashMessagePresent: Boolean = {
    new WebDriverWait(driver, 25).until(ExpectedConditions.visibilityOf(successFlashMessage))
    successFlashMessage.isDisplayed
  }

  def clickAccountDetailsTab = {
    accountDetailsTab.click
    new IdentityEnterDetailsPage(driver)
  }

  def isMembershipCancelled = {
    new WebDriverWait(driver, 35).until(ExpectedConditions.visibilityOf(cancelledMembershipH2))
    cancelledMembershipH2.isDisplayed
  }
}
