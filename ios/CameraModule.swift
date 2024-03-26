import AVFoundation
import ExpoModulesCore
import VisionKit

let cameraEvents = [
  "onCameraReady", 
  "onMountError", 
  "onFacesDetected", 
  "onFaceDetectionError", 
  "onPictureSaved"
]

public final class CameraModule: Module {
  public func definition() -> ModuleDefinition {
    Name("FaceDetectorCamera")

    // Events("onFacesDetected")

    OnCreate {
      let permissionsManager = self.appContext?.permissions
      EXPermissionsMethodsDelegate.register(
        [
          CameraPermissionRequester(),
          CameraOnlyPermissionRequester()
        ],
        withPermissionsManager: permissionsManager
      )
    }

    // swiftlint:disable:next closure_body_length
    View(CameraView.self) {
      Events(cameraEvents)

      Prop("facing") { (view, type: CameraType) in
        if view.presetCamera != type.toPosition() {
          view.presetCamera = type.toPosition()
        }
      }

      Prop("faceDetectorEnabled") { (view, detectFaces: Bool?) in
        if view.isDetectingFaces != detectFaces {
          view.isDetectingFaces = detectFaces ?? false
        }
      }

      Prop("faceDetectorSettings") { (view, settings: [String: Any]?) in
        if settings == nil {
          return
        }
        view.isDetectingFaces = true
        view.updateFaceDetectorSettings(settings: settings!)
      }

      AsyncFunction("takePicture") { (view, options: TakePictureOptions, promise: Promise) in
        #if targetEnvironment(simulator)
        try takePictureForSimulator(self.appContext, view, options, promise)
        #else // simulator
        view.takePicture(options: options, promise: promise)
        #endif // not simulator
      }.runOnQueue(.main)
    }

    AsyncFunction("getCameraPermissionsAsync") { (promise: Promise) in
      EXPermissionsMethodsDelegate.getPermissionWithPermissionsManager(
        self.appContext?.permissions,
        withRequester: CameraOnlyPermissionRequester.self,
        resolve: promise.resolver,
        reject: promise.legacyRejecter
      )
    }

    AsyncFunction("requestCameraPermissionsAsync") { (promise: Promise) in
      EXPermissionsMethodsDelegate.askForPermission(
        withPermissionsManager: self.appContext?.permissions,
        withRequester: CameraOnlyPermissionRequester.self,
        resolve: promise.resolver,
        reject: promise.legacyRejecter
      )
    }
  }

  func onItemScanned(result: [String: Any]) {
    sendEvent("onFacesDetected", result)
  }
}

private func takePictureForSimulator(
  _ appContext: AppContext?,
  _ view: CameraView,
  _ options: TakePictureOptions,
  _ promise: Promise
) throws {
  if options.fastMode {
    promise.resolve()
  }
  let result = try generatePictureForSimulator(appContext: appContext, options: options)

  if options.fastMode {
    view.onPictureSaved([
      "data": result,
      "id": options.id
    ])
  } else {
    promise.resolve(result)
  }
}

private func generatePictureForSimulator(
  appContext: AppContext?,
  options: TakePictureOptions
) throws -> [String: Any?] {
  let path = FileSystemUtilities.generatePathInCache(
    appContext,
    in: "Camera",
    extension: ".jpg"
  )

  let generatedPhoto = CameraUtils.generatePhoto(of: CGSize(width: 200, height: 200))
  guard let photoData = generatedPhoto.jpegData(compressionQuality: options.quality) else {
    throw CameraInvalidPhotoData()
  }

  return [
    "uri": CameraUtils.write(data: photoData, to: path),
    "width": generatedPhoto.size.width,
    "height": generatedPhoto.size.height,
    "base64": options.base64 ? photoData.base64EncodedString() : nil
  ]
}
