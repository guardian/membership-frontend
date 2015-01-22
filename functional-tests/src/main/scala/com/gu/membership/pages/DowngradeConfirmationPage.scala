package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 08/07/2014.
 */
class DowngradeConfirmationPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def profileButton = driver.findElement(By.cssSelector(".action-cta.action-cta--confirm"))

  private def currentPackageTd = driver.findElement(By.xpath(".//*[@id='container']/div/main/div[2]/section/div[2]/table/tbody/tr[1]/td"))

  private def newPackageTd = driver.findElement(By.xpath(".//*[@id='container']/div/main/div[3]/section/div[2]/table/tbody/tr[1]/td"))

  private def endDateTd = driver.findElement(By.xpath(".//*[@id='container']/div/main/div[2]/section/div[2]/table/tbody/tr[4]/td"))

  private def startDateTd = driver.findElement(By.xpath(".//*[@id='container']/div/main/div[3]/section/div[2]/table/tbody/tr[2]/td"))

  def clickProfileButton = {
    profileButton.click
    new IdentityEditPage(driver)
  }

  def getCurrentPackage = currentPackageTd.getText

  def getNewPackage = newPackageTd.getText

  def getEndDate = endDateTd.getText

  def getStartDate = startDateTd.getText
}
