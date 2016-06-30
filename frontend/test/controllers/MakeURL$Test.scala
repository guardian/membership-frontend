package controllers

import com.gu.i18n.CountryGroup
import org.scalatest.FunSuite
import org.specs2.mutable.Specification
import play.api.test.FakeRequest

class MakeURL$Test extends Specification {

  "No parameters" in {
    val fakeRequest = FakeRequest("GET", "/contribute")

    MakeURL(fakeRequest, CountryGroup.UK) must_== "/uk/contribute?"
  }

  "One parameter" in {
    val fakeRequest = FakeRequest("GET", "/contribute?test1=a")

    MakeURL(fakeRequest, CountryGroup.UK) must_== "/uk/contribute?test1=a"
  }


  "Multiple parameters" in {
    val fakeRequest = FakeRequest("GET", "/contribute?test1=a&test2=b")

    MakeURL(fakeRequest, CountryGroup.UK) must_== "/uk/contribute?test1=a&test2=b"
  }

  "US" in {
    val fakeRequest = FakeRequest("GET", "/contribute?test1=a&test2=b")

    MakeURL(fakeRequest, CountryGroup.US) must_== "/us/contribute?test1=a&test2=b"
  }

  "Europe" in {
    val fakeRequest = FakeRequest("GET", "/contribute?test1=a&test2=b")

    MakeURL(fakeRequest, CountryGroup.Europe) must_== "/eu/contribute?test1=a&test2=b"
  }

  "Australia" in {
    val fakeRequest = FakeRequest("GET", "/contribute?test1=a&test2=b")

    MakeURL(fakeRequest, CountryGroup.Australia) must_== "/au/contribute?test1=a&test2=b"
  }

  "Canada" in {
    val fakeRequest = FakeRequest("GET", "/contribute?test1=a&test2=b")

    MakeURL(fakeRequest, CountryGroup.Canada) must_== "/us/contribute?test1=a&test2=b"
  }

  "Rest of world" in {
    val fakeRequest = FakeRequest("GET", "/contribute?test1=a&test2=b")

    MakeURL(fakeRequest, CountryGroup.RestOfTheWorld) must_== "/us/contribute?test1=a&test2=b"
  }




}