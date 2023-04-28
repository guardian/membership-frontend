package com.gu.lib

import java.io.IOException
import okhttp3.{Callback, OkHttpClient, Request, Response, Call}
import com.gu.monitoring.SafeLogger
import scala.concurrent.{Future, Promise}

package object okhttpscala {

  implicit class RickOkHttpClient(client: OkHttpClient) {

    def execute(request: Request): Future[Response] = {
      val p = Promise[Response]()

      client.newCall(request).enqueue(new Callback {
        override def onFailure(call: Call, e: IOException): Unit = {
          val sanitizedUrl = s"${request.url().uri().getHost}${request.url().uri().getPath}" // don't log query string
          SafeLogger.warn(s"okhttp request failure: ${request.method()} $sanitizedUrl", e)
          p.failure(e)
        }

        override def onResponse(call: Call, response: Response): Unit = {
          p.success(response)
        }
      })

      p.future
    }

  }
}
