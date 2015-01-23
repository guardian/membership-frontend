package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 30/05/2014.
 */
class ThankYouPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def packageCell = driver.findElement(By.id("qa-joiner-summary-tier"))

  private def startDateCell = driver.findElement(By.id("qa-joiner-summary-start"))

  private def amountPaidTodayCell = driver.findElement(By.id("qa-joiner-summary-today"))

  private def monthlyPaymentCell = driver.findElement(By.id("qa-joiner-summary-recurring"))

  private def nextPaymentCell = driver.findElement(By.id("qa-joiner-summary-next"))

  private def cardNumberCell = driver.findElement(By.id("qa-joiner-summary-card"))

  private def getStartedButton = driver.findElement(By.cssSelector(".action"))

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
