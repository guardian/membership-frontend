package services

import java.math.BigInteger

object DiscountCode {
  def generate(text: String): String = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
    val digest = md.digest(text.getBytes)
    val uniqueUserEventCode =
      new BigInteger(digest).abs.toString(36).toUpperCase.substring(0, 8)
    s"MEMBER-CODE-$uniqueUserEventCode"
  }
}
