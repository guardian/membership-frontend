import scala.util.Either.RightProjection

package object utils {
  implicit class OptionOps[T](get: Option[T]) {
    def toEither(message: String): RightProjection[String, T] =
      get.toRight(message).right
  }
}
