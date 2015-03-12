package com.gu.membership.pages

import java.util

import org.openqa.selenium.{JavascriptExecutor, By, WebDriver, WebElement}

class EventsListPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def eventLinkList: util.List[WebElement] = driver.findElements(By.cssSelector(".qa-available-event-item"))

  private def eventsTitleList: util.List[WebElement] = driver.findElements(By.cssSelector(".qa-event-item-title"))

  def getEventTitleByIndex(index: Int): String = eventsTitleList.get(index).getText

  def getEventsListSize: Int = eventLinkList.size()

  def clickEventByIndex(index: Int): EventPage = {
    val event = eventLinkList.get(index)
    val y = event.getLocation
    driver.asInstanceOf[JavascriptExecutor].executeScript(s"window.scrollTo(0, $y)")
    event.click
    new EventPage(driver)
  }

  def clickAnEvent(): EventPage = {
    clickEventByIndex(3)
  }
}
