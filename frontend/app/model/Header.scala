package model

sealed trait Header

object Header {

	case object DefaultHeader extends Header
	case object SimpleHeader extends Header
	case object BundlesHeader extends Header
  case object MasterClassesHeader extends Header
  case object LiveHeader extends Header
  case object GuardianHeader extends Header
  case object PatronsHeader extends Header

}
