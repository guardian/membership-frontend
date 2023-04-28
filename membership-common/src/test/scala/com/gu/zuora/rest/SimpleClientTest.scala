package com.gu.zuora.rest
import com.gu.zuora.ZuoraRestConfig
import io.lemonlabs.uri.dsl._
import okhttp3.{MediaType, Protocol, Request, Response => OkResponse, ResponseBody}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.exceptions.TestFailedException
import play.api.libs.json.{Json, Reads}

import scalaz.{-\/, OptionT, Writer, \/}

class SimpleClientTest extends AnyFlatSpec {

  /* The writer monad lets us output two values - the actual value and a side "logging" value.
   * In these unit tests we want to inspect the requests issued by the client so we make our logging value a List[Request]
   * (We wrap the request in a list because the writer monad requires that you can log more than one value)
   */
  type W[A] = Writer[List[Request], A]

  /* to avoid having to construct a response we really want a Writer[List[Request], Option[Response]]
   * And to create that we can use the OptionT monad transformer to convert a Writer[List[Request], Option[A]] (not a monad)
   * into an OptionT[Writer[List[Request], _], A] (an OptionT monad)
   */
  type M[A] = OptionT[W, A]

  /* Hence the function that fulfills HTTP requests returns a response in an option (always None)
   * Nested within a writer which contains a List[Request], so we can see the request and skip providing a response
   */
  val noResponseRunner: Request => M[OkResponse] = a => OptionT[W, OkResponse](Writer(List(a), None))

  /*
   * After all that I've realised we do actually also want to test how responses get parsed
   * so lets also write a request running function that returns a response with a string body
   */
  def constRunner(body: String): Request => W[OkResponse] = a => {
    Writer(List(a),
      new OkResponse.Builder()
        .body(ResponseBody.create(body, MediaType.parse("application/json")))
        .protocol(Protocol.HTTP_2) // in my dreams :'(
        .request(a)
        .message("test")
        .code(200)
        .build()
    )
  }

  val config = ZuoraRestConfig("DEV", "https://example.com", "user", "pass")
  val noResponseClient = SimpleClient[M](config, noResponseRunner)

  case class TestClass(in: String, bar: Int)
  implicit val r: Reads[TestClass] = Json.reads[TestClass]

  "Simple rest client" should "Generate the right URL and auth headers for each http method" in {
    Seq(
      noResponseClient.get[String]("foo/bar"),
      noResponseClient.put[String, String]("foo/bar", "test"),
      noResponseClient.post[String, String]("foo/bar", "test")
    ).foreach{ r =>
      val sentRequest = r.run.written.last
      sentRequest.url().toString() should be("https://example.com/foo/bar")
      sentRequest.header("apiSecretAccessKey") should be("pass")
      sentRequest.header("apiAccessKeyId") should be("user")
    }
  }

  it should "include the body string if it fails to parse the response as JSON post" in {
    val badClient = SimpleClient[W](config, constRunner("bad json"))

    val result = badClient.post[String, Int]("foo/bar", "stuff")

    result.value match {
      case -\/(error) => error should include("bad json")
      case failed => throw new TestFailedException(failed.toString, 0)
    }
  }

  it should "include the body string if it fails to parse the response as JSON put" in {
    val badClient = SimpleClient[W](config, constRunner("bad json"))

    val result = badClient.put[String, Int]("foo/bar", "stuff")

    result.value match {
      case -\/(error) => error should include("bad json")
      case failed => throw new TestFailedException(failed.toString, 0)
    }
  }

  it should "Return the body string parsed into a JSObject if it does contain valid JSON" in {
    SimpleClient[W](config, constRunner("""{"in": "foo", "bar": 4}""")).post[String, TestClass]("foo/bar", "stuff").value should be(\/.right(TestClass("foo", 4)))
    SimpleClient[W](config, constRunner("""{"in": "foo", "bar": 4}""")).put[String, TestClass]("foo/bar", "stuff").value should be(\/.right(TestClass("foo", 4)))
  }
}
