package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 08/07/2014.
 */
class DowngradeConfirmationPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def profileButton = driver.findElement(By.id("qa-downgrade-profile-link"))

  private def currentPackageTd = driver.findElement(By.id("qa-tier-summary-tier"))

  private def newPackageTd = driver.findElement(By.id("qa-downgrade-summary-tier"))

  private def endDateTd = driver.findElement(By.id("qa-tier-summary-end"))

  private def startDateTd = driver.findElement(By.id("qa-downgrade-summary-start"))

  def clickProfileButton = {
    profileButton.click
    new IdentityEditPage(driver)
  }

  def getCurrentPackage = currentPackageTd.getText

  def getNewPackage = newPackageTd.getText

  def getEndDate = endDateTd.getText

  def getStartDate = startDateTd.getText
}
