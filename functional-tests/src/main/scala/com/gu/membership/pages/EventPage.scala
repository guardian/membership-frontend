package com.gu.membership.pages

import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.{By, WebDriver}

class EventPage(driver: WebDriver) extends BasePage(driver) {

  private def eventLocationP = driver.findElement(By.cssSelector(".event__location"))

  private def eventTimeP = driver.findElement(By.cssSelector(".event__time"))

  private def eventSalesEndSpan = driver.findElement(By.cssSelector(".event__sale_ends_time"))

  private def eventPriceSpan = driver.findElement(By.cssSelector(".event__price"))

  private def discountedEventPriceSpan = driver.findElement(By.cssSelector(".event__price-amount"))

  private def eventDescriptionDiv = driver.findElement(By.cssSelector(".event__description"))

  private def buyButton = driver.findElement(By.xpath("//div[@class[contains(., 'event__content')]]/a[1]"))

  private def eventName = driver.findElement(By.cssSelector(".event__name"))

  private def originalPriceSpan = driver.findElement(By.cssSelector(".event__price-amount"))

  def getEventLocation: String = eventLocationP.getText

  def getEventSalesEndTime: String = eventSalesEndSpan.getText

  def getEventTime: String = eventTimeP.getText

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
