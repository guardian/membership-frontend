package model

case class MembershipAccess(accessType: String) {
  val isMembershipAccess = accessType.equals("members-only")
  val isPaidAccess = accessType.equals("paid-members-only")
}
