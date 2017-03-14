package model

sealed trait Footer

object Footer {

	case object DefaultFooter extends Footer
	case object SimpleFooter extends Footer
	case object BundlesFooter extends Footer

}
