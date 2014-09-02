package com.gu.membership.pages

import org.openqa.selenium.{By, WebDriver}

/**
 * Created by jao on 13/08/2014.
 */
class JoinFlowChooseTierPage(driver: WebDriver) extends BaseMembershipPage(driver) {

  private def registerButton = driver.findElement(By.xpath("//div[1]/section/div[3]/div[2]/a"))

  private def friendButton = driver.findElement(By.xpath("//div[1]/section/ul[1]/li[1]/a"))

  def clickRegisterButton = {
    registerButton.click()
    new RegisterPage(driver)
  }

  def clickFriendButton = {
    friendButton.click()
    new JoinFlowRegisterOrSignUpPage(driver)
  }
}
