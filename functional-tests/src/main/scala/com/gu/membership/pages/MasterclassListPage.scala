package com.gu.membership.pages

import org.openqa.selenium.{Keys, By, WebDriver}

/**
 * Created by jao on 18/11/14.
 */
class MasterclassListPage(driver: WebDriver) extends EventsListPage(driver) {

  private def searchTextbox = driver.findElement(By.id("js-filter"))

  def search(keyword: String) = {
    searchTextbox.sendKeys(keyword)
    searchTextbox.sendKeys(Keys.ENTER)
    this
  }
}
