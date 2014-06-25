package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 30/05/2014.
 */
class ThankYouPage(driver: WebDriver) extends BasePage(driver) {

  private def packageCell = driver.findElement(By.xpath("//body/div[1]/section[2]/table/tbody/tr[1]/td[2]"))

  private def startDateCell = driver.findElement(By.xpath("//body/div[1]/section[2]/table/tbody/tr[2]/td[2]"))

  private def amountPaidTodayCell = driver.findElement(By.xpath("//body/div[1]/section[2]/table/tbody/tr[3]/td[2]"))

  private def monthlyPaymentCell = driver.findElement(By.xpath("//body/div[1]/section[2]/table/tbody/tr[4]/td[2]"))

  private def nextPaymentCell = driver.findElement(By.xpath("//body/div[1]/section[2]/table/tbody/tr[5]/td[2]"))

  private def getStartedButton = driver.findElement(By.cssSelector(".action"))

  private def cardNumberCell = driver.findElement(By.xpath("//td[@class='summary-table__cell']/span[2]"))

  def getPackage: String = packageCell.getText

  def getStartDate: String = startDateCell.getText

  def getAmountPaidToday: String = amountPaidTodayCell.getText

  def getMonthlyPayment: String = monthlyPaymentCell.getText

  def getNextPaymentDate: String = nextPaymentCell.getText

  def getCardNumber = cardNumberCell.getText

  def clickGetStarted: EventsListPage = {
    getStartedButton.click
    new EventsListPage(driver)
  }
}
