package com.facedetectorcamera.tasks

import android.os.Bundle

fun interface PictureSavedDelegate {
  fun onPictureSaved(response: Bundle)
}

