package tests

import java.util.concurrent.TimeUnit

import com.gu.automation.core.{GivenWhenThen, WebDriverFeatureSpec}
import com.gu.automation.support.{Browser, Config, TestRetries}

/**
 * Created by jao on 20/06/2014.
 */
abstract class BaseMembershipTest extends WebDriverFeatureSpec with TestRetries with GivenWhenThen {

    override def startDriver(testName: String, targetBrowser: Browser, extraCapabilities: Map[String, String] = Map()) = {
      val driver = super.startDriver(testName, Browser(Config().getUserValue("browser"), None), extraCapabilities)
      driver.manage().timeouts().implicitlyWait(25, TimeUnit.SECONDS)
      driver
    }
}
