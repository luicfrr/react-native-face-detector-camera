import UIKit
import ExpoModulesCore
import CoreMotion

public class CameraView: ExpoView, EXCameraInterface, EXAppLifecycleListener, AVCapturePhotoCaptureDelegate {
  public var session = AVCaptureSession()
  public var sessionQueue = DispatchQueue(label: "captureSessionQueue")

  // Legacy Modules
  private var lifecycleManager: EXAppLifecycleService?
  private var permissionsManager: EXPermissionsInterface?

  // Properties
  private var faceDetector: FaceDetectorManagerInterface?
  private var previewLayer = PreviewView()
  private var isValidVideoOptions = true
  private var photoCaptureOptions: TakePictureOptions?
  private var errorNotification: NSObjectProtocol?
  // private var physicalOrientation: UIDeviceOrientation = .unknown
  private var motionManager: CMMotionManager = {
    let mm = CMMotionManager()
    mm.accelerometerUpdateInterval = 0.2
    mm.gyroUpdateInterval = 0.2
    return mm
  }()

  // Property Observers
  var isDetectingFaces = false {
    didSet {
      if let faceDetector {
        faceDetector.setIsEnabled(isDetectingFaces)
      } else if isDetectingFaces {
        log.error("FaceDetector module not found.")
      }
    }
  }

  var presetCamera = AVCaptureDevice.Position.front {
    didSet {
      updateType()
    }
  }

  // Session Inputs and Outputs
  private var photoOutput: AVCapturePhotoOutput?
  private var captureDeviceInput: AVCaptureDeviceInput?

  // Promises
  private var photoCapturedPromise: Promise?

  // Events
  let onCameraReady = EventDispatcher()
  let onMountError = EventDispatcher()
  let onFacesDetected = EventDispatcher()
  let onFaceDetectionError = EventDispatcher()
  let onPictureSaved = EventDispatcher()

  private var deviceOrientation: UIInterfaceOrientation {
    window?.windowScene?.interfaceOrientation ?? .unknown
  }

  required init(appContext: AppContext? = nil) {
    super.init(appContext: appContext)
    faceDetector = createFaceDetectorManager()
    lifecycleManager = appContext?.legacyModule(
      implementing: EXAppLifecycleService.self
    )
    permissionsManager = appContext?.legacyModule(
      implementing: EXPermissionsInterface.self
    )
    #if !targetEnvironment(simulator)
    setupPreview()
    #endif
    UIDevice.current.beginGeneratingDeviceOrientationNotifications()
    initializeCaptureSessionInput()
    startSession()
    NotificationCenter.default.addObserver(
      self,
      selector: #selector(orientationChanged(notification:)),
      name: UIDevice.orientationDidChangeNotification,
      object: nil)
    lifecycleManager?.register(self)
  }

  private func setupPreview() {
    previewLayer.videoPreviewLayer.session = session
    previewLayer.videoPreviewLayer.videoGravity = .resizeAspectFill
    previewLayer.videoPreviewLayer.needsDisplayOnBoundsChange = true
  }

  private func updateType() {
    sessionQueue.async {
      self.initializeCaptureSessionInput()
      if !self.session.isRunning {
        self.startSession()
      }
    }
  }

  public func onAppForegrounded() {
    if !session.isRunning {
      sessionQueue.async {
        self.session.startRunning()
      }
    }
  }

  public func onAppBackgrounded() {
    if session.isRunning {
      sessionQueue.async {
        self.session.stopRunning()
      }
    }
  }

  private func startSession() {
    #if targetEnvironment(simulator)
    return
    #endif
    guard let manager = permissionsManager else {
      log.info("Permissions module not found.")
      return
    }
    if !manager.hasGrantedPermission(
      usingRequesterClass: CameraOnlyPermissionRequester.self
    ) {
      onMountError(["message": "Camera permissions not granted - component could not be rendered."])
      return
    }

    sessionQueue.async {
      self.session.beginConfiguration()

      let photoOutput = AVCapturePhotoOutput()
      if self.session.canAddOutput(photoOutput) {
        self.session.addOutput(photoOutput)
        photoOutput.isLivePhotoCaptureEnabled = false
        self.photoOutput = photoOutput
      }

      self.addErrorNotification()
      self.changePreviewOrientation()
      self.session.commitConfiguration()

      // Delay starting face detection
      self.sessionQueue.asyncAfter(
        deadline: .now() + 0.5
      ) {
        self.maybeStartFaceDetection(self.presetCamera == .front)
        self.session.startRunning()
        self.onCameraReady()
      }
    }
  }

