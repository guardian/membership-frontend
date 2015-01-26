# Membership Functional tests

## Setup

1. You will need to install [ChromeDriver](https://code.google.com/p/selenium/wiki/ChromeDriver) to run the tests. If you are a Mac and using homebrew you can run `brew install chromedriver`.
2. In `~/.gu/identity-api.properties` you will need to disable rate limiting with `rate-limiting-switch=false`
3. You will need to create a config file in `funcitonal-tests/local.conf` with the the template below.
4. In `functional-tests` you can then run `sbt test` to start the tests

### local.conf

```
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
```

## Sauce Connect

To run functional tests locally with Sauce Connect:

1. Setup a new personal key in saucelabs
2. Download the Sauce Connect driver from saucelabs: `https://docs.saucelabs.com/reference/sauce-connect/`
3. Run sauceconnect `bin/sc -u USERNAME -k KEY`
4. Run the functional tests `sbt test`
