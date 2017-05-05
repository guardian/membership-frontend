package abtests

import java.time.Duration.ofDays

import abtests.AudienceId._
import abtests.AudienceRange.percentageToId
import com.google.common.hash.Hashing.goodFastHash
import play.api.mvc.{Cookie, RequestHeader}

import scala.util.Random

object AudienceId {
  val MaxId: Int = 10000

  val CookieName = "GU_MEM_mvt_id"

  val CookieAge = ofDays(3650).getSeconds.toInt

  private val startupTime = System.currentTimeMillis()

  private val AudienceHash = goodFastHash(64) // Note this isn't stable between VM runs & that's fine for this purpose

  /**
   * @return an Audience id for this request.
    *         The Audience id for a given request will always be the same for a given JVM instance, and should distribute
    *         well over all JVM instances.
   */
  def generateIdFor(req: RequestHeader): AudienceId = {
    val seed = AudienceHash.newHasher()
      .putLong(req.id) // Integer incremented by 0 for each request, unique only in this JVM instance
      .putLong(startupTime) // Explicitly placed here to ensure different JVMs have different hash distributions
      .hash().asLong()
    AudienceId(new Random(seed).nextInt(MaxId)) // uniformly distributed, even tho' 2^32 is not divisible by MaxId. See https://docs.oracle.com/javase/8/docs/api/java/util/Random.html#nextInt-int-
  }

  def idFromMVTCookie(req: RequestHeader): Option[AudienceId] = for {
    cookie <- req.cookies.get(CookieName)
  } yield AudienceId(cookie.value.toInt)

  def idFor(req: RequestHeader): AudienceId = idFromMVTCookie(req) getOrElse AudienceId.generateIdFor(req)

  def cookieWhichShouldBeDropped(req: RequestHeader): Option[Cookie] = idFromMVTCookie(req) match {
    case Some(existingCookieValue) => None
    case None => Some(AudienceId.generateIdFor(req).cookie)
  }
}

case class AudienceId(num: Int) {
  require(0 <= num)
  require(num < MaxId)

  lazy val cookie = Cookie(CookieName, num.toString, maxAge = Some(CookieAge), httpOnly = false, secure = true)
}


case class AudienceRange(start: Double, end: Double) {
  val startId: Int = percentageToId(start)
  val endId: Int = percentageToId(end)

  def includes(audienceId: AudienceId): Boolean = startId <= audienceId.num && audienceId.num < endId
}

object AudienceRange {

  def percentageToId(p: Double): Int = (AudienceId.MaxId * p / 100).toInt

  implicit class PercentageNum[A](start: A)(implicit num: Numeric[A]) {
    def upTo(end: A) = AudienceRange(num.toDouble(start), num.toDouble(end))
  }

  val NoAudience = 0 upTo 0

  val FullAudience = 0 upTo 100
}
