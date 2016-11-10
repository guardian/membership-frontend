package model

case class Video(
  srcUrl: com.netaporter.uri.Uri,
  posterImage: Option[com.gu.memsub.images.ResponsiveImageGroup],
  autoplay: Boolean = false,
  loop: Boolean = false
)
