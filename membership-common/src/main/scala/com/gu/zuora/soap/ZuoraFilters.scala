package com.gu.zuora.soap
import scala.language.implicitConversions

sealed trait ZuoraFilter {
  def toFilterString: String
}

case class SimpleFilter(key: String, value: String, operator: String = "=") extends ZuoraFilter {
  override def toFilterString = s"$key$operator'$value'"
}

object ZuoraFilter {
  implicit def tupleToFilter(t: (String, String)): SimpleFilter = SimpleFilter(t._1, t._2)
}
