package views.support


import java.net.URLEncoder

import com.gu.i18n.CountryGroup
import play.api.libs.json._
import play.api.mvc.{Cookie, Request}
import play.twirl.api.Html

import scalaz.NonEmptyList

sealed trait TestTrait {

  type VariantFn
  case class Variant(variantName: String, slug: String, weight: Double, render: VariantFn) {
    def testName = name
  }

  def name: String
  def variants: NonEmptyList[Variant]
  val weight: Double = variants.map(_.weight).list.fold(0.0)(_ + _)
  val cdf : Seq[Double]= variants.map(_.weight).list.foldLeft(Seq[Double]())( (l,p) => l :+ l.lastOption.getOrElse(0.0) + p/weight)
  val weightedVariants = cdf.zip(variants.list)
}


object AmountHighlightTest extends TestTrait {

  def name = "AmountHighlightTest"
  override type VariantFn = (CountryGroup, Option[Int]) => Html

  def variants = NonEmptyList(
    Variant("Amount test - 5 highlight","test1a",1,views.html.fragments.giraffe.contributeAmountButtons(List(5,25,50,100),Some(5))),
    Variant("Amount test - 25 highlight","test1b",1,views.html.fragments.giraffe.contributeAmountButtons(List(5,25,50,100),Some(25))),
    Variant("Amount test - No highlight","test1c",1,views.html.fragments.giraffe.contributeAmountButtons(List(5,25,50,100),None))
  )
}

object MessageCopyTest extends TestTrait {
  def name = "MessageCopyTest"
  override type VariantFn = () => Html
    def variants = NonEmptyList(
    Variant("Copy Test - Support","test2a",1,views.html.fragments.giraffe.contributeMessage("Support the Guardian")),
    Variant("Copy Test - Powerful", "test2b",1,views.html.fragments.giraffe.contributeMessage("The powerful won't investigate themselves")),
    Variant("Copy Test - Mutual","test2c",1,views.html.fragments.giraffe.contributeMessage("Can't live without us? The feeling's mutual")),
    Variant("Copy Test - Hell","test2d",1,views.html.fragments.giraffe.contributeMessage("Give us money - we'll give 'em hell"))
  )
}

case class ChosenVariants(v1: AmountHighlightTest.Variant, v2: MessageCopyTest.Variant) {
  def asList: Seq[TestTrait#Variant] = Seq(v1,v2) //this makes me very sad
  def asJson = Json.toJson(asList).toString()
  implicit val writesVariant: Writes[TestTrait#Variant] = new Writes[TestTrait#Variant]{
    def writes(variant: TestTrait#Variant) =  Json.obj(
      "testName" -> variant.testName,
      "variantName" -> variant.variantName,
      "slug" -> variant.slug
    )
  }
}





object Test {
  def pickRandomly(test: TestTrait): test.Variant = {
    val n = scala.util.Random.nextDouble()
    test.weightedVariants.dropWhile(_._1 < n).head._2
  }

  def pickByQueryStringOrCookie[A](request: Request[A], test: TestTrait): Option[test.Variant] = {
    val search: Option[String] = request.getQueryString(test.name)
      .orElse(request.cookies.get(test.name + "_GIRAFFE_TEST").map(_.value))
    test.variants.list.find(_.slug == search.getOrElse(None))
  }

  def pickVariant[A](request: Request[A], test: TestTrait) : test.Variant = {
    pickByQueryStringOrCookie(request, test).getOrElse(pickRandomly(test))
  }

  def getContributePageVariants[A](request: Request[A]) = {
    ChosenVariants(pickVariant(request, AmountHighlightTest), pickVariant(request, MessageCopyTest))
  }
}




