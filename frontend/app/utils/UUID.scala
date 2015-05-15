package utils

import java.util.{UUID => JUUID}

object UUID {
  def next() = JUUID.randomUUID().toString
}
