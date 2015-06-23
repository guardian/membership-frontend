package com.gu.membership.tags

import org.scalatest.Tag

/* Usage: The tag is added as a comma separated list for each scenario after the scenario name
e.g. scenarioWeb("Example test that is unstable but quick", Small, Unstable)
  Running tests locally:  The Run/Debug configuration can specify which tags you want to run or ignore using the tag
  text e.g. to run only core tests but ignore unstable or optional tests you would use the following in Test Options
  -n CoreTest -l OptionalTest -l Unstable
  Running tests TeamCity: In Edit Configuration Settings -> Build Step -> SBT Commands enter the following
  "project functional-tests" clean compile "test-only -- -l unstable"
  to add further tags use this format "test-only -- -l Unstable -l OptionalTest"
  NOTE: Ideally you should not use -n as this will ignore tests that are untagged. By only using -l these untagged
  tests will also be run
*/

/* CoreTest:
    + Non-members are up-sold on the identity membership tab (MB6)
    + A Partner's membership details are displayed on the identity membership tab (MB3)
    + A logged in user can see the full events list (ME1)
    + A friend can upgrade to a partner (MCT3)
    + Non-members are shown the potential membership discount on event prices (MB1)

*/

object EventListTest extends Tag("EventListTest")
object EventDetailTest extends Tag("EventDetailTest")
object EventTicketPurchase extends Tag("EventTicketPurchase")
object UserChangeTier extends Tag("UserChangeTier")

object Unstable extends Tag("Unstable") //A test which fails intermittently giving false negatives
object CoreTest extends Tag("CoreTest") //A test which must pass for the site to be functional
object OptionalTest extends Tag("OptionalTest") //test is important but this feature can be missing for a short time
