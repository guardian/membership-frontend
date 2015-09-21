package views.support

object RenderClasses {
  def apply(classes: Map[String, Boolean]): String = {
    apply(classes.filter(_._2).keys.toSeq:_*)
  }

  def apply(classes: String*): String = classes.filter(_.nonEmpty).sorted.distinct.mkString(" ")
}
