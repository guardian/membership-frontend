package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 12/08/2014.
 */
class CancelConfirmationPage(driver: WebDriver) extends BasePage(driver) {

  private def myProfileButton = driver.findElement(By.cssSelector(".action"))

  def clickBackToMyProfile = {
    myProfileButton.click()
    new IdentityEditPage(driver)
  }
}
