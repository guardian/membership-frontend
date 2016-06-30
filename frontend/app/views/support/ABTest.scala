package views.support



import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.gu.i18n.CountryGroup
import play.api.libs.json._
import play.api.mvc.{Cookie, Request}
import play.twirl.api.Html

import scalaz.NonEmptyList

sealed trait TestTrait {

  type VariantFn
  case class Variant(variantName: String, variantSlug: String, weight: Double, render: VariantFn) {
    def testName = name
    def testSlug = slug
  }

  def name: String
  def slug: String
  def variants: NonEmptyList[Variant]
  val weight: Double = variants.map(_.weight).list.fold(0.0)(_ + _)
  val cdf : Seq[Double]= variants.map(_.weight).list.foldLeft(Seq[Double]())( (l,p) => l :+ l.lastOption.getOrElse(0.0) + p/weight)
  val weightedVariants = cdf.zip(variants.list)
}

object AmountHighlightTest extends TestTrait {

  def name = "AmountHighlightTest"
  def slug = "highlight"
  override type VariantFn = (CountryGroup, Option[Int]) => Html

  def variants = NonEmptyList(
    Variant("Amount - 5 highlight","5",0,views.html.fragments.giraffe.contributeAmountButtons(List(5,25,50,100),Some(5))),
    Variant("Amount - 25 highlight","25",0,views.html.fragments.giraffe.contributeAmountButtons(List(5,25,50,100),Some(25))),
    Variant("Amount - no highlight","None",0,views.html.fragments.giraffe.contributeAmountButtons(List(5,25,50,100), None)),
    Variant("Amount -  35 highlight","35",0,views.html.fragments.giraffe.contributeAmountButtons(List(10,35,65,100),Some(35))),
    Variant("Amount -  35 highlight descending","35-descending",0,views.html.fragments.giraffe.contributeAmountButtons(List(100,65,35,10),Some(35))),
    Variant("Amount -  50 highlight","50",0.33,views.html.fragments.giraffe.contributeAmountButtons(List(25,50,100,250),Some(50))),
    Variant("Amount -  75 highlight","75",0.33,views.html.fragments.giraffe.contributeAmountButtons(List(25,50,75,150),Some(75))),
    Variant("Amount -  100 highlight","100",0.33,views.html.fragments.giraffe.contributeAmountButtons(List(25,50,100,250),Some(100)))
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
  def pickRandomly(test: TestTrait): test.Variant = {
    val n = scala.util.Random.nextDouble()
    test.weightedVariants.dropWhile(_._1 < n).head._2
  }

  def pickByQueryStringOrCookie[A](request: Request[A], test: TestTrait): Option[test.Variant] = {
    val search: Option[String] = request.getQueryString(test.slug)
      .orElse(request.cookies.get(test.slug + "_GIRAFFE_TEST").map(_.value))
    test.variants.list.find(_.variantSlug == search.getOrElse(None))
  }

  def pickVariant[A](request: Request[A], test: TestTrait) : test.Variant = {
    pickByQueryStringOrCookie(request, test).getOrElse(pickRandomly(test))
  }

  def createCookie(variant: TestTrait#Variant): Cookie = {
    Cookie(variant.testSlug+"_GIRAFFE_TEST", variant.variantSlug, maxAge = Some(1209600))
  }

  def getContributePageVariants[A](request: Request[A]) = {
    ChosenVariants(pickVariant(request, AmountHighlightTest), pickVariant(request, MessageCopyTest))
  }
}




