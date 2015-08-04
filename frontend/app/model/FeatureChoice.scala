package model

trait FeatureChoice {
  val id: String
  val label: String
}

object FeatureChoice {
  val all: Set[FeatureChoice] = Set(
    Events,
    Books
  )

  val codes = all.map(_.id)

  def fromString(id: String): Set[FeatureChoice] = id match {
    case Books.id => Set(Books)
    case Events.id => Set(Events)
    case _ =>
      if (id == Events.id + Books.id || id == Books.id + Events.id) Set(Books, Events)
      else Set.empty
  }
}

case object Books extends FeatureChoice {
  override val id    = "Books"
  override val label = "4 free books"
}
case object Events extends FeatureChoice {
  override val id    = "Events"
  override val label = "6 free events"
}
