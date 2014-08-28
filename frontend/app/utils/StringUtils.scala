package utils

object StringUtils {

  def truncateToWordBoundary(text: String, length: Int) = {
    if (text.length <= length) text
    else {
      val index = text.lastIndexWhere(_.isSpaceChar, length + 1)
      text.take(if (index>= 0) index else length) + " …"
    }
  }

}
