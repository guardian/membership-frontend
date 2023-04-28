package com.gu.zuora.soap.readers

import com.gu.zuora.soap.models
import com.gu.zuora.soap.models.errors._

import scala.xml.Node

trait Result[T <: models.Result] extends Reader[T] {
  protected def extractEither(result: Node): Either[Error, T] = {
    if ((result \ "Success").text == "true") {
      Right(extract(result))
    } else {
      Left(ErrorHandler(result))
    }
  }

  protected def extract(result: Node): T
}

object Result {
  def create[T <: models.Result](tag: String, multi: Boolean, extractFn: Node => T) = new Result[T] {
    val responseTag = tag
    override val multiResults: Boolean = multi
    protected def extract(result: Node) = extractFn(result)
  }

  def apply[T <: models.Result](tag: String)(extractFn: Node => T) = create(tag, multi=false, extractFn)
  def multi[T <: models.Result](tag: String)(extractFn: Node => T) = create(tag, multi=true, extractFn)
}
