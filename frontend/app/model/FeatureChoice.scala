package model

trait FeatureChoice {
  val id: String
  val label: String
  val codes: Seq[String]
}

object FeatureChoice {
  val partners: Seq[FeatureChoice] = Seq(
    Events,
    Books
  )

  def fromString(id: String): Option[FeatureChoice] = id match {
    case Books.id => Some(Books)
    case Events.id => Some(Events)
    case BooksAndEvents.id => Some(BooksAndEvents)
    case _ => None
  }
}

case object Books extends FeatureChoice {
  override val id    = "Books"
  override val label = "4 free books"
  override val codes = Seq("Books")
}
case object Events extends FeatureChoice {
  override val id    = "Events"
  override val label = "6 free events"
  override val codes = Seq("Events")
}

case object BooksAndEvents extends FeatureChoice {
  override val id = "Both"
  override val label = "Patron memberships are entitled to both"
  override val codes = Books.codes ++ Events.codes
}
