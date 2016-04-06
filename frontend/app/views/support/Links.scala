package views.support

object Links {
  case class BasicLink(
      href: String, title: String, idOpt: Option[String] = None)
}
