import java.util.concurrent.TimeUnit

import com.gu.automation.core.{GivenWhenThen, WebDriverFeatureSpec}
import com.gu.automation.support.TestRetries

/**
 * Created by jao on 20/06/2014.
 */
abstract class BaseMembershipTest extends WebDriverFeatureSpec with TestRetries with GivenWhenThen {

    override def startDriver(testName: String, extraCapabilities: Map[String, String] = Map()) = {
      val capabilities = extraCapabilities + ("browserstack.local" -> "true");
      val driver = super.startDriver(testName, capabilities)
      driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS)
      driver
    }
}
