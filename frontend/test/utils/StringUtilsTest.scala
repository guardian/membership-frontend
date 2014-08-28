package utils

import org.specs2.mutable.Specification
import StringUtils.truncateToWordBoundary

class StringUtilsTest extends Specification {

  "truncateToWordBoundary" should {
    "not truncate if text shorter than the limit" in {
      truncateToWordBoundary("qwerty", 8) mustEqual "qwerty"
    }

    "truncate if text longer than the limit" in {
      truncateToWordBoundary("qwerty", 3) mustEqual "qwe …"
    }

    "truncate at a word boundary" in {
      truncateToWordBoundary("this is a test string", 13) mustEqual "this is a test …"
      truncateToWordBoundary("this is a test string", 14) mustEqual "this is a test …"
      truncateToWordBoundary("this is a test string", 15) mustEqual "this is a test …"
    }
  }

}
