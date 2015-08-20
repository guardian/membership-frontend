package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

class PatronPage(driver: WebDriver) {

  private def joinButton = driver.findElement(By.cssSelector(".qa-package-patron"))

  def clickJoinButton: PaymentPage = {
    joinButton.click()
    new PaymentPage(driver)
  }

}
