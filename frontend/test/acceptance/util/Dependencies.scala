package acceptance.util

import java.security.cert.X509Certificate
import javax.net.ssl.{X509TrustManager, SSLContext, SSLSession, HostnameVerifier}
import com.gu.lib.okhttpscala._
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request.Builder
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import scala.language.postfixOps

object Dependencies {

  object MembershipFrontend extends Availability {
    val url = Config.baseUrl
  }

  object IdentityFrontend extends Availability {
    val url = s"${Config.identityFrontendUrl}/signin"
  }

  trait Availability {
    val url: String
    def isAvailable: Boolean = {
      val request = new Builder().url(url).build()
      Try(Await.result(insecureClient.execute(request), 30 second).isSuccessful).getOrElse(false)
    }
  }

  private val client = new OkHttpClient()
  private val insecureClient = InsecureOkHttpClient()

 /*
  * Get OkHttpClient which ignores all SSL errors.
  *
  * Needed when running against local servers which might not have a valid cert.
  * https://stackoverflow.com/questions/25509296/trusting-all-certificates-with-okhttp
  */
  private object InsecureOkHttpClient {

   def apply() =
     new OkHttpClient().setSslSocketFactory(SSL.InsecureSocketFactory).setHostnameVerifier(new HostnameVerifier {
       override def verify(hostname: String, sslSession: SSLSession): Boolean = true
     })

    private object SSL {
      val InsecureSocketFactory = {
        val sslcontext = SSLContext.getInstance("TLS")
        sslcontext.init(null, Array(TrustEveryoneTrustManager), null)
        sslcontext.getSocketFactory
      }

      object TrustEveryoneTrustManager extends X509TrustManager {
        def checkClientTrusted(chain: Array[X509Certificate], authType: String) {}

        def checkServerTrusted(chain: Array[X509Certificate], authType: String) {}

        val getAcceptedIssuers = new Array[X509Certificate](0)
      }
    }
  }
}
