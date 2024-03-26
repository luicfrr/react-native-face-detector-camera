package com.facedetectorcamera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.facedetectorcamera.facedetector.FaceDetector
import com.facedetectorcamera.facedetector.FaceDetectorSettings
import com.facedetectorcamera.facedetector.toByteArray
import com.facedetectorcamera.records.CameraType
import com.facedetectorcamera.tasks.ResolveTakenPicture
import expo.modules.core.errors.ModuleDestroyedException
import expo.modules.interfaces.camera.CameraViewInterface
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.viewevent.EventDispatcher
import expo.modules.kotlin.views.ExpoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class CameraView(
    context: Context,
    appContext: AppContext
) : ExpoView(context, appContext),
    CameraViewInterface {
    private val currentActivity
        get() = appContext.currentActivity as? AppCompatActivity
            ?: throw Exceptions.MissingActivity()

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val providerFuture = ProcessCameraProvider.getInstance(context)
    private var imageCaptureUseCase: ImageCapture? = null
    private var imageAnalysisUseCase: ImageAnalysis? = null
    private var previewView = PreviewView(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    var lenFacing = CameraType.FRONT
        set(value) {
            field = value
            createCamera()
        }

    private val onCameraReady by EventDispatcher<Unit>()
    private val onMountError by EventDispatcher<CameraMountErrorEvent>()
    private val onFacesDetected by EventDispatcher<FacesDetectedEvent>(
        /**
         * Should events about detected faces coalesce, the best strategy will be
         * to ensure that events with different faces count are always being transmitted.
         */
        coalescingKey = { event -> (event.faces.size % Short.MAX_VALUE).toShort() }
    )
    private val onPictureSaved by EventDispatcher<PictureSavedEvent>(
        coalescingKey = { event ->
            val uriHash = event.data.getString("uri")?.hashCode() ?: -1
            (uriHash % Short.MAX_VALUE).toShort()
        }
    )

    // Scanning-related properties
    private var faceDetectorSettings = FaceDetectorSettings()
    private var shouldDetectFaces = false

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        val width = right - left
        val height = bottom - top

        previewView.layout(0, 0, width, height)
        postInvalidate(left, top, right, bottom)
    }

    override fun onViewAdded(child: View) {
        if (previewView === child) {
            return
        }

        removeView(previewView)
        addView(previewView, 0)
    }

    fun takePicture(options: PictureOptions, promise: Promise, cacheDirectory: File) {
        imageCaptureUseCase?.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val data = image.planes.toByteArray()

                    if (options.fastMode) {
                        promise.resolve(null)
                    }
                    cacheDirectory.let {
                        scope.launch {
                            ResolveTakenPicture(data, promise, options, it) { response: Bundle ->
                                onPictureSaved(response)
                            }.resolve()
                        }
                    }
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    promise.reject(CameraExceptions.ImageCaptureFailed())
                }
            }
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun createCamera() {
        Log.d("FaceDetector", "creating camera")
        providerFuture.addListener(
            {
                val cameraProvider: ProcessCameraProvider = providerFuture.get()

                Log.d("FaceDetector", "building preview")
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                Log.d("FaceDetector", "selecting camera")
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lenFacing.mapToCharacteristic())
                    .build()

                imageCaptureUseCase = ImageCapture.Builder().build()
                imageAnalysisUseCase = createImageAnalyzer()

                Log.d("FaceDetector", "applying useCases")
                val useCases = UseCaseGroup.Builder().apply {
                    addUseCase(preview)
                    imageCaptureUseCase?.let { addUseCase(it) }
                    imageAnalysisUseCase?.let { addUseCase(it) }
                }.build()

                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        currentActivity,
                        cameraSelector,
                        useCases
                    )
                    camera?.let {
                        observeCameraState(it.cameraInfo)
                    }
                    this.cameraProvider = cameraProvider
                } catch (e: Exception) {
                    onMountError(
                        CameraMountErrorEvent("Camera component could not be rendered - is there any other instance running?")
                    )
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun createImageAnalyzer(): ImageAnalysis =
        ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analyzer ->
                Log.d("FaceDetector", "analyzer creating")
                analyzer.setAnalyzer(
                    ContextCompat.getMainExecutor(context),
                    FaceDetector(faceDetectorSettings) {
                        onFacesDetected(it)
                    }
                )
            }

    private fun observeCameraState(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.observe(currentActivity) {
            when (it.type) {
                CameraState.Type.OPEN -> {
                    onCameraReady(Unit)
                }

                else -> {}
            }
        }
    }

    fun setShouldDetectFaces(shouldDetectFaces: Boolean) {
        this.shouldDetectFaces = shouldDetectFaces
        createCamera()
    }

    fun setFaceDetectorSettings(settings: Map<String, Any>) {
        faceDetectorSettings.setSettings(settings)
    }

    fun releaseCamera() {
        appContext.mainQueue.launch {
            cameraProvider?.unbindAll()
        }
    }

    private fun onFacesDetected(
        faces: List<Bundle>
    ) {
        Log.d("FaceDetector", "onFacesDetected private fun call")
        if (shouldDetectFaces) {
            Log.d("FaceDetector", "onFacesDetected event dispatcher call")
            onFacesDetected(
                FacesDetectedEvent(
                    faces,
                    id
                )
            )
        }
    }

    override fun setPreviewTexture(
        surfaceTexture: SurfaceTexture?
    ) = Unit

    override fun getPreviewSizeAsArray() = intArrayOf(
        previewView.width,
        previewView.height
    )

    init {
        previewView.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
            override fun onChildViewRemoved(parent: View?, child: View?) = Unit
            override fun onChildViewAdded(parent: View?, child: View?) {
                parent?.measure(
                    MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
                )
                parent?.layout(0, 0, parent.measuredWidth, parent.measuredHeight)
            }
        })
        addView(previewView)
    }

    fun onPictureSaved(response: Bundle) {
        onPictureSaved(
            PictureSavedEvent(
                response.getInt("id"),
                response.getBundle("data")!!
            )
        )
    }

    fun cancelCoroutineScope() {
        try {
            scope.cancel(ModuleDestroyedException())
        } catch (e: Exception) {
            Log.e(CameraModule.TAG, "The scope does not have a job in it")
        }
    }
}
