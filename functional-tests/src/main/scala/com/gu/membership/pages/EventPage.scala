package com.gu.membership.pages

import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.{By, WebDriver}

class EventPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def eventLocationSpan = driver.findElement(By.cssSelector(".event-content__venue-name"))

  private def eventTimeDiv = driver.findElement(By.cssSelector(".event-content__time"))

  private def eventSalesEndSpan = driver.findElement(By.cssSelector(".event-content__sale-ends>time"))

  private def eventPriceSpan = driver.findElement(By.cssSelector(".event-ticket__price-amount"))

  private def discountedEventPriceSpan = driver.findElement(By.cssSelector(".event-ticket__trail-upsell"))

  private def eventDescriptionDiv = driver.findElement(By.cssSelector(".event-content__body"))

  private def buyButton = driver.findElement(By.cssSelector(".event-ticket>.action"))

  private def eventName = driver.findElement(By.cssSelector(".event-masthead__name"))

  private def originalPriceSpan = driver.findElement(By.cssSelector(".event-ticket__price-amount"))

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
