package monitoring

object IdentityApiMetrics extends CloudWatch {

  def putPasswordsExistsResponse(status: Int) {
    putStatus("identity-get-user-password-exists-response", status)
  }

  def putUserDetailsResponse(status: Int) {
    putStatus("identity-get-user-details-response", status)
  }

  def putUpdateUserDetailsResponse(status : Int) {
    putStatus("identity-update-user-details-response", status)
  }

  def putPasswordUpdateResponse(status: Int) {
    putStatus("identity-password-update-response", status)
  }

  private def putStatus(name: String, status: Int) {
    val statusClass = status / 100
    val metrics = Map(s"$name-${statusClass}XX" -> 1.00)
    put("Identity API", metrics)
  }
}