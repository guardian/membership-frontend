package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 19/03/15.
 */
class UnavailableTierPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def header = driver.findElement(By.cssSelector("h1"))

  def getHeader = header.getText
}
