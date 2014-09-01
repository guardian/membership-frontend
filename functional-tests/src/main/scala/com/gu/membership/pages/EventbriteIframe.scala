package com.gu.membership.pages

import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 20/08/2014.
 */
class EventbriteIframe(driver: WebDriver) extends BasePage(driver) {

  private def bookNowButton = driver.findElement(By.cssSelector(".js-checkout-button"))

  private def ticketTable = driver.findElements(By.xpath("id('ticket_table')/tbody/tr[@class='ticket_row']/td"))

  private def eventIframe = driver.findElement(By.xpath("//iframe"))


  def isIframeLoaded = {
    ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.xpath("//iframe"))
    driver.switchTo().frame(eventIframe)
    val loaded = !ticketTable.isEmpty
    driver.switchTo().parentFrame()
    loaded
  }

  private def getTicketCount = {
    ticketTable.size()
  }
}
