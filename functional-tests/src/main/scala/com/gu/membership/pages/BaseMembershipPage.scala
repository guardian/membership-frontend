package com.gu.membership.pages

import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 07/07/2014.
 */
class BaseMembershipPage(driver: WebDriver) extends BasePage(driver) {

  private def accountControlDiv = driver.findElement(By.cssSelector(".identity__account"))
  private def editProfileLink = driver.findElement(By.xpath("//a[contains(text(), 'Edit profile')]"))
  private def pricingLink = driver.findElement(By.xpath("//header/div[2]/nav/div/ul/li[5]/a"))

  def clickAccountControl = {
    accountControlDiv.click
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
}
