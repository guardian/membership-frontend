package model

case class Video(
  srcUrl: com.netaporter.uri.Uri,
  posterImage: Option[model.ResponsiveImageGroup]
)
