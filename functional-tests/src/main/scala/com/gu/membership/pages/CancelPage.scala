package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 12/08/2014.
 */
class CancelPage(driver: WebDriver) extends BasePage(driver) {

  private def confirmCancelLink = driver.findElement(By.id("qa-confirm-cancel"))

  def clickConfirmCancellation = {
    confirmCancelLink.click()
    new CancelConfirmationPage(driver)
  }
}
