package com.gu.membership.pages

import org.openqa.selenium.{JavascriptExecutor, By, WebDriver}

/**
 * Created by jao on 12/02/15.
 */
class UpgradeToPatronPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def upgradeButton = driver.findElement(By.id("qa-upgrade-submit"))

  private def oldTierTd = driver.findElement(By.id("qa-upgrade-current-tier"))

  private def oldPaymentTd = driver.findElement(By.id("qa-upgrade-current-payment"))

  private def oldLastPaymentDate = driver.findElement(By.id("qa-upgrade-current-last-date"))

  private def oldTierEndDate = driver.findElement(By.id("qa-upgrade-current-end-date"))

  private def newTierTd = driver.findElement(By.id("qa-upgrade-new-tier"))

  private def newPaymentTd = driver.findElement(By.id("qa-upgrade-new-recurring-payment"))

  private def firstPaymentTd = driver.findElement(By.id("qa-upgrade-new-first-payment"))

  private def nextPaymentTd = driver.findElement(By.id("qa-upgrade-new-date"))

  def clickUpgrade = {
    val y = upgradeButton.getLocation
    driver.asInstanceOf[JavascriptExecutor].executeScript(s"window.scrollTo(0, $y)")
    upgradeButton.click
    new ThankYouPage(driver)
  }

  def getOldTier = oldTierTd.getText

  def getOldPaymentAmont = oldPaymentTd.getText

  def getOldLastPaymentDate = oldLastPaymentDate.getText

  def getOldTierEndDate = oldTierEndDate.getText

  def getNewTier = newTierTd.getText

  def getNewPaymentAmount = newPaymentTd.getText

  def getFirstPayment = firstPaymentTd.getText

  def getNextPayment = nextPaymentTd.getText

}
