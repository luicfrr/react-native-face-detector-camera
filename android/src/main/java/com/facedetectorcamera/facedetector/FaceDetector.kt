package com.facedetectorcamera.facedetector

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.facedetectorcamera.facedetector.FaceDetectorUtils.rotateFaceX
import com.facedetectorcamera.facedetector.FaceDetectorUtils.serializeFace
import com.google.mlkit.vision.common.InputImage

class FaceDetector(
    private val settings: FaceDetectorSettings,
    val onComplete: (ArrayList<Bundle>) -> Unit
) : ImageAnalysis.Analyzer {
    // device display data
    // divide size by density so we can have real screen px size
    private var density = Resources.getSystem().displayMetrics.density
    private var windowWidth = Resources.getSystem().displayMetrics.widthPixels / density
    private var windowHeight = Resources.getSystem().displayMetrics.heightPixels / density

    private var faceDetector = settings.getFaceDetector()
    private var minDetectionInterval = settings.minDetectionInterval
    private var lastDetectionMillis: Long = 0

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(
        imageProxy: ImageProxy
    ) {
        Log.d("FaceDetector", "analyze method called")
        if (settings.faceDetectorTaskLock) {
            Log.d("FaceDetector", "faceDetectorTaskLock is true - skipping")
            finishFaceDetection(imageProxy)
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            Log.d("FaceDetector", "mediaImage is null")
            finishFaceDetection(imageProxy)
            return
        }

        if (minDetectionInterval > 0 && !minIntervalPassed()) {
            Log.d("FaceDetector", "min intervall skip")
            finishFaceDetection(imageProxy)
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)
        val scaleX: Double
        val scaleY: Double
        if (rotation == 270 || rotation == 90) {
            scaleX = windowWidth.toDouble() / image.height
            scaleY = windowHeight.toDouble() / image.width
        } else {
            scaleX = windowWidth.toDouble() / image.width
            scaleY = windowHeight.toDouble() / image.height
        }

        Log.d("FaceDetector", "windowW: $windowWidth, windowH: $windowHeight, density: $density")
        Log.d("FaceDetector", "imageW: " + image.width + ", imageH: " + image.height)
        Log.d("FaceDetector", "scaleX: $scaleX, scaleY: $scaleY rotation: $rotation")

        settings.lockFaceDetectorTask()
        lastDetectionMillis = System.currentTimeMillis()
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                val facesArray = ArrayList<Bundle>()
                faces?.forEach { face ->
                    var result = serializeFace(face, scaleX, scaleY)
                    result = rotateFaceX(
                        result,
                        if (rotation == 270 || rotation == 90) image.height else image.width,
                        scaleX
                    )
                    facesArray.add(result)
                }

                Log.d("FaceDetector", "faces detected - returning")
                onComplete(facesArray)
            }
            .addOnFailureListener {
                Log.d("FaceDetector", it.cause?.message ?: "Face detection failed")
                finishFaceDetection(imageProxy)
            }
            .addOnCompleteListener {
                Log.d("FaceDetector", "face detection addOnCompleteListener task finished")
                finishFaceDetection(imageProxy)
            }
    }

    private fun finishFaceDetection(
        imageProxy: ImageProxy
    ) {
        imageProxy.close()
        settings.releaseFaceDetectorTask()
    }

    private fun minIntervalPassed() = (
            lastDetectionMillis + minDetectionInterval
            ) < System.currentTimeMillis()
}
