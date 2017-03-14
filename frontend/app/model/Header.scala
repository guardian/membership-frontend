package model

sealed trait Header

object Header {

	case object DefaultHeader extends Header
	case object SimpleHeader extends Header
	case object BundlesHeader extends Header

}
