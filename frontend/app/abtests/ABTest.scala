package abtests

import java.time.Duration.ofDays

import abtests.AudienceId.idFor
import play.api.mvc.{Cookie, Request, RequestHeader}

trait BaseVariant {
  val slug: String
}

abstract class ABTest(val slug: String, val audience: AudienceRange, canRun: RequestHeader => Boolean = _ => true) {
  val CookiePrefix = "gu.membership"

  val abSlug = s"ab.$slug"

  val cookieName = s"$CookiePrefix.$abSlug"

  type Variant <: BaseVariant

  val variants: Seq[Variant]

  lazy val variantsBySlug: Map[String, Variant] = variants.map(v => v.slug -> v).toMap

  def cookieFor(variant: BaseVariant) =
    Cookie(cookieName, variant.slug, maxAge = Some(ABTest.CookieAge), httpOnly = false)

  def describeParticipation(implicit request: RequestHeader): String =
    s"ab.$cookieName=${allocate(request).map(_.slug).getOrElse("None")}"

  def allocate(request: RequestHeader): Option[Variant] = for {
    v <- variantForcedBy(request) orElse variantFromABCookie(request) orElse defaultVariantFor(idFor(request)) if canRun(request)
  } yield v

  def variantForcedBy(req: RequestHeader): Option[Variant] = req.getQueryString(abSlug).map(variantSlug => variantsBySlug(variantSlug))

  def variantFromABCookie(req: RequestHeader): Option[Variant] = req.cookies.get(cookieName).map(cookie => variantsBySlug(cookie.value))

  def defaultVariantFor(id: AudienceId): Option[Variant] =
    if (audience.includes(id)) Some(variants(id.num % variants.size)) else None
}

object ABTest {

  val CookieAge = ofDays(365).getSeconds.toInt

  lazy val allTests: Set[ABTest] = Set(
    MergedRegistration,
    SupporterLandingPage
  )

  def allocations(request: Request[_]): Map[ABTest, BaseVariant] = (for {
    test <- allTests
    variant <- test.allocate(request)
  } yield test -> variant).toMap

  def cookiesWhichShouldBeDropped(request: Request[_]): Seq[Cookie] = for {
    (test, variant) <- allocations(request).toSeq
    cookie: Cookie = test.cookieFor(variant) if !request.cookies.get(cookie.name).map(_.value).contains(cookie.value)
  } yield cookie

}
