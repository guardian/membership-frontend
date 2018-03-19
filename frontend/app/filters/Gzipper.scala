package filters

import akka.stream.Materializer
import play.filters.gzip.GzipFilter

class Gzipper(implicit val mat: Materializer) extends GzipFilter(
  shouldGzip = (req, resp) => !resp.header.headers.get("Content-Type").exists(_.startsWith("image/"))
)
