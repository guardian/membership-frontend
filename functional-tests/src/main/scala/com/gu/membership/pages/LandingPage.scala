package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

 class LandingPage(driver: WebDriver) extends BaseMembershipPage(driver) {

   private def title = driver.findElement(By.cssSelector("h1"))

   private def joinButton = driver.findElement(By.id("qa-join"))

   def getTitle() = title.getText

   def clickJoinButton: JoinPage = {
     joinButton.click
     new JoinPage(driver)
   }
}