  private func addErrorNotification() {
    if self.errorNotification != nil {
      NotificationCenter.default.removeObserver(self.errorNotification as Any)
    }

    self.errorNotification = NotificationCenter.default.addObserver(
      forName: .AVCaptureSessionRuntimeError,
      object: self.session,
      queue: nil
    ) { [weak self] notification in
      guard let self else {
        return
      }
      guard let error = notification.userInfo?[
        AVCaptureSessionErrorKey
      ] as? AVError else {
        return
      }

      if error.code == .mediaServicesWereReset {
        if !self.session.isRunning {
          self.session.startRunning()
          self.onCameraReady()
        }
      }
    }
  }

  func updateFaceDetectorSettings(
    settings: [String: Any]
  ) {
    if let faceDetector {
      faceDetector.updateSettings(settings)
    }
  }

  func takePicture(
    options: TakePictureOptions, 
    promise: Promise
  ) {
    if photoCapturedPromise != nil {
      promise.reject(CameraNotReadyException())
      return
    }

    guard let photoOutput else {
      promise.reject(CameraOutputNotReadyException())
      return
    }

    photoCapturedPromise = promise
    photoCaptureOptions = options

    sessionQueue.async {
      let connection = photoOutput.connection(with: .video)
      let orientation = UIDevice.current.orientation // self.physicalOrientation
      connection?.videoOrientation = CameraUtils.videoOrientation(for: orientation)
      let photoSettings = AVCapturePhotoSettings(
        format: [AVVideoCodecKey: AVVideoCodecType.jpeg]
      )

      if photoOutput.isHighResolutionCaptureEnabled {
        photoSettings.isHighResolutionPhotoEnabled = true
      }
      photoOutput.capturePhoto(
        with: photoSettings, 
        delegate: self
      )
    }
  }

  public func photoOutput(
    _ output: AVCapturePhotoOutput,
    didFinishProcessingRawPhoto rawSampleBuffer: CMSampleBuffer?,
    previewPhoto previewPhotoSampleBuffer: CMSampleBuffer?,
    resolvedSettings: AVCaptureResolvedPhotoSettings,
    bracketSettings: AVCaptureBracketedStillImageSettings?,
    error: Error?
  ) {
    guard let promise = photoCapturedPromise, let options = photoCaptureOptions else {
      return
    }
    photoCapturedPromise = nil
    photoCaptureOptions = nil

    guard let rawSampleBuffer, error != nil else {
      promise.reject(CameraImageCaptureException())
      return
    }

    guard let imageData = AVCapturePhotoOutput.jpegPhotoDataRepresentation(
      forJPEGSampleBuffer: rawSampleBuffer,
      previewPhotoSampleBuffer: previewPhotoSampleBuffer),
      let sourceImage = CGImageSourceCreateWithData(imageData as CFData, nil),
      let metadata = CGImageSourceCopyPropertiesAtIndex(sourceImage, 0, nil) as? [String: Any]
    else {
      promise.reject(CameraMetadataDecodingException())
      return
    }

    self.handleCapturedImageData(
      imageData: imageData, 
      metadata: metadata, 
      options: options, 
      promise: promise
    )
  }

  public func photoOutput(
    _ output: AVCapturePhotoOutput,
    didFinishProcessingPhoto photo: AVCapturePhoto,
    error: Error?
  ) {
    guard let promise = photoCapturedPromise, let options = photoCaptureOptions else {
      return
    }

    photoCapturedPromise = nil
    photoCaptureOptions = nil

    if error != nil {
      promise.reject(CameraImageCaptureException())
      return
    }

    let imageData = photo.fileDataRepresentation()
    handleCapturedImageData(
      imageData: imageData,
      metadata: photo.metadata,
      options: options,
      promise: promise
    )
  }

