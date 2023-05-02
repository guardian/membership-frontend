package com.gu.salesforce.job

/**
 * Defines an HTTP request which should return an object of type T
 *
 * @tparam T The expected resultant object
 */
sealed trait Action[T <: Result] {
  /**
   * The URL to send the request to
   */
  val url: String

  def name = getClass.getSimpleName
}

/**
 * A GET request
 */
sealed trait ReadAction[T <: Result] extends Action[T]

/**
 * A POST request
 */
sealed trait WriteAction[T <: Result] extends Action[T] {
  // Can't make this Elem because queries have to be plain text (despite Content-Type
  // having to be application/xml
  val body: String
}

case class JobCreate(op: String, objType: String) extends WriteAction[JobInfo] {
  val url = "job"

  val body =
    <jobInfo xmlns="http://www.force.com/2009/06/asyncapi/dataload">
      <operation>{op}</operation>
      <object>{objType}</object>
      <contentType>XML</contentType>
    </jobInfo>.toString()
}

case class JobClose(job: JobInfo) extends WriteAction[JobInfo] {
  val url = s"job/${job.id}"
  val body =
    <jobInfo xmlns="http://www.force.com/2009/06/asyncapi/dataload">
      <state>Closed</state>
    </jobInfo>.toString()
}

case class JobGetBatchList(job: JobInfo) extends ReadAction[BatchInfoList] {
  val url = s"job/${job.id}/batch"
}

case class QueryCreate(job: JobInfo, query: String) extends WriteAction[BatchInfo] {
  val url = s"job/${job.id}/batch"
  val body = query
}

case class QueryGetResult(batch: BatchInfo) extends ReadAction[QueryResult] {
  val url = s"job/${batch.jobId}/batch/${batch.id}/result"
}

case class QueryGetRows(batch: BatchInfo, query: QueryResult) extends ReadAction[QueryRows] {
  val url = s"job/${batch.jobId}/batch/${batch.id}/result/${query.id}"
}
