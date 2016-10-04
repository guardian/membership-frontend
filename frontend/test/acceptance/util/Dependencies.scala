package acceptance.util

import java.security.cert.X509Certificate
import javax.net.ssl.{HostnameVerifier, SSLContext, SSLSession, X509TrustManager}

import com.gu.lib.okhttpscala._
import okhttp3.OkHttpClient
import okhttp3.Request.Builder

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

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
     client.newBuilder().sslSocketFactory(SSL.InsecureSocketFactory).hostnameVerifier(new HostnameVerifier {
       override def verify(hostname: String, sslSession: SSLSession): Boolean = true
     }).build()

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
