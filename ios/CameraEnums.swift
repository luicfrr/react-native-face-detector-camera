import AVFoundation
import ExpoModulesCore

enum CameraType: String, Enumerable {
  case front

  func toPosition() -> AVCaptureDevice.Position {
    switch self {
    default:
      return .front
    }
  }
}
