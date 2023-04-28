package com.gu.memsub.subsv2.services
import com.gu.config.SubsV2ProductIds
import com.gu.memsub.Subscription.ProductId
import com.gu.zuora.ZuoraRestConfig
import com.gu.zuora.rest.SimpleClient
import okhttp3._
import org.specs2.mutable.Specification
import com.gu.memsub.subsv2.Fixtures._
import io.lemonlabs.uri.dsl._
import com.typesafe.config.ConfigFactory
import utils.Resource

import scalaz.Id._
import scalaz.\/

class CatalogServiceTest extends Specification {

  "Catalog service" should {

    "Read a catalog in UAT" in {
      val cats = new CatalogService[Id](productIds, FetchCatalog.fromZuoraApi(CatalogServiceTest.client("rest/CatalogUat.json")), identity, "UAT")
      cats.catalog.map(_ => true) mustEqual \/.right(true)
    }

    "Read a catalog in DEV with the config product IDs" in {
      val dev = ConfigFactory.parseResources("touchpoint.DEV.conf")
      val ids = SubsV2ProductIds(dev.getConfig("touchpoint.backend.environments.DEV.zuora.productIds"))
      val cats = new CatalogService[Id](ids, FetchCatalog.fromZuoraApi(CatalogServiceTest.client("rest/Catalog.json")), identity, "DEV")
      cats.catalog.map(_ => true) mustEqual \/.right(true)
    }
  }
}

object CatalogServiceTest {

  def client(path: String) = {
    val runner = (r: Request) =>
      new Response.Builder()
        .request(r)
        .message("test")
        .code(200)
        .body(ResponseBody.create(MediaType.parse("application/json"), Resource.getJson(path).toString))
        .protocol(Protocol.HTTP_1_1).build()

    val restConfig = ZuoraRestConfig("foo", "http://localhost", "joe", "public")
    new SimpleClient[Id](restConfig, runner)
  }

}
