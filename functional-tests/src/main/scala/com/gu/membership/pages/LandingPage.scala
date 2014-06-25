package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

 class LandingPage(driver: WebDriver) extends BasePage(driver) {

   private def title = driver.findElement(By.cssSelector("h1"))

   private def eventsButton = driver.findElement(By.linkText("Events"))

   private def joinButton = driver.findElement(By.linkText("Become a member"))

   def getTitle(): String = title.getText

   def clickEventsButton: EventsListPage = {
     eventsButton.click
     new EventsListPage(driver)
   }

   def clickJoinButton: JoinPage = {
     joinButton.click
     new JoinPage(driver)
   }
}
