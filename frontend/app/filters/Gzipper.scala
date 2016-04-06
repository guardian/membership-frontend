package filters

import play.filters.gzip.GzipFilter

object Gzipper
    extends GzipFilter(
        shouldGzip = (req, resp) =>
            !resp.headers.get("Content-Type").exists(_.startsWith("image/"))
    )
