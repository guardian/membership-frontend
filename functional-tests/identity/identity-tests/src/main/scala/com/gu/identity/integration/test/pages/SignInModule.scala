package com.gu.identity.integration.test.pages

import com.gu.integration.test.pages.common.ParentPage
import com.gu.integration.test.util.ElementLoader._
import org.openqa.selenium.{WebDriver, WebElement, By}

/**
 * Do not confuse this with the sign in page. This is only the module which sits at the top of most frontend pages
 */
class SignInModule(implicit driver: WebDriver) extends ParentPage {
  private val signInLink: WebElement = driver.findElement(By.cssSelector(".identity__notice.control__info"))
  def signInName: WebElement = driver.findElement(By.cssSelector(".js-user-displayname"))

  def clickSignInLink(): SignInPage = {
    signInLink.click()
    new SignInPage()
  }

  def clickSignInLinkWhenLoggedIn(): ProfileNavMenu = {
    signInLink.click()
    new ProfileNavMenu
  }
}