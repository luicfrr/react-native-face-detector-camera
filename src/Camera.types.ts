import {
  PermissionResponse,
  PermissionStatus,
  PermissionExpiration,
  PermissionHookOptions,
} from 'expo-modules-core'
import { Ref } from 'react'
import type { ViewProps } from 'react-native'

export type CameraType = 'front'

export type CameraCapturedPicture = {
  /**
   * Captured image width.
   */
  width: number
  /**
   * Captured image height.
   */
  height: number
  /**
   * On web, the value of `uri` is the same as `base64` because file system URLs are not supported in the browser.
   */
  uri: string
  /**
   * A Base64 representation of the image.
   */
  base64?: string
  /**
   * On Android and iOS this object may include various fields based on the device and operating system.
   * On web, it is a partial representation of the [`MediaTrackSettings`](https://developer.mozilla.org/en-US/docs/Web/API/MediaTrackSettings) dictionary.
   */
  exif?: Partial<MediaTrackSettings> | any
}

export type CameraPictureOptions = {
  /**
   * Specify the compression quality from `0` to `1`. `0` means compress for small size, and `1` means compress for maximum quality.
   */
  quality?: number
  /**
   * Whether to also include the image data in Base64 format.
   */
  base64?: boolean
  /**
   * Whether to also include the EXIF data for the image.
   */
  exif?: boolean
  /**
   * A callback invoked when picture is saved. If set, the promise of this method will resolve immediately with no data after picture is captured.
   * The data that it should contain will be passed to this callback. If displaying or processing a captured photo right after taking it
   * is not your case, this callback lets you skip waiting for it to be saved.
   * @param picture
   */
  onPictureSaved?: ( picture: CameraCapturedPicture ) => void
  /**
   * If set to `true`, camera skips orientation adjustment and returns an image straight from the device's camera.
   * If enabled, `quality` option is discarded (processing pipeline is skipped as a whole).
   * Although enabling this option reduces image delivery time significantly, it may cause the image to appear in a wrong orientation
   * in the `Image` component (at the time of writing, it does not respect EXIF orientation of the images).
   * > **Note**: Enabling `skipProcessing` would cause orientation uncertainty. `Image` component does not respect EXIF
   * > stored orientation information, that means obtained image would be displayed wrongly (rotated by 90°, 180° or 270°).
   * > Different devices provide different orientations. For example some Sony Xperia or Samsung devices don't provide
   * > correctly oriented images by default. To always obtain correctly oriented image disable `skipProcessing` option.
   */
  skipProcessing?: boolean
  /**
   * @hidden
   */
  id?: number
  /**
   * @hidden
   */
  fastMode?: boolean
  /**
   * @hidden
   */
  maxDownsampling?: number
}

/**
 * @hidden
 */
export type PictureSavedListener = ( event: {
  nativeEvent: { data: CameraCapturedPicture; id: number }
} ) => void

/**
 * @hidden
 */
export type CameraReadyListener = () => void

/**
 * @hidden
 */
export type MountErrorListener = ( event: {
  nativeEvent: CameraMountError
} ) => void

export type CameraMountError = { message: string }

export type FaceDetectionSettings = object

export type FaceDetectionResult = {
  /**
   * Array of objects representing results of face detection.
   */
  faces: FaceFeature[]
  id: number
}

export type Image = {
  /**
   * URI of the image.
   */
  uri: string
  /**
   * Width of the image in pixels.
   */
  width: number
  /**
   * Height of the image in pixels.
   */
  height: number
  /**
   * Orientation of the image (value conforms to the EXIF orientation tag standard).
   */
  orientation: number
}

export type Point = {
  x: number
  y: number
}

export type FaceFeatureBounds = {
  /**
   * Size of the square containing the face in image coordinates,
   */
  size: {
    width: number
    height: number
  }
  /**
   * Position of the top left corner of a square containing the face in image coordinates,
   */
  origin: Point
}

