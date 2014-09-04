package monitoring

object IdentityApiMetrics extends StatusMetrics {

  val namespace = "Identity API"

  def putPasswordsExistsResponse(status: Int) {
    putResponseCode("identity-get-user-password-exists-response", status)
  }

  def putUserDetailsResponse(status: Int) {
    putResponseCode("identity-get-user-details-response", status)
  }

  def putUpdateUserDetailsResponse(status : Int) {
    putResponseCode("identity-update-user-details-response", status)
  }

  def putPasswordUpdateResponse(status: Int) {
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