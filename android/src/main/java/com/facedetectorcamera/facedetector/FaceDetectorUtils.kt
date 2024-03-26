package com.facedetectorcamera.facedetector

import android.graphics.PointF
import android.os.Bundle
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.nio.ByteBuffer

private const val DETECT_LANDMARKS_KEY = "detectLandmarks"
private const val MIN_INTERVAL_MILLIS_KEY = "minDetectionInterval"
private const val MODE_KEY = "mode"
private const val RUN_CLASSIFICATIONS_KEY = "runClassifications"
private const val TRACKING_KEY = "tracking"

// constants
const val NO_CLASSIFICATIONS = FaceDetectorOptions.CLASSIFICATION_MODE_NONE
const val NO_LANDMARKS = FaceDetectorOptions.LANDMARK_MODE_NONE
const val FAST_MODE = FaceDetectorOptions.PERFORMANCE_MODE_FAST

class FaceDetectorSettings() {
    private var faceDetector: FaceDetector? = null
    var minDetectionInterval: Long = 0
    // Concurrency lock for scanners to avoid flooding the runtime
    @Volatile
    var faceDetectorTaskLock = false
    private val minFaceSize = 0.15f
    private var tracking = false
        set(value) {
            if (field != value) {
                release()
                field = value
            }
        }
    private var landmarkType = NO_LANDMARKS
        set(value) {
            if (field != value) {
                release()
                field = value
            }
        }
    private var classificationType = NO_CLASSIFICATIONS
        set(value) {
            if (field != value) {
                release()
                field = value
            }
        }
    private var mode = FAST_MODE
        set(value) {
            if (field != value) {
                release()
                field = value
            }
        }

    fun setSettings(settings: Map<String, Any>) {
        if (settings[MODE_KEY] is Number) {
            mode = (settings[MODE_KEY] as Number).toInt()
        }
        if (settings[DETECT_LANDMARKS_KEY] is Number) {
            landmarkType = (settings[DETECT_LANDMARKS_KEY] as Number).toInt()
        }
        if (settings[TRACKING_KEY] is Boolean) {
            tracking = (settings[TRACKING_KEY] as Boolean)
        }
        if (settings[RUN_CLASSIFICATIONS_KEY] is Number) {
            classificationType = (settings[RUN_CLASSIFICATIONS_KEY] as Number).toInt()
        }
        minDetectionInterval = if (settings[MIN_INTERVAL_MILLIS_KEY] is Number) {
            (settings[MIN_INTERVAL_MILLIS_KEY] as Number).toInt().toLong()
        } else {
            0
        }
    }

    private fun createOptions(): FaceDetectorOptions {
        val builder = FaceDetectorOptions.Builder()
            .setClassificationMode(classificationType)
            .setLandmarkMode(landmarkType)
            .setPerformanceMode(mode)
            .setMinFaceSize(minFaceSize)
        if (tracking) {
            builder.enableTracking()
        }
        return builder.build()
    }

    fun getFaceDetector(): FaceDetector {
        if(faceDetector == null) {
            faceDetector = FaceDetection.getClient(createOptions())
        }

        return faceDetector as FaceDetector
    }

    fun lockFaceDetectorTask() {
        faceDetectorTaskLock = true
    }

    fun releaseFaceDetectorTask() {
        faceDetectorTaskLock = false
    }

    private fun release() {
        faceDetector = null
    }
}

object FaceDetectorUtils {
    @JvmStatic
    @JvmOverloads
    fun serializeFace(face: Face, scaleX: Double = 1.0, scaleY: Double = 1.0): Bundle {
        val encodedFace = Bundle().apply {
            face.trackingId?.let { putInt("faceID", it) }
            putDouble("rollAngle", face.headEulerAngleZ.toDouble())
            putDouble("yawAngle", face.headEulerAngleY.toDouble())

            face.smilingProbability?.let {
                if (it >= 0) {
                    putDouble("smilingProbability", it.toDouble())
                }
            }
            face.leftEyeOpenProbability?.let {
                if (it >= 0) {
                    putDouble("leftEyeOpenProbability", it.toDouble())
                }
            }

            face.rightEyeOpenProbability?.let {
                if (it >= 0) {
                    putDouble("rightEyeOpenProbability", it.toDouble())
                }
            }

            LandmarkId.values()
                .forEach { id ->
                    face.getLandmark(id.id)?.let { faceLandmark ->
                        putBundle(id.name, mapFromPoint(faceLandmark.position, scaleX, scaleY))
                    }
                }

            val box = face.boundingBox
            val origin = Bundle(2).apply {
                putDouble("x", box.left * scaleX)
                putDouble("y", box.top * scaleY)
            }

            val size = Bundle(2).apply {
                putDouble("width", (box.right - box.left) * scaleX)
                putDouble("height", (box.bottom - box.top) * scaleY)
            }

            val bounds = Bundle(2).apply {
                putBundle("origin", origin)
                putBundle("size", size)
            }
            putBundle("bounds", bounds)
        }
        return mirrorRollAngle(encodedFace)
    }

