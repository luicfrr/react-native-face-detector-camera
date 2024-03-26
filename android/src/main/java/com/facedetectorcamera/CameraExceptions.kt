package com.facedetectorcamera

import expo.modules.kotlin.exception.CodedException

class CameraExceptions {
  class ImageCaptureFailed : CodedException(message = "Failed to capture image")
}
