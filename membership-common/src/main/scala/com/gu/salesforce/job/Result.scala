package com.gu.salesforce.job

sealed trait Result

case class Error(msg: String) extends Throwable with Result {
  override def getMessage: String = msg
}

// https://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_reference_jobinfo.htm
case class JobInfo(id: String) extends Result

// https://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_reference_batchinfo.htm
case class BatchInfo(id: String, jobId: String, state: String, stateMessage: String) extends Result {
  val completed = state == "Completed"
  val failed = state == "Failed"
}

// https://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_batches_get_info_all.htm
sealed trait BatchInfoList extends Result
case class InProcessBatchList() extends BatchInfoList
case class CompletedBatchList(batches: Seq[BatchInfo]) extends BatchInfoList
case class FailedBatchList(batch: BatchInfo) extends BatchInfoList

// https://www.salesforce.com/us/developer/docs/api_asynch/Content/asynch_api_bulk_query.htm
case class QueryResult(id: String) extends Result
case class QueryRows(records: Seq[Map[String, String]]) extends Result