  func handleCapturedImageData(
    imageData: Data?,
    metadata: [String: Any],
    options: TakePictureOptions,
    promise: Promise
  ) {
    guard let imageData, var takenImage = UIImage(data: imageData) else {
      return
    }

    if options.fastMode {
      promise.resolve()
    }

    let previewSize: CGSize = {
      return deviceOrientation == .portrait ?
      CGSize(width: previewLayer.frame.size.height, height: previewLayer.frame.size.width) :
      CGSize(width: previewLayer.frame.size.width, height: previewLayer.frame.size.height)
    }()

    guard let takenCgImage = takenImage.cgImage else {
      return
    }

    let cropRect = CGRect(
      x: 0, 
      y: 0, 
      width: takenCgImage.width, 
      height: takenCgImage.height
    )
    let croppedSize = AVMakeRect(
      aspectRatio: previewSize, 
      insideRect: cropRect
    )

    takenImage = CameraUtils.crop(
      image: takenImage, 
      to: croppedSize
    )

    let path = FileSystemUtilities.generatePathInCache(
      appContext,
      in: "Camera",
      extension: ".jpg"
    )

    let width = takenImage.size.width
    let height = takenImage.size.height
    var processedImageData: Data?

    var response = [String: Any]()

    if options.exif {
      guard let exifDict = metadata[kCGImagePropertyExifDictionary as String] as? NSDictionary else {
        return
      }
      let updatedExif = CameraUtils.updateExif(
        metadata: exifDict,
        with: ["Orientation": CameraUtils.export(orientation: takenImage.imageOrientation)]
      )

      updatedExif[kCGImagePropertyExifPixelYDimension] = width
      updatedExif[kCGImagePropertyExifPixelXDimension] = height
      response["exif"] = updatedExif

      var updatedMetadata = metadata

      if let additionalExif = options.additionalExif {
        updatedExif.addEntries(from: additionalExif)
        var gpsDict = [String: Any]()

        let gpsLatitude = additionalExif["GPSLatitude"] as? Double
        if let latitude = gpsLatitude {
          gpsDict[kCGImagePropertyGPSLatitude as String] = abs(latitude)
          gpsDict[kCGImagePropertyGPSLatitudeRef as String] = latitude >= 0 ? "N" : "S"
        }

        let gpsLongitude = additionalExif["GPSLongitude"] as? Double
        if let longitude = gpsLongitude {
          gpsDict[kCGImagePropertyGPSLongitude as String] = abs(longitude)
          gpsDict[kCGImagePropertyGPSLongitudeRef as String] = longitude >= 0 ? "E" : "W"
        }

        let gpsAltitude = additionalExif["GPSAltitude"] as? Double
        if let altitude = gpsAltitude {
          gpsDict[kCGImagePropertyGPSAltitude as String] = abs(altitude)
          gpsDict[kCGImagePropertyGPSAltitudeRef as String] = altitude >= 0 ? 0 : 1
        }

        let metadataGpsDict = updatedMetadata[kCGImagePropertyGPSDictionary as String] as? [String: Any]
        if updatedMetadata[kCGImagePropertyGPSDictionary as String] == nil {
          updatedMetadata[kCGImagePropertyGPSDictionary as String] = gpsDict
        } else {
          if let metadataGpsDict = updatedMetadata[kCGImagePropertyGPSDictionary as String] as? NSMutableDictionary {
            metadataGpsDict.addEntries(from: gpsDict)
          }
        }
      }

      updatedMetadata[kCGImagePropertyExifDictionary as String] = updatedExif
      processedImageData = CameraUtils.data(
        from: takenImage,
        with: updatedMetadata,
        quality: Float(options.quality))
    } else {
      processedImageData = takenImage.jpegData(compressionQuality: options.quality)
    }

    guard let processedImageData else {
      promise.reject(CameraSavingImageException())
      return
    }

    response["uri"] = CameraUtils.write(data: processedImageData, to: path)
    response["width"] = width
    response["height"] = height

    if options.base64 {
      response["base64"] = processedImageData.base64EncodedString()
    }

    if options.fastMode {
      onPictureSaved(["data": response, "id": options.id])
    } else {
      promise.resolve(response)
    }
  }

  public override func layoutSubviews() {
    previewLayer.videoPreviewLayer.frame = self.bounds
    self.backgroundColor = .black
    self.layer.insertSublayer(previewLayer.videoPreviewLayer, at: 0)
  }

