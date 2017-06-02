package utils

import org.specs2.mutable.Specification
import StringUtils._

class StringUtilsTest extends Specification {

  "firstSentenceOrTruncate" should {
    "not truncate if text shorter than the limit" in {
      firstSentenceOrTruncate("qwerty", 8) mustEqual "qwerty"
    }

    "truncate if text longer than the limit" in {
      firstSentenceOrTruncate("qwerty", 3) mustEqual "qwe…"
    }

    "truncate at a word boundary" in {
      firstSentenceOrTruncate("this is a test string", 13) mustEqual "this is a test…"
      firstSentenceOrTruncate("this is a test string", 14) mustEqual "this is a test…"
      firstSentenceOrTruncate("this is a test string", 15) mustEqual "this is a test…"
    }

    "truncate to first sentence" in {
      val graf = """Steve Coogan got his first break as a voice artist on Spitting Image, providing the voices of Neil Kinnock and Margaret Thatcher.
        | A stand-up show introducing characters including Ernest Moss and student-hating Paul Calf earned him a Perrier award in 1992.""".stripMargin
      val grafSingle = "Steve Coogan got his first break as a voice artist on Spitting Image, providing the voices of Neil Kinnock and Margaret Thatcher"

      firstSentenceOrTruncate(graf, 120) mustEqual "Steve Coogan got his first break as a voice artist on Spitting Image, providing the voices of Neil Kinnock and Margaret Thatcher"
      firstSentenceOrTruncate(grafSingle, 120) mustEqual "Steve Coogan got his first break as a voice artist on Spitting Image, providing the voices of Neil Kinnock and Margaret…"
    }
  }

  "slugify" should {
    "normalize accented characters" in {
      slugify("ÀÁÂÃÄÅÇÈÉÊËÌÍÎÏÑÒÓÔÕÖÙÚÛÜÝß") mustEqual "aaaaaaceeeeiiiinooooouuuuy"
      slugify("àáâãäåçèéêëìíîïñòóôõöùúûüýÿ") mustEqual "aaaaaaceeeeiiiinooooouuuuyy"
    }

    "remove symbols" in {
      slugify("Something & something else") mustEqual "something-something-else"
      slugify("****£20 event name /|/|/|/| @blah ^^^") mustEqual "20-event-name-blah"
    }

    "never have multiple hyphens consecutively" in {
      slugify("blah  +  blah") mustEqual "blah-blah"
    }
  }

}
