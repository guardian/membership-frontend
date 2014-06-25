package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

class EventBritePage(driver: WebDriver) extends BasePage(driver) {

  private def eventNameSpan = driver.findElement(By.cssSelector(".ticket_type_name"))

  private def descriptionSpan = driver.findElement(By.cssSelector(".description"))

  def getEventName: String = eventNameSpan.getText

  def getEventDescription: String = descriptionSpan.getText

  def isPageLoaded: Boolean = descriptionSpan.isDisplayed && driver.getCurrentUrl.contains("eventbrite")
}
