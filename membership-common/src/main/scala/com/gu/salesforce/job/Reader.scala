package com.gu.salesforce.job

import okhttp3.Response

import scala.util.{Failure, Success, Try}
import scala.xml.{XML, Node}

/**
 * Converts the body of a Response to an object of type T.
 * Assumes that the body is XML
 *
 * @tparam T The type of result this Reader returns
 */
sealed trait Reader[T <: Result] {
  /**
   * Checks the response status code and attempts to convert its body to XML
   *
   * @param response The Response object
   * @return A Left if the response was invalid or a Right with the object of type T
   */
  def read(response: Response): Either[Error, T] = {
    response.code() match {
      case 200 | 201 =>
        Try { XML.load(response.body().byteStream()) } match {
          case Success(xml) => Right(extract(xml))
          case Failure(ex) => Left(Error(s"Failed to extract XML, ${ex.getMessage}"))
        }

      case code => Left(Error(s"Unexpected response code $code"))
    }
  }

  /**
   * Converts the XML to the object of type T
   * No error checking is done, it is assumed that the correct XML tree exists
   *
   * @param node The XML to read
   * @return The resulting object
   */
  def extract(node: Node): T
}

object Reader {
  def apply[T <: Result](fn: Node => T) = new Reader[T] {
    def extract(node: Node): T = fn(node)
  }
}

object Implicits {
  implicit val jobInfoReader = Reader { node =>
    JobInfo((node \ "id").text)
  }

  implicit val batchInfoReader = Reader { node =>
    BatchInfo((node \ "id").text, (node \ "jobId").text, (node \ "state").text, (node \ "stateMessage").text)
  }

  implicit val batchInfoListReader = Reader[BatchInfoList] { node =>
    val batches = (node \ "batchInfo").map(batchInfoReader.extract)

    batches.find(_.failed).fold {
      if (batches.forall(_.completed)) {
        CompletedBatchList(batches)
      } else {
        InProcessBatchList()
      }
    }(FailedBatchList)
  }

  implicit val queryResultReader = Reader { node =>
    QueryResult((node \ "result").text)
  }

  implicit val queryRowsReader = Reader { node =>
    val records = (node \ "records").map { record =>
      record.child.map { child => (child.label, child.text) }.toMap
    }
    QueryRows(records)
  }
}
