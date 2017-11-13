package filters

import javax.inject.Inject

import akka.stream.Materializer
import play.filters.gzip.GzipFilter

class Gzipper @Inject()(implicit val mat: Materializer) extends GzipFilter(
  shouldGzip = (req, resp) => !resp.header.headers.get("Content-Type").exists(_.startsWith("image/"))
)
