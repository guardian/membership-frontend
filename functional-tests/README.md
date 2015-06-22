# Membership Functional tests

## Setup

1. You will need to install [ChromeDriver](https://code.google.com/p/selenium/wiki/ChromeDriver) to run the tests. If you are a Mac and using homebrew you can run `brew install chromedriver`.
2. In `~/.gu/identity-api.properties` you will need to disable rate limiting with `rate-limiting-switch=false`
3. You will need to create a config file in `functional-tests/local.conf` with the the template below.
4. In `functional-tests` you can then run `sbt test` to start the tests

## Tagged Tests
### Adding Tags
The tag is added as a comma separated list for each scenario after the scenario name, e.g. `scenarioWeb("Example test that is essential but unstable", CoreTest, Unstable)`

###  Running in SBT
To run the tagged tests in the SBT console you will need the local environment to be running for frontend, identity, and membership

1. `sbt` to enter SBT console  
2. `test-only -- -n TAG_VALUE` where `TAG_VALUE` is the tag you want to run e.g., `EventListTest`

### Running in Run/Debug configuration
To run the tagged tests in the IDE, useful for debugging, you will need the local environment to be running for frontend, identity, and membership

1. Edit the `Run/Debug configuration` for the test(s) - if there are none then right click on the tests package or class and select `Run`   
2. In the field `Test options:` enter the tag value, e.g. `-n CoreTest -l OptionalTest -l Unstable`
  
### Running in TeamCity
To run the tagged tests as part of a CI or distributed automation suite you will need the code environment to be running for frontend, identity, and membership

1. Select the project  
2. Navigate to `Edit Configuration Settings` -> `Build Step`   
3. In `SBT Commands` enter the following `"project functional-tests" clean compile "test-only -- -l unstable -l OptionalTest"`    
*NOTE:* Ideally you should not use `-n` as this will ignore tests that are untagged. By only using `-l` these untagged tests will also be run

If the trigger is set up correctly then the build will run the tagged tests whenever a build is deployed to the environment or at periodic times during the day, depending on what is most useful.

### Available tags

The full list of available tags can be found in [Tags.scala](src/main/scala/com.gu.membership/tags/Tags.scala)

### local.conf

  "browserEnvironment" : "local"  
  "environment" : "local"  
  "local" : {  
      "browser" : "phantomjs"  
      "webDriverRemoteUrl" : "http://localhost:4444/wd/hub"  
      "testBaseUrl" : "https://mem.thegulocal.com/"  
  }   
  "user" : {  
      "identityReturn" : "https://profile.thegulocal.com/signin?returnUrl=https%3A%2F%2Fmem.thegulocal.com&skipConfirmation=true",  
      "partnerPayment" : "https://mem.thegulocal.com/join/partner/payment",  
      "accountEdit"    : "https://profile.thegulocal.com/account/edit",  
      "membershipEdit" : "https://profile.thegulocal.com/membership/edit",  
      "identity"       : "https://profile.thegulocal.com/signin?skipConfirmation=true",  
      "browser"        : "chrome"  
  }

## Sauce Connect

To run functional tests locally with Sauce Connect:

1. Setup a new personal key in saucelabs
2. Download the Sauce Connect driver from saucelabs: `https://docs.saucelabs.com/reference/sauce-connect/`
3. Run sauceconnect `bin/sc -u USERNAME -k KEY`
4. Run the functional tests `sbt test`
