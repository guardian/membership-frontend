import scala.util.Either.RightProjection

package object utils {
  implicit class OptionOps[T](get: Option[T]) {
    def toEither[L](left: L): RightProjection[L, T] = get.toRight(left).right
  }
}
