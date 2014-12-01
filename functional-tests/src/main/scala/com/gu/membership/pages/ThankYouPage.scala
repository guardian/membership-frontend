package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 30/05/2014.
 */
class ThankYouPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def packageCell = driver.findElement(By.xpath("//tbody/tr[1]/td"))

  private def startDateCell = driver.findElement(By.xpath("//tbody/tr[2]/td"))

  private def amountPaidTodayCell = driver.findElement(By.xpath("//tbody/tr[3]/td"))

  private def monthlyPaymentCell = driver.findElement(By.xpath("//tbody/tr[4]/td"))

  private def nextPaymentCell = driver.findElement(By.xpath("//tbody/tr[5]/td"))

  private def getStartedButton = driver.findElement(By.cssSelector(".action"))

  private def cardNumberCell = driver.findElement(By.xpath("//tbody/tr[6]/td"))

  def getPackage: String = packageCell.getText

  def getStartDate: String = startDateCell.getText

  def getAmountPaidToday: String = amountPaidTodayCell.getText

  def getPaymentAmount: String = monthlyPaymentCell.getText

  def getNextPaymentDate: String = nextPaymentCell.getText

  def getCardNumber = cardNumberCell.getText

  def clickGetStarted: EventsListPage = {
    getStartedButton.click
    new EventsListPage(driver)
  }
}
