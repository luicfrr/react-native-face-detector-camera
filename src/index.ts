import { createPermissionHook } from 'expo-modules-core'
import { PermissionResponse } from './Camera.types'
import CameraManager from './CameraManager'
export { default as CameraView } from './CameraView'

/**
 * Checks user's permissions for accessing camera.
 * @return A promise that resolves to an object of type [PermissionResponse](#permissionresponse).
 */
async function getCameraPermissionsAsync(): Promise<PermissionResponse> {
  return CameraManager.getCameraPermissionsAsync()
}

/**
 * Asks the user to grant permissions for accessing camera.
 * On iOS this will require apps to specify an `NSCameraUsageDescription` entry in the **Info.plist**.
 * @return A promise that resolves to an object of type [PermissionResponse](#permissionresponse).
 */
async function requestCameraPermissionsAsync(): Promise<PermissionResponse> {
  return CameraManager.requestCameraPermissionsAsync()
}

/**
 * Check or request permissions to access the camera.
 * This uses both `requestCameraPermissionsAsync` and `getCameraPermissionsAsync` to interact with the permissions.
 *
 * @example
 * ```ts
 * const [status, requestPermission] = useCameraPermissions();
 * ```
 */
export const useCameraPermissions = createPermissionHook( {
  getMethod: getCameraPermissionsAsync,
  requestMethod: requestCameraPermissionsAsync,
} )

export * from './Camera.types'

/**
 * @hidden
 */
export const Camera = {
  getCameraPermissionsAsync,
  requestCameraPermissionsAsync
}
