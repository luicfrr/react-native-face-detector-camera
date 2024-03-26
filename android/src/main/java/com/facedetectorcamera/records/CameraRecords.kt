package com.facedetectorcamera.records

import android.hardware.camera2.CameraMetadata
import expo.modules.kotlin.types.Enumerable

enum class CameraType(val value: String) : Enumerable {
    FRONT("front");

    fun mapToCharacteristic() = when (this) {
        FRONT -> CameraMetadata.LENS_FACING_FRONT
    }
}