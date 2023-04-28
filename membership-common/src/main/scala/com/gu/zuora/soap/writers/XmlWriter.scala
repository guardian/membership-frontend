package com.gu.zuora.soap.writers
import scala.xml.Elem
import scalaz.Writer

trait XmlWriter[T] {
  def write(t: T): Writer[Map[String, String], Elem]
}

object XmlWriter {
  def write[T](t: T)(implicit w: XmlWriter[T]): Writer[Map[String, String], Elem] = w.write(t)
}