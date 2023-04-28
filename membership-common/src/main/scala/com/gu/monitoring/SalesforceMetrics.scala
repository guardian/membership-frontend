package com.gu.monitoring

import com.amazonaws.regions.{Regions, Region}


class SalesforceMetrics(val stage: String, val application: String) extends CloudWatch
  with StatusMetrics
  with RequestMetrics
  with AuthenticationMetrics {

  val region = Region.getRegion(Regions.EU_WEST_1)
  val service = "Salesforce"

  def recordRequest() {
    putRequest
  }

  def recordResponse(status: Int, responseMethod: String) {
    putResponseCode(status, responseMethod)
  }

  def recordAuthenticationError() {
    putAuthenticationError
  }
}