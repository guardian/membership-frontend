package utils

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import java.util.concurrent.{Future => JFuture}

import scala.concurrent.{Future, Promise}

class AwsAsyncHandler[Request <: AmazonWebServiceRequest, Response] extends AsyncHandler[Request, Response] {
  private val promise = Promise[Response]()

  override def onError(exception: Exception): Unit = promise.failure(exception)

  override def onSuccess(request: Request, result: Response): Unit = promise.success(result)

  def future = promise.future
}

object AwsAsync {

  def apply[Request <: AmazonWebServiceRequest, Response](
                                                           f: (Request, AsyncHandler[Request, Response]) => JFuture[Response],
                                                           request: Request
                                                         ): Future[Response] = {
    val handler = new AwsAsyncHandler[Request, Response]
    f(request, handler)
    handler.future
  }
}
