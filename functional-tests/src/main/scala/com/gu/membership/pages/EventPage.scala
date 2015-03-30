package com.gu.membership.pages

import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.{By, WebDriver}

class EventPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def eventLocationSpan = driver.findElement(By.cssSelector(".qa-event-detail-location"))

  private def eventTimeDiv = driver.findElement(By.cssSelector(".qa-event-detail-datetime"))

  private def eventSalesEndSpan = driver.findElement(By.cssSelector(".qa-event-detail-sales-end"))

  private def eventPriceSpan = driver.findElements(By.cssSelector(".qa-event-detail-price")).head

  private def discountedEventPriceSpan = driver.findElements(By.cssSelector(".qa-event-detail-price-discount")).head

  private def eventDescriptionDiv = driver.findElement(By.cssSelector(".qa-event-detail-description"))

  private def buyButton = driver.findElement(By.cssSelector(".qa-event-detail-booking-action"))

  private def eventName = driver.findElement(By.cssSelector(".qa-event-detail-name"))

  // TODO: Remove this version, duplicated by eventPriceSpan
  private def originalPriceSpan = driver.findElements(By.cssSelector(".qa-event-detail-price")).head

  def getEventLocation: String = eventLocationSpan.getText

  def getEventSalesEndTime: String = eventSalesEndSpan.getText

  def getEventTime: String = eventTimeDiv.getText

  def getEventPrice: String = eventPriceSpan.getText

  def getEventDescription: String = eventDescriptionDiv.getText

  def getDiscountedEvent: String = discountedEventPriceSpan.getText

  def getOriginalPrice: String = originalPriceSpan.getText

  def clickSignInButton: LoginPage = {
    clickConversionButton
    new LoginPage(driver)
  }

  def clickBuyButton: EventBritePage = {
    clickConversionButton
    new EventBritePage(driver)
  }

  private def clickConversionButton {
    new Actions(driver).moveToElement(buyButton).perform()
    buyButton.click()
  }

  def getEventName: String = eventName.getText
}
