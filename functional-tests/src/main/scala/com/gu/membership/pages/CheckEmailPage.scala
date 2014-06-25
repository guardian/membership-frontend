package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 16/06/2014.
 */
class CheckEmailPage(driver: WebDriver) extends BasePage(driver)  {

  private def completeRegistrationButton = driver.findElement(By.cssSelector(".submit-input"))

  def clickCompleteRegistration: EventBritePage = {
    completeRegistrationButton.click()
    new EventBritePage(driver)
  }
}
