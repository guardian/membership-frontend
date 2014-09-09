package monitoring

object MembershipMetrics extends Metrics {
  val service = "Membership"

  def recordTiming(time: Long) {
    put("subscription-duration-ms", time)
  }
}
