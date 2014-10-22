package com.gu.identity.integration.test.pages

import com.gu.integration.test.pages.common.ParentPage
import com.gu.integration.test.util.ElementLoader._
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{WebDriver, WebElement, By}

/**
 * Do not confuse this with the sign in page. This is only the module which sits at the top of most frontend pages
 */
class SignInModule(implicit driver: WebDriver) extends ParentPage {

  private val signInLink: WebElement = driver.findElement(By.cssSelector(".identity__notice.control__info"))
  private val userIcon: WebElement = driver.findElement(By.cssSelector(".icon-sprite-user-black"))
  private val editProfileLink: WebElement = driver.findElement(By.cssSelector(".js-edit-profile"))
  def signInName = driver.findElement(By.cssSelector(".js-user-displayname"))
  // this has been added here due to frontend having the links hardcoded
  private def changePasswordLink = driver.findElement(By.cssSelector("[data-link-name=\"Change password\"]"))
  private def signOutButton = driver.findElement(By.xpath("//a[contains(., \"Sign out\")]"))

  def clickSignInLink(): SignInPage = {
    signInLink.click()
    new SignInPage()
  }

  def clickSignInLinkWhenLoggedIn(): ProfileNavMenu = {
    clickUserMenu
    editProfileLink.click
    new ProfileNavMenu
  }

  def clickChangePassword: ChangePasswordPage = {
    clickUserMenu
    changePasswordLink.click
    new ChangePasswordPage
  }

  def clickSignOut = {
    clickUserMenu
    signOutButton.click
  }

  private def clickUserMenu = {
    userIcon.click()
    new WebDriverWait(driver, 25).until(ExpectedConditions.visibilityOf(editProfileLink))
  }
}