package model

case class Video(
  srcUrl: io.lemonlabs.uri.Uri,
  posterImage: Option[com.gu.memsub.images.ResponsiveImageGroup],
  autoplay: Boolean = false,
  loop: Boolean = false
)
