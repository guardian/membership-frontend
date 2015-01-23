package com.gu.membership.pages

import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 07/07/2014.
 */
class BaseMembershipPage(driver: WebDriver) extends BasePage(driver) {

  private def accountControlLink = driver.findElement(By.id("qa-identity-control"))

  private def editProfileLink = driver.findElement(By.id("qa-identity-nav-edit"))

  private def pricingLink = driver.findElement(By.id("qa-nav-pricing"))

  private def eventsButton = driver.findElement(By.id("qa-nav-events"))

  private def logoLink = driver.findElement(By.id("qa-header-logo"))

  def clickLogo = {
    logoLink.click
    new LandingPage(driver)
  }

  def clickAccountControl = {
    accountControlLink.click
    new WebDriverWait(driver, 25).until(ExpectedConditions.visibilityOf(editProfileLink))
    this
  }

  def clickEditProfile = {
    editProfileLink.click
    new IdentityEditPage(driver)
  }

  def clickPricing = {
    pricingLink.click
    new JoinFlowChooseTierPage(driver)
  }

  def clickEventsButton: EventsListPage = {
    eventsButton.click
    new EventsListPage(driver)
  }
}
