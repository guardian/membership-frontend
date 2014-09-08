package monitoring

import com.gu.monitoring.StatusMetrics

object IdentityApiMetrics extends Metrics with StatusMetrics {

  val service = "Identity API"

  def recordGetPasswordExistsResponse(status: Int) {
    putResponseCode(status, "GET")
  }

  def recordGetResponse(status: Int) {
    putResponseCode(status, "GET")
  }

  def recordUpdateUserDetailsPostResponse(status : Int) {
    putResponseCode(status, "POST")
  }

  def recordPasswordUpdatePostResponse(status: Int) {
    putResponseCode(status, "POST")
  }
}