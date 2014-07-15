package com.gu.membership.pages

import org.openqa.selenium.WebDriver

class PaymentPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  val cardWidget = new CreditCardWidget(driver)
}
