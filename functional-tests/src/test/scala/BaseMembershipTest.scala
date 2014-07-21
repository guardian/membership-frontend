import java.util.concurrent.TimeUnit

import com.gu.automation.core.{GivenWhenThen, WebDriverFeatureSpec}
import com.gu.automation.support.TestRetries

/**
 * Created by jao on 20/06/2014.
 */
abstract class BaseMembershipTest extends WebDriverFeatureSpec with TestRetries with GivenWhenThen {

    override def startDriver(testName: String) = {
      val driver = super.startDriver(testName)
      driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS)
      driver
    }
}
