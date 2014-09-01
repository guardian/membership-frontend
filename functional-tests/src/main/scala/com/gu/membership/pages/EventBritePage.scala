package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

class EventBritePage(driver: WebDriver) extends BasePage(driver) {

  private def eventNameSpan = driver.findElement(By.cssSelector(".summary"))

  private def descriptionSpan = driver.findElement(By.cssSelector(".description"))

  def getEventName = eventNameSpan.getText

  def getEventDescription = descriptionSpan.getText

  def isPageLoaded = descriptionSpan.isDisplayed && driver.getCurrentUrl.contains("eventbrite")
}
