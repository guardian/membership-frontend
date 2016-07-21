package controllers

import com.gu.i18n.CountryGroup
import org.scalatest.FunSuite
import org.specs2.mutable.Specification
import play.api.test.FakeRequest

class MakeGiraffeRedirectURL$Test extends Specification {

  "No parameters" in {
    val fakeRequest = FakeRequest("GET", "/contribute")

    MakeGiraffeRedirectURL(fakeRequest, CountryGroup.UK).toString() must_== "/uk/contribute"
  }

  "One parameter" in {
    val fakeRequest = FakeRequest("GET", "/contribute?test1=a")

    MakeGiraffeRedirectURL(fakeRequest, CountryGroup.UK).toString() must_== "/uk/contribute?test1=a"
  }


  "Multiple parameters" in {
    val fakeRequest = FakeRequest("GET", "/contribute?test1=a&test2=b")

    MakeGiraffeRedirectURL(fakeRequest, CountryGroup.UK).toString() must_== "/uk/contribute?test1=a&test2=b"
  }

  "US" in {
    val fakeRequest = FakeRequest("GET", "/contribute?test1=a&test2=b")

    MakeGiraffeRedirectURL(fakeRequest, CountryGroup.US).toString() must_== "/us/contribute?test1=a&test2=b"
  }

  "Europe" in {
    val fakeRequest = FakeRequest("GET", "/contribute?test1=a&test2=b")

    MakeGiraffeRedirectURL(fakeRequest, CountryGroup.Europe).toString() must_== "/eu/contribute?test1=a&test2=b"
  }

  "Australia" in {
    val fakeRequest = FakeRequest("GET", "/contribute?test1=a&test2=b")

    MakeGiraffeRedirectURL(fakeRequest, CountryGroup.Australia).toString() must_== "/au/contribute?test1=a&test2=b"
  }

  "Canada" in {
    val fakeRequest = FakeRequest("GET", "/contribute?test1=a&test2=b")

    MakeGiraffeRedirectURL(fakeRequest, CountryGroup.Canada).toString() must_== "/ca/contribute?test1=a&test2=b"
  }

  "Rest of world" in {
    val fakeRequest = FakeRequest("GET", "/contribute?test1=a&test2=b")

    MakeGiraffeRedirectURL(fakeRequest, CountryGroup.RestOfTheWorld).toString() must_== "/uk/contribute?test1=a&test2=b"
  }




}
