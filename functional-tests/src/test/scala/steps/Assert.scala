package steps

import org.scalatest.Matchers._

/**
 * Created by jao on 10/07/2014.
 */
object Assert {

  def assert[A](found: A, expected: A, message: String = "") {
    println("Expecting " + found + " to be " + expected + ". " + message)
    found should be(expected)
  }

  def assertNotEmpty(found: String, message: String = "") {
    println("Expecting " + found + " to not be empty")
    found should not be empty
  }
}
