package views.support

import com.gu.i18n.CountryGroup
import org.scalatest.{MustMatchers, WordSpec}
import CountryGroup._
import org.scalatest.matchers.{MatchResult, Matcher}

import scalaz.NonEmptyList

class TestTraitSpec extends WordSpec with MustMatchers {

  "A Test" must {
    "generate even split in ranges for 2 variants of equal weights" in {
      val test = getTestTrait(NonEmptyList(("v1", 20.0, allGroups), ("v2", 20.0, allGroups)))

      val expectedRanges = List(("v1", 0.5), ("v2", 1.0))

      allGroups.foreach(c => extractWeights(test, c) must matchRanges(expectedRanges))
    }

    "generate even split in ranges for 3 variants of equal weights" in {
      val test = getTestTrait(NonEmptyList(("v1", 40.0, allGroups), ("v2", 40.0, allGroups), ("v3", 40.0, allGroups)))
      val expectedRanges = List(("v1", 0.333), ("v2", 0.666), ("v3", 1.0))
      allGroups.foreach(c => extractWeights(test, c) must matchRanges(expectedRanges))
    }

    "generate ranges of size proportional to weights" in {
      val test = getTestTrait(NonEmptyList(("v1", 50.0, allGroups), ("v2", 30.0, allGroups), ("v3", 20.0, allGroups)))
      val expectedRanges = List(("v1", 0.5), ("v2", 0.8), ("v3", 1.0))
      allGroups.foreach(c => extractWeights(test, c) must matchRanges(expectedRanges))
    }

    "filter variant with country filters correctly" in {
      val test = getTestTrait(NonEmptyList(("v1", 50.0, allGroups), ("v2", 30.0, List(UK)), ("v3", 20.0, List(US))))
      val notUkOrUs = allGroups.filterNot(c => (c == UK || c == US))

      notUkOrUs.foreach(extractWeights(test, _) must matchRanges(List(("v1", 1.0))))
      extractWeights(test, UK) must matchRanges(List(("v1", 0.625), ("v2", 1.0)))
      extractWeights(test, US) must matchRanges(List(("v1", 0.714), ("v3", 1.0)))
    }

  }

  def extractWeights(test: TestTrait, c: CountryGroup) = test.variantRangesByCountry(c).map { case (weight, variant) => ((variant.variantName, weight)) }

  def getTestTrait(weights: NonEmptyList[(String, Double, List[CountryGroup])]): TestTrait = {
    object TestImp extends TestTrait {
      override type VariantFn = String

      override def name: String = "something"

      override def slug: String = "somethingElse"

      override def variants: NonEmptyList[TestImp.Variant] = weights.map { case (name, weight, countries) => Variant(name, "slug", weight, "render", countries.toSet) }
    }
    TestImp
  }

  case class RangeMatcher(expectedRanges: Seq[(String, Double)]) extends Matcher[Seq[(String, Double)]] {
    def apply(actual: Seq[(String, Double)]): MatchResult = {
      actual.size mustEqual expectedRanges.size
      val combined = actual zip expectedRanges
      val result = !combined.exists { case ((actualName, actualRange), (expectedName, expectedRange)) => (actualName != expectedName || (actualRange !== expectedRange +- .001)) }

      MatchResult(result, s"$expectedRanges is not equal to $actual", s"$expectedRanges is different to $actual found")
    }
  }

  def matchRanges(ranges: Seq[(String, Double)]) = RangeMatcher(ranges)

}
