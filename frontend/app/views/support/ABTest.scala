package views.support



import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup._
import play.api.libs.json._
import play.api.mvc.{Cookie, Request}
import play.twirl.api.Html
import scala.util.Random

import scalaz.NonEmptyList


trait TestTrait {

  type VariantFn
  case class Variant(variantName: String, variantSlug: String, weight: Double, render: VariantFn, countryGroupFilter: Set[CountryGroup] = Set.empty) {
    def testName = name
    def testSlug = slug
    def matches(countryGroup:CountryGroup):Boolean = countryGroupFilter.isEmpty || countryGroupFilter.contains(countryGroup)
  }

  def name: String
  def slug: String
  def variants: NonEmptyList[Variant]

  val variantsByCountry: Map[CountryGroup, Set[Variant]] = {

    def filterVariants(countryGroup: CountryGroup) = variants.list.filter(_.matches(countryGroup)).toSet

    CountryGroup.allGroups.map(country => country -> filterVariants(country)).toMap
  }

  val variantRangesByCountry: Map[CountryGroup, Seq[(Double, Variant)]] = {

    def addRanges(filteredVariants: Set[Variant]): Seq[(Double, Variant)] = {
      val filteredVariantList = filteredVariants.toList
      val variantWeights = filteredVariantList.map(_.weight)
      val weightSum = variantWeights.sum
      val totalWeight = if (weightSum != 0.0) weightSum else 1
      val cdf: Seq[Double] = variantWeights.scanLeft(0.0)(_ + _).tail.map(_ / totalWeight)
      cdf.zip(filteredVariantList)
    }

    variantsByCountry.map { case (country, variants) => country -> addRanges(variants) }
  }
}

object AmountHighlightTest extends TestTrait {

  def name = "AmountHighlightTest"
  def slug = "highlight"
  override type VariantFn = (CountryGroup, Option[Int]) => Html
  private lazy val notAustralia: Set[CountryGroup] = Set(
    UK,
    US,
    Canada,
    NewZealand,
    Europe,
    RestOfTheWorld
  )
  def variants = NonEmptyList(
    Variant("Amount - 5 highlight","5",0,views.html.fragments.giraffe.contributeAmountButtons(List(5,25,50,100),Some(5)),notAustralia),
    Variant("Amount - 25 highlight","25",0,views.html.fragments.giraffe.contributeAmountButtons(List(5,25,50,100),Some(25)), notAustralia),
    Variant("Amount - no highlight","None",0,views.html.fragments.giraffe.contributeAmountButtons(List(5,25,50,100), None), notAustralia),
    Variant("Amount - 35 highlight","35",0,views.html.fragments.giraffe.contributeAmountButtons(List(10,35,65,100),Some(35)), notAustralia),
    Variant("Amount - 35 highlight descending","35-descending",0,views.html.fragments.giraffe.contributeAmountButtons(List(100,65,35,10),Some(35)), notAustralia),
    Variant("Amount - 100 highlight","100",0,views.html.fragments.giraffe.contributeAmountButtons(List(25,50,100,250),Some(100)), notAustralia),
    Variant("Amount - 50 highlight","50",1,views.html.fragments.giraffe.contributeAmountButtons(List(25,50,100,250),Some(50)), notAustralia),
    Variant("Amount - 15","15",0,views.html.fragments.giraffe.contributeAmountButtons(List(15,35,65,100),Some(35)), notAustralia),
    Variant("Amount - 40 highlight","40",0,views.html.fragments.giraffe.contributeAmountButtons(List(20,40,75,100),Some(40)), notAustralia),

    Variant("Amount - 100 highlight Australia", "40-Australia", 1, views.html.fragments.giraffe.contributeAmountButtons(List(50, 100, 250, 500), Some(100)), Set(Australia))


  )
}

object MessageCopyTest extends TestTrait {
  def name = "MessageCopyTest"
  def slug = "mcopy"
  override type VariantFn = () => Html

