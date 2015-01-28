package com.gu.membership.pages

import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.{By, WebDriver}

class EventPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def eventLocationSpan = driver.findElement(By.cssSelector(".stat-item__second.copy"))

  private def eventTimeDiv = driver.findElement(By.cssSelector(".stat-item__second>span"))

  private def eventSalesEndSpan = driver.findElement(By.xpath("//*[contains(., \"Sale ends\")]/time"))

  private def eventPriceSpan = driver.findElement(By.cssSelector(".price-info-inline__value.js-event-price-value"))

  private def discountedEventPriceSpan = driver.findElement(By.cssSelector(".event-ticket__trail-tag.js-event-price-discount"))

  private def eventDescriptionDiv = driver.findElement(By.cssSelector(".event__description.copy"))

  private def buyButton = driver.findElement(By.cssSelector(".js-ticket-cta.action.action--booking"))

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
