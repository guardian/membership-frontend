package monitoring

import com.gu.monitoring.StatusMetrics

object IdentityApiMetrics extends Metrics with StatusMetrics {

  val namespace = "Identity API"

  def recordGetPasswordExistsResponse(status: Int) {
    putResponseCode(namespace, "identity-get-user-password-exists-response", status)
  }

  def recordGetResponse(status: Int) {
    putResponseCode(namespace, "identity-get-user-details-response", status)
  }

  def recordUpdateUserDetailsPostResponse(status : Int) {
    putResponseCode(namespace, "identity-update-user-details-response", status)
  }

  def recordPasswordUpdatePostResponse(status: Int) {
    putResponseCode(namespace, "identity-password-update-response", status)
  }
}