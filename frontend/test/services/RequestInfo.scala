package services

case class RequestInfo(url: String, body: Map[String, Seq[String]])

object RequestInfo {
  val empty = RequestInfo("", Map.empty)
}
