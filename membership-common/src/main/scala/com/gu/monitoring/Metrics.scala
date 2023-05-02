package com.gu.monitoring


trait StatusMetrics extends CloudWatch {
  def putResponseCode(status: Int, responseMethod: String) {
    val statusClass = status / 100
    put(s"${statusClass}XX-response-code", 1, responseMethod)
  }
}

trait RequestMetrics extends CloudWatch {
  def putRequest {
    put("request-count", 1)
  }
}

trait AuthenticationMetrics extends CloudWatch {
  def putAuthenticationError {
    put("auth-error", 1)
  }
}

object CloudWatchHealth {
  var hasPushedMetricSuccessfully = false
}
