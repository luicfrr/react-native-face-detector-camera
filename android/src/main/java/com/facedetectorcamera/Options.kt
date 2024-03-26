package com.facedetectorcamera

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

class PictureOptions : Record {
  @Field val quality: Double = 1.0

  @Field val base64: Boolean = false

  @Field val exif: Boolean = false

  @Field val additionalExif: Map<String, Any>? = null

  @Field val skipProcessing: Boolean = false

  @Field val fastMode: Boolean = false

  @Field val id: Int? = null

  @Field val maxDownsampling: Int = 1
}
