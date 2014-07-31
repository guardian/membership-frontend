package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 29/05/2014.
 */
class JoinPartnerPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def joinButton = driver.findElement(By.cssSelector(".action"))

  def clickJoinButton: PaymentPage = {
    joinButton.click
    new PaymentPage(driver)
  }

  def clickJoinFriendButton = {
    joinButton.click
    new CreditCardWidget(driver) // it is the widget without the payment part
  }
}
