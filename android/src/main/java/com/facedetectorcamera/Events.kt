package com.facedetectorcamera

import android.os.Bundle
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

data class CameraMountErrorEvent(
  @Field val message: String
) : Record

data class FacesDetectedEvent(
  @Field val faces: List<Bundle>,
  @Field val target: Int
) : Record

data class PictureSavedEvent(
  @Field val id: Int,
  @Field val data: Bundle
) : Record