export type FaceFeature = {
  /**
   * An object containing face bounds.
   */
  bounds: FaceFeatureBounds
  /**
   * Probability that the face is smiling. Returned only if detection classifications property is
   * set to `FaceDetectorClassifications.all`.
   */
  smilingProbability?: number
  /**
   * Position of the left ear in image coordinates. Returned only if detection classifications
   * property is set to `FaceDetectorLandmarks.all`.
   */
  leftEarPosition?: Point
  /**
   * Position of the right ear in image coordinates. Returned only if detection classifications
   * property is set to `FaceDetectorLandmarks.all`.
   */
  rightEarPosition?: Point
  /**
   * Position of the left eye in image coordinates. Returned only if detection classifications
   * property is set to `FaceDetectorLandmarks.all`.
   */
  leftEyePosition?: Point
  /**
   * Probability that the left eye is open. Returned only if detection classifications property is
   * set to `FaceDetectorClassifications.all`.
   */
  leftEyeOpenProbability?: number
  /**
   * Position of the right eye in image coordinates. Returned only if detection classifications
   * property is set to `FaceDetectorLandmarks.all`.
   */
  rightEyePosition?: Point
  /**
   * Probability that the right eye is open. Returned only if detection classifications property is
   * set to `FaceDetectorClassifications.all`.
   */
  rightEyeOpenProbability?: number
  /**
   * Position of the left cheek in image coordinates. Returned only if detection classifications
   * property is set to `FaceDetectorLandmarks.all`.
   */
  leftCheekPosition?: Point
  /**
   * Position of the right cheek in image coordinates. Returned only if detection classifications
   * property is set to `FaceDetectorLandmarks.all`.
   */
  rightCheekPosition?: Point
  /**
   * Position of the left edge of the mouth in image coordinates. Returned only if detection
   * classifications property is set to `FaceDetectorLandmarks.all`.
   */
  leftMouthPosition?: Point
  /**
   * Position of the center of the mouth in image coordinates. Returned only if detection
   * classifications property is set to `FaceDetectorLandmarks.all`.
   */
  mouthPosition?: Point
  /**
   * Position of the right edge of the mouth in image coordinates. Returned only if detection
   * classifications property is set to `FaceDetectorLandmarks.all`.
   */
  rightMouthPosition?: Point
  /**
   * Position of the bottom edge of the mouth in image coordinates. Returned only if detection
   * classifications property is set to `FaceDetectorLandmarks.all`.
   */
  bottomMouthPosition?: Point
  /**
   * Position of the nose base in image coordinates. Returned only if detection classifications
   * property is set to `FaceDetectorLandmarks.all`.
   */
  noseBasePosition?: Point
  /**
   * Yaw angle of the face (heading, turning head left or right).
   */
  yawAngle?: number
  /**
   * Roll angle of the face (bank).
   */
  rollAngle?: number
  /**
   * A face identifier (used for tracking, if the same face appears on consecutive frames it will
   * have the same `faceID`).
   */
  faceID?: number
}

export enum FaceDetectorMode {
  fast = 1,
  accurate = 2,
}

export enum FaceDetectorLandmarks {
  none = 1,
  all = 2,
}

export enum FaceDetectorClassifications {
  none = 1,
  all = 2,
}

export type FaceDetectorSettings = {
  /**
   * Whether to detect faces in fast or accurate mode. Use `FaceDetectorMode.{fast, accurate}`.
   */
  mode?: FaceDetectorMode
  /**
   * Whether to detect and return landmarks positions on the face (ears, eyes, mouth, cheeks, nose).
   * Use `FaceDetectorLandmarks.{all, none}`.
   */
  detectLandmarks?: FaceDetectorLandmarks
  /**
   * Whether to run additional classifications on detected faces (smiling probability, open eye
   * probabilities). Use `FaceDetectorClassifications.{all, none}`.
   */
  runClassifications?: FaceDetectorClassifications
  /**
   * Minimal interval in milliseconds between two face detection events being submitted to JS.
   * Use, when you expect lots of faces for long time and are afraid of JS Bridge being overloaded.
   * @default 0
   */
  minDetectionInterval?: number
  /**
   * Flag to enable tracking of faces between frames. If true, each face will be returned with
   * `faceID` attribute which should be consistent across frames.
   * @default false
   */
  tracking?: boolean
}

export type CameraProps = ViewProps & {
  /**
   * Camera facing `front` using the front-facing camera.
   * @default 'front'
   */
  facing?: CameraType
  /**
   * Callback invoked when camera preview has been set.
   */
  onCameraReady?: () => void
  /**
   * Callback invoked when camera preview could not been started.
   * @param event Error object that contains a `message`.
   */
  onMountError?: ( event: CameraMountError ) => void
  /**
   * A settings object passed directly to an underlying module providing face detection features.
   */
  faceDetectorSettings?: FaceDetectorSettings
  /**
   * Callback invoked with results of face detection on the preview.
   * @param faces
   */
  onFacesDetected?: ( faces: FaceDetectionResult ) => void
}

/**
 * @hidden
 */
export interface CameraViewRef {
  readonly takePicture: ( options: CameraPictureOptions ) => Promise<CameraCapturedPicture>
}

/**
 * @hidden
 */
export type CameraNativeProps = {
  pointerEvents?: any
  style?: any
  ref?: Ref<CameraViewRef>
  onCameraReady?: CameraReadyListener
  onMountError?: MountErrorListener
  onFacesDetected?: ( event: { nativeEvent: FaceDetectionResult } ) => void
  onFaceDetectionError?: ( event: { nativeEvent: Error } ) => void
  onPictureSaved?: PictureSavedListener
  faceDetectorSettings?: FaceDetectorSettings
}


export {
  PermissionResponse,
  PermissionStatus,
  PermissionExpiration,
  PermissionHookOptions
}
