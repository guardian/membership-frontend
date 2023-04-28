package com.gu.zuora.soap.readers

import com.gu.zuora.soap.models
import com.gu.zuora.soap.models.errors._

import scala.util.{Failure, Success, Try}
import scala.xml.Node

trait Reader[T <: models.Result] {
  val responseTag: String
  val multiResults = false

  def read(body: String): Either[Error, T] = {
    Try(scala.xml.XML.loadString(body)) match {
      case Failure(ex) => Left(XmlParseError(ex.getMessage))

      case Success(node) =>
        val body = scala.xml.Utility.trim((scala.xml.Utility.trim(node) \ "Body").head)

        (body \ "Fault").headOption.fold {
          val resultNode = if (multiResults) "results" else "result"
          val result = body \ responseTag \ resultNode

          extractEither(result.head)
        } { fault => Left(ErrorHandler(fault)) }
    }
  }

  protected def extractEither(result: Node): Either[Error, T]
}

object Reader {
  def apply[T <: models.Result](tag: String)(extractFn: Node => Either[Error, T]) = new Reader[T] {
    val responseTag = tag
    protected def extractEither(result: Node): Either[Error, T] = extractFn(result)
  }
}
