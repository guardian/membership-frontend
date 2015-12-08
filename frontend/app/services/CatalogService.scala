package services

import com.gu.membership.model.TierPlan
import com.gu.membership.zuora
import scala.concurrent.ExecutionContext.Implicits.global
import com.gu.config.Membership
import model.MembershipCatalog
import com.gu.membership.util.FutureSupplier
import scala.concurrent.Future
import com.gu.membership.touchpoint.TouchpointBackendConfig.BackendType

class CatalogService(zuoraRestClient: zuora.rest.Client,
                     val productFamily: Membership
                     )(implicit backendType: BackendType) extends api.CatalogService {
  override val membershipCatalog: FutureSupplier[MembershipCatalog] = new FutureSupplier[MembershipCatalog](
    productRatePlans.map(MembershipCatalog.unsafeFromZuora(productFamily))
  )

  override def getMembershipCatalog: Future[MembershipCatalog.Val[MembershipCatalog]] =
    productRatePlans.map(MembershipCatalog.fromZuora(productFamily))

  override def findRatePlanId(newTierPlan: TierPlan): Future[RatePlanId] = {
    membershipCatalog.get().map(_.ratePlanId(newTierPlan))
  }

  private def productRatePlans: Future[Seq[zuora.rest.ProductRatePlan]] =
    zuoraRestClient.productCatalog.map(_.products.flatMap(_.productRatePlans))

}
