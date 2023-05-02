package com.gu.salesforce.job

import org.specs2.mutable.Specification
import utils.Resource

import Implicits._
import com.gu.salesforce.ContactDeserializer.Keys

class ReaderTest extends Specification {
  "Reader" should {
    "deserialize JobInfo" in {
      val jobInfo = Resource.getXML("job-info.xml")
      jobInfoReader.extract(jobInfo) mustEqual JobInfo("75011000000XgXSAA0")
    }

    "deserialize BatchInfo" in {
      val batchInfo = Resource.getXML("batch-info.xml")
      batchInfoReader.extract(batchInfo) mustEqual BatchInfo("75111000000gUOCAA2", "75011000000XgXSAA0", "Queued", "")
    }

    "deserialize CompletedBatchList" in {
      val batchInfoList = Resource.getXML("batch-info-list-completed.xml")
      batchInfoListReader.extract(batchInfoList) mustEqual CompletedBatchList(Seq(
        BatchInfo("75111000000gUOCAA2", "75011000000XgXSAA0", "Completed", ""),
        BatchInfo("75111000000gUOCAA3", "75011000000XgXSAA1", "Completed", "")
      ))
    }

    "deserialize FailedBatchList" in {
      val batchInfoList = Resource.getXML("batch-info-list-failed.xml")
      batchInfoListReader.extract(batchInfoList) mustEqual FailedBatchList(
        BatchInfo("75111000000gUOCAA2", "75011000000XgXSAA0", "Failed", "Failed message")
      )
    }

    "deserialize InProgressBatchList" in {
      val batchInfoList = Resource.getXML("batch-info-list-in-progress.xml")
      batchInfoListReader.extract(batchInfoList) mustEqual InProcessBatchList()
    }

    "deserialize QueryResult" in {
      val queryResult = Resource.getXML("query-result.xml")
      queryResultReader.extract(queryResult) mustEqual QueryResult("75211000000EyhI")
    }

    "deserialize QueryRows with no results" in {
      val queryRows = Resource.getXML("query-rows-empty.xml")
      queryRowsReader.extract(queryRows) mustEqual QueryRows(Nil)
    }

    "deserialize QueryRows with results" in {
      val queryRows = Resource.getXML("query-rows-results.xml")
      val rows = queryRowsReader.extract(queryRows)

      rows.records.length mustEqual 1
      rows.records(0)(Keys.CONTACT_ID) mustEqual "0031100000ad2NpAAI"
      rows.records(0)(Keys.IDENTITY_ID) mustEqual "10000140"
    }
  }
}
