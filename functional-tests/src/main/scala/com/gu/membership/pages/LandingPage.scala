package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

 class LandingPage(driver: WebDriver) extends BaseMembershipPage(driver) {

   private def title = driver.findElement(By.cssSelector("h1"))

   private def eventsButton = driver.findElement(By.xpath("//header/div[2]/nav/div[1]/ul/li[2]/a"))

   private def joinButton = driver.findElement(By.id("qa-join"))

   def getTitle() = title.getText

   def clickEventsButton: EventsListPage = {
     eventsButton.click
     new EventsListPage(driver)
   }

   def clickJoinButton: JoinPage = {
     joinButton.click
     new JoinPage(driver)
   }
}
