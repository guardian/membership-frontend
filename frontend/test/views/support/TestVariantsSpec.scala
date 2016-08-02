package views.support

import com.gu.i18n.CountryGroup
import org.scalatest.{MustMatchers, WordSpec}

class TestVariantsSpec extends WordSpec with MustMatchers {

  val allCountries = CountryGroup.allGroups

  Test.allTests.foreach { test =>
    test.name must {
      "have non negative weights for all variants" in {
        val negativeWeightVariants = test.variants.list.filter(_.weight < 0)
        negativeWeightVariants mustBe empty
      }
      "have at least one variant per supported CountryGroup" in {
        allCountries.map(test.variantsByCountry(_)).filter(_.isEmpty) mustBe empty
      }
      "have at least one variant range per supported CountryGroup" in {
        allCountries.map(test.variantRangesByCountry(_)).filter(_.isEmpty) mustBe empty
      }
      "have ranges for all countries" in {
        test.variantRangesByCountry.size mustBe allCountries.size
      }
      "have variant ranges that end at 1" in {
        val maxRange = test.variantRangesByCountry.values.map(_.unzip._1.max).toSet
        maxRange mustEqual Set(1.0)
      }
      "variant ranges should be greater or equal to 0" in {
        val negativeMinRanges = test.variantRangesByCountry.values.map(_.unzip._1.min).filter(_ < 0)
        negativeMinRanges mustBe empty
      }
      "have variant ranges with no duplicate values" in {
        allCountries.foreach { c =>
          val ranges = test.variantRangesByCountry(c)
          ranges.size mustBe ranges.toSet.size
        }
      }
    }
  }

}
