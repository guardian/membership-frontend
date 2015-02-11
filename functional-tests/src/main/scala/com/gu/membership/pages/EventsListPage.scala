package com.gu.membership.pages

import java.util

import org.openqa.selenium.{JavascriptExecutor, By, WebDriver, WebElement}

class EventsListPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def eventsImageList: util.List[WebElement] = driver.findElements(By.cssSelector(".image-replace"))

  private def eventsTitleList: util.List[WebElement] = driver.findElements(By.cssSelector(".event-item__title>span"))

  private def eventsLocationList: util.List[WebElement] = driver.findElements(By.cssSelector(".event-item__location"))

  private def eventsTimeList: util.List[WebElement] = driver.findElements(By.cssSelector(".event-item__time"))

  def getEventTitleByIndex(index: Int): String = eventsTitleList.get(index).getText

  def getEventsListSize: Int = eventsImageList.size()

  def getEventLocationByIndex(index: Int): String = eventsLocationList.get(index).getText

  def getEventTimeByIndex(index: Int): String = eventsTimeList.get(index).getText

  def clickEventByIndex(index: Int): EventPage = {
    val event = eventsImageList.get(index)
    val y = event.getLocation
    driver.asInstanceOf[JavascriptExecutor].executeScript(s"window.scrollTo(0, $y)")
    event.click
    new EventPage(driver)
  }

  def clickAnEvent(): EventPage = {
    clickEventByIndex(5)
  }
}
