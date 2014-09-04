package monitoring

object IdentityApiMetrics extends StatusMetrics {

  val namespace = "Identity API"

  def recordGetPasswordExistsResponse(status: Int) {
    putResponseCode("identity-get-user-password-exists-response", status)
  }

  def recordGetResponse(status: Int) {
    putResponseCode("identity-get-user-details-response", status)
  }

  def recordUpdateUserDetailsPostResponse(status : Int) {
    putResponseCode("identity-update-user-details-response", status)
  }

  def recordPasswordUpdatePostResponse(status: Int) {
    putResponseCode("identity-password-update-response", status)
  }
}

abstract class StatusMetrics extends CloudWatch {
  val namespace : String

  def putResponseCode(name: String, status: Int) {
    val statusClass = status / 100
    val metrics = Map(s"$name-${statusClass}XX" -> 1.00)
    put(namespace, metrics)
  }
}