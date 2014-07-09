package com.gu.membership.pages

import java.util

import org.openqa.selenium.{By, WebDriver, WebElement}

class EventsListPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def eventsImageList: util.List[WebElement] = driver.findElements(By.cssSelector(".item__image-container>img"))

  private def eventsTitleList: util.List[WebElement] = driver.findElements(By.cssSelector(".item__title"))

  private def eventsLocationList: util.List[WebElement] = driver.findElements(By.cssSelector(".event__location"))

  private def eventsTimeList: util.List[WebElement] = driver.findElements(By.cssSelector(".event__time"))

  def getEventTitleByIndex(index: Int): String = eventsTitleList.get(index).getText

  def getEventsListSize: Int = eventsImageList.size()

  def getEventLocationByIndex(index: Int): String = eventsLocationList.get(index).getText

  def getEventTimeByIndex(index: Int): String = eventsTimeList.get(index).getText

  def clickEventByIndex(index: Int): EventPage = {
    eventsImageList.get(index).click
    new EventPage(driver)
  }

  def clickLastEvent(): EventPage = {
    clickEventByIndex(getEventsListSize - 1)
  }
}
