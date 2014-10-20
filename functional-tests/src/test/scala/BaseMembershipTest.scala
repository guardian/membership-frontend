import java.util.concurrent.TimeUnit

import com.gu.automation.core.{GivenWhenThen, WebDriverFeatureSpec}
import com.gu.automation.support.{Browser, TestRetries}
import com.gu.automation.support.Config

/**
 * Created by jao on 20/06/2014.
 */
abstract class BaseMembershipTest extends WebDriverFeatureSpec with TestRetries with GivenWhenThen {

    override def startDriver(testName: String, targetBrowser: Browser, extraCapabilities: Map[String, String] = Map()) = {
      val capabilities = extraCapabilities + ("browserstack.local" -> "true");
      val driver = super.startDriver(testName, Browser(Config().getUserValue("browser"), None), capabilities)
      driver.manage().timeouts().implicitlyWait(25, TimeUnit.SECONDS)
      driver
    }
}
