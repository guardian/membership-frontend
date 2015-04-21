package model

case class MembershipAccess(accessType: String) {
  val isMembersOnly = accessType.equals("members-only")
  val isPaidMembersOnly = accessType.equals("paid-members-only")
}