  def variants = NonEmptyList(
    Variant("Copy - control", "control", 1, views.html.fragments.giraffe.contributeMessage("Support the Guardian")),
    Variant("Copy - support", "support", 0, views.html.fragments.giraffe.contributeMessage("Support the Guardian")),
    Variant("Copy - power", "power", 0, views.html.fragments.giraffe.contributeMessage("The powerful won't investigate themselves. That's why we need you.")),
    Variant("Copy - mutual", "mutual", 0, views.html.fragments.giraffe.contributeMessage("Can't live without us? The feeling's mutual.")),
    Variant("Copy - everyone", "everyone", 0, views.html.fragments.giraffe.contributeMessage("If everyone who sees this chipped in the Guardian's future would be more secure.")),
    Variant("Copy - everyone", "everyoneinline", 0, views.html.fragments.giraffe.contributeMessage("If everyone who sees this chipped in the Guardian's future would be more secure.")),
    Variant("Copy - everyone editorial", "everyone-editorial", 0, views.html.fragments.giraffe.contributeMessage("If everyone who sees this chipped in the Guardian's future would be more secure.")),
    Variant("Copy - expensive", "expensive", 0, views.html.fragments.giraffe.contributeMessage("Producing the Guardian is expensive. Supporting it isn't.")),
    Variant("Copy - expensive inline", "expensiveinline", 0, views.html.fragments.giraffe.contributeMessage("Producing the Guardian is expensive. Supporting it isn't.")),
    Variant("Copy - british", "british", 0, views.html.fragments.giraffe.contributeMessage("It's not very British to talk about money. So we'll just ask for it instead.")),
    Variant("Copy - british inline", "britishinline", 0, views.html.fragments.giraffe.contributeMessage("It's not very British to talk about money. So we'll just ask for it instead.")),
    Variant("Copy - powerless", "powerless", 0, views.html.fragments.giraffe.contributeMessage("Don't let the powerless pay the price. Make your contribution")),
    Variant("Copy - powerless inline", "powerlessinline", 0, views.html.fragments.giraffe.contributeMessage("Don't let the powerless pay the price. Make your contribution")),
    Variant("Copy - coffee inline", "costofnewswithyourcoffeeinline", 0, views.html.fragments.giraffe.contributeMessage("Do you want the news with your coffee or do you just want coffee? Quality journalism costs. Please contribute.")),
    Variant("Copy - coffee", "costofnewswithyourcoffee", 0, views.html.fragments.giraffe.contributeMessage("Do you want the news with your coffee or do you just want coffee? Quality journalism costs. Please contribute.")),
    Variant("Copy - heritage inline", "heritageinline", 0, views.html.fragments.giraffe.contributeMessage("From the Peterloo massacre to phone hacking and the Panama Papers, we've been there - on your side for almost 200 years. Contribute to the Guardian today")),
    Variant("Copy - heritage", "heritage", 0, views.html.fragments.giraffe.contributeMessage("From the Peterloo massacre to phone hacking and the Panama Papers, we've been there - on your side for almost 200 years. Contribute to the Guardian today")), Variant("Copy - global beijing inline", "global-beijing-inline", 0, views.html.fragments.giraffe.contributeMessage("By the time you've had your morning tea, reporters in Rio, Beijing, Moscow, Berlin, Paris, Johannesburg have already filed their stories. Covering the world's news isn't cheap. Please chip in a few pounds.")),
    Variant("Copy - global beijing inline", "global-beijing-inline", 0, views.html.fragments.giraffe.contributeMessage("By the time you've had your morning tea, reporters in Rio, Beijing, Moscow, Berlin, Paris, Johannesburg have already filed their stories. Covering the world's news isn't cheap. Please chip in a few pounds.")),
    Variant("Copy - global beijing", "global-beijing", 0, views.html.fragments.giraffe.contributeMessage("By the time you've had your morning tea, reporters in Rio, Beijing, Moscow, Berlin, Paris, Johannesburg have already filed their stories. Covering the world's news isn't cheap. Please chip in a few pounds."))

  )
}

case class ChosenVariants(v1: AmountHighlightTest.Variant, v2: MessageCopyTest.Variant) {
  def asList: Seq[TestTrait#Variant] = Seq(v1,v2) //this makes me very sad
  def asJson = Json.toJson(asList).toString()
  def encodeURL = URLEncoder.encode(asJson, StandardCharsets.UTF_8.name())
  implicit val writesVariant: Writes[TestTrait#Variant] = new Writes[TestTrait#Variant]{
    def writes(variant: TestTrait#Variant) =  Json.obj(
      "testName" -> variant.testName,
      "testSlug" -> variant.testSlug,
      "variantName" -> variant.variantName,
      "variantSlug" -> variant.variantSlug
    )
  }
}

object Test {
  val allTests = List(AmountHighlightTest, MessageCopyTest)

  def pickVariant[A](countryGroup: CountryGroup, request: Request[A], test: TestTrait): test.Variant = {

    def pickRandomly: test.Variant = {
      val n = Random.nextDouble
      test.variantRangesByCountry(countryGroup).dropWhile(_._1 < n).head._2
    }

    def pickByQueryStringOrCookie[A]: Option[test.Variant] = {
      val search: Option[String] = request.getQueryString(test.slug)
        .orElse(request.cookies.get(test.slug + "_GIRAFFE_TEST").map(_.value))
      test.variantsByCountry(countryGroup).find(_.variantSlug == search.getOrElse(None))
    }
    pickByQueryStringOrCookie getOrElse pickRandomly
  }

  def createCookie(variant: TestTrait#Variant): Cookie = {
    Cookie(variant.testSlug+"_GIRAFFE_TEST", variant.variantSlug, maxAge = Some(1209600))
  }

  def getContributePageVariants[A](countryGroup: CountryGroup,request: Request[A]) = {
    ChosenVariants(pickVariant(countryGroup, request, AmountHighlightTest), pickVariant(countryGroup, request, MessageCopyTest))
  }

}




