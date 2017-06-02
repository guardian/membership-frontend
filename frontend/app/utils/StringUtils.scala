package utils

import java.text.Normalizer

object StringUtils {

  def seqToLine(seq: Seq[String], separator: String = ", ") = {
    if (seq.isEmpty) None else Some(seq.mkString(separator))
  }

  def firstSentenceOrTruncate(text: String, length: Int) = if (text.length <= length) text else {
    text.split("\\.").toList match {
      case firstSentence :: secondSentence :: anythingElse => firstSentence
      case _ =>
        val index = text.lastIndexWhere(_.isSpaceChar, length + 1)
        text.take(if (index >= 0) index else length) + "…"
    }
  }

  def slugify(text: String): String = {
    Normalizer.normalize(text, Normalizer.Form.NFKD)
      .toLowerCase
      .replaceAll("[^0-9a-z ]", "")
      .trim.replaceAll(" +", "-")
  }

}
