package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 08/07/2014.
 */
class AreYouSurePage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def continueButton = driver.findElement(By.cssSelector(".action-cta.action-cta--confirm"))

  def clickContinue = {
    continueButton.click()
    new DowngradeConfirmationPage(driver)
  }
}
