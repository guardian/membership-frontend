package com.gu.membership.pages

import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.PageFactory

trait BasePageTrait {

  val driverWait: WebDriverWait
  val driver: WebDriver

  PageFactory.initElements(driver, this)
}

abstract class BasePage(createdDriver: WebDriver) extends BasePageTrait {
  override val driverWait = new WebDriverWait(createdDriver, 30)
  override val driver = createdDriver
}