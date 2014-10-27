package com.gu.identity.integration.test.pages

import com.gu.integration.test.pages.common.ParentPage
import com.gu.integration.test.util.ElementLoader._
import org.openqa.selenium.support.ui.ExpectedConditions._
import org.openqa.selenium.{WebDriver, By}

/**
 * Created by jao on 27/10/14.
 */
class MembershipLandingPage(implicit driver: WebDriver) extends ParentPage {

  private def logoSpan = driver.findElement(By.cssSelector(".js-user-displayname"))

  private def logOutLink = driver.findElement(By.xpath("//a[contains(., \"Sign out\")]"))

  def signOut = {
    logoSpan.click
    waitUntil(visibilityOf(logOutLink))
    logOutLink.click
  }
}
