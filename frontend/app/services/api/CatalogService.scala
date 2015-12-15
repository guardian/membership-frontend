package services.api

import com.gu.config.Membership
import com.gu.membership.model.TierPlan
import model.MembershipCatalog
import com.gu.membership.util.FutureSupplier
import scala.concurrent.Future

trait CatalogService {
  type  RatePlanId = String
  def membershipCatalog: FutureSupplier[MembershipCatalog]
  def getMembershipCatalog: Future[MembershipCatalog.Val[MembershipCatalog]]
  def productFamily: Membership
  def findProductRatePlanId(tierPlan: TierPlan): Future[RatePlanId]
}