    @JvmStatic
    fun rotateFaceX(face: Bundle, sourceWidth: Int, scaleX: Double): Bundle {
        val faceBounds = face.getBundle("bounds") as Bundle
        val oldOrigin = faceBounds.getBundle("origin")
        val mirroredOrigin = positionMirroredHorizontally(oldOrigin, sourceWidth, scaleX)
        val translateX = -(faceBounds.getBundle("size") as Bundle).getDouble("width")
        val translatedMirroredOrigin = positionTranslatedHorizontally(mirroredOrigin, translateX)
        val newBounds = Bundle(faceBounds).apply {
            putBundle("origin", translatedMirroredOrigin)
        }
        face.apply {
            LandmarkId.values().forEach { id ->
                face.getBundle(id.name)?.let { landmark ->
                    val mirroredPosition = positionMirroredHorizontally(landmark, sourceWidth, scaleX)
                    putBundle(id.name, mirroredPosition)
                }
            }
            putBundle("bounds", newBounds)
        }
        return mirrorYawAngle(mirrorRollAngle(face))
    }

    private fun mirrorRollAngle(face: Bundle) = face.apply {
        putDouble("rollAngle", (-face.getDouble("rollAngle") + 360) % 360)
    }

    private fun mirrorYawAngle(face: Bundle) = face.apply {
        putDouble("yawAngle", (-face.getDouble("yawAngle") + 360) % 360)
    }

    private fun mapFromPoint(point: PointF, scaleX: Double, scaleY: Double) = Bundle().apply {
        putDouble("x", point.x * scaleX)
        putDouble("y", point.y * scaleY)
    }

    private fun positionTranslatedHorizontally(position: Bundle, translateX: Double) =
        Bundle(position).apply {
            putDouble("x", position.getDouble("x") + translateX)
        }

    private fun positionMirroredHorizontally(position: Bundle?, containerWidth: Int, scaleX: Double) =
        Bundle(position).apply {
            putDouble("x", valueMirroredHorizontally(position!!.getDouble("x"), containerWidth, scaleX))
        }

    private fun valueMirroredHorizontally(elementX: Double, containerWidth: Int, scaleX: Double) =
        -elementX + containerWidth * scaleX

    // All the landmarks reported by Google Mobile Vision in constants' order.
    // https://developers.google.com/android/reference/com/google/mlkit/vision/face/FaceLandmark
    private enum class LandmarkId(val id: Int, val landmarkName: String) {
        BOTTOM_MOUTH(FaceLandmark.MOUTH_BOTTOM, "bottomMouthPosition"),
        RIGHT_MOUTH(FaceLandmark.MOUTH_RIGHT, "rightMouthPosition"),
        LEFT_MOUTH(FaceLandmark.MOUTH_LEFT, "leftMouthPosition"),
        LEFT_CHEEK(FaceLandmark.LEFT_CHEEK, "leftCheekPosition"),
        RIGHT_EYE(FaceLandmark.RIGHT_EYE, "rightEyePosition"),
        LEFT_EYE(FaceLandmark.LEFT_EYE, "leftEyePosition"),
        LEFT_EAR(FaceLandmark.LEFT_EAR, "leftEarPosition"),
        RIGHT_CHEEK(FaceLandmark.RIGHT_CHEEK, "rightCheekPosition"),
        RIGHT_EAR(FaceLandmark.RIGHT_EAR, "rightEarPosition"),
        NOSE_BASE(FaceLandmark.NOSE_BASE, "noseBasePosition")
    }
}

private fun ByteBuffer.toByteArray(): ByteArray {
    rewind()
    val data = ByteArray(remaining())
    get(data)
    return data
}

fun Array<ImageProxy.PlaneProxy>.toByteArray() = this.fold(mutableListOf<Byte>()) { acc, plane ->
    acc.addAll(plane.buffer.toByteArray().toList())
    acc
}.toByteArray()