  public override func removeFromSuperview() {
    lifecycleManager?.unregisterAppLifecycleListener(self)
    super.removeFromSuperview()
    UIDevice.current.endGeneratingDeviceOrientationNotifications()
    NotificationCenter.default.removeObserver(self, name: UIDevice.orientationDidChangeNotification, object: nil)
  }

  func setPresetCamera(presetCamera: AVCaptureDevice.Position) {
    self.presetCamera = presetCamera
  }

  // Must be called on the sessionQueue
  func updateSessionPreset(preset: AVCaptureSession.Preset) {
    #if !targetEnvironment(simulator)
    if self.session.canSetSessionPreset(preset) {
      self.session.beginConfiguration()
      self.session.sessionPreset = preset
      self.session.commitConfiguration()
    }
    #endif
  }

  func initializeCaptureSessionInput() {
    if captureDeviceInput?.device.position == presetCamera {
      return
    }

    sessionQueue.async {
      self.session.beginConfiguration()

      guard let device = CameraUtils.device(
        with: .video, 
        preferring: self.presetCamera
      ) else {
        return
      }

      if let videoCaptureDeviceInput = self.captureDeviceInput {
        self.session.removeInput(videoCaptureDeviceInput)
      }

      do {
        let captureDeviceInput = try AVCaptureDeviceInput(device: device)

        if self.session.canAddInput(captureDeviceInput) {
          self.session.addInput(captureDeviceInput)
          self.captureDeviceInput = captureDeviceInput
        }
      } catch {
        self.onMountError([
          "message": "Camera could not be started - \(error.localizedDescription)"
        ])
      }
      self.session.commitConfiguration()
    }
  }

  private func stopSession() {
    #if targetEnvironment(simulator)
    return
    #endif
    self.previewLayer.videoPreviewLayer.removeFromSuperlayer()

    sessionQueue.async {
      self.session.beginConfiguration()
      for input in self.session.inputs {
        self.session.removeInput(input)
      }

      for output in self.session.outputs {
        self.session.removeOutput(output)
      }
      if let faceDetector = self.faceDetector {
        faceDetector.stopFaceDetection()
      }
      self.session.commitConfiguration()

      self.motionManager.stopAccelerometerUpdates()
      self.session.stopRunning()
    }
  }

  func maybeStartFaceDetection(_ mirrored: Bool) {
    print("maybeStartFaceDetection call")
    guard let faceDetector else {
      print("faceDetector guardade - returning")
      return
    }
    let connection = photoOutput?.connection(with: .video)
    connection?.videoOrientation = CameraUtils.videoOrientation(
      for: UIDevice.current.orientation
    )
    print("maybe starting")
    faceDetector.maybeStartFaceDetection(
      on: session, 
      with: previewLayer.videoPreviewLayer, 
      mirrored: mirrored
    )
  }

  @objc func orientationChanged(notification: Notification) {
    changePreviewOrientation()
  }

  func changePreviewOrientation() {
    EXUtilities.performSynchronously {
      // We shouldn't access the device orientation anywhere but on the main thread
      let videoOrientation = CameraUtils.videoOrientation(for: self.deviceOrientation)
      if (self.previewLayer.videoPreviewLayer.connection?.isVideoOrientationSupported) == true {
        self.previewLayer.videoPreviewLayer.connection?.videoOrientation = videoOrientation
      }
    }
  }

  private func createFaceDetectorManager() -> FaceDetectorManagerInterface? {
    print("createFaceDetectorManager call")
    let provider: FaceDetectorManagerProviderInterface? = appContext?.legacyModule(
      implementing: FaceDetectorManagerProviderInterface.self
    )

    guard let faceDetector = provider?.createFaceDetectorManager() else {
      print("face detector priver nil - returning")
      return nil
    }

    faceDetector.setOnFacesDetected{ [weak self] faces in
      guard let self else {
        print("faceDetector.setOnFacesDetected guarded - returning")
        return
      }

      print("faceDetector event call")
      self.onFacesDetected([
        "faces": faces
      ])
    }

    faceDetector.setSessionQueue(sessionQueue)
    return faceDetector
  }

  deinit {
    if let photoCapturedPromise {
      photoCapturedPromise.reject(CameraUnmountedException())
    }

    if let errorNotification {
      NotificationCenter.default.removeObserver(errorNotification)
    }
  }
}
