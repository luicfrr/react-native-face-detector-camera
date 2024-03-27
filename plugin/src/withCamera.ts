import {
  withPlugins,
  AndroidConfig,
  ConfigPlugin,
  createRunOncePlugin
} from '@expo/config-plugins'
const pkg = require( '../../package.json' )

const CAMERA_USAGE = 'Allow $(PRODUCT_NAME) to access your camera'

const withCamera: ConfigPlugin<{
  cameraPermission?: string
}> = (
  config,
  props = {}
) => {
    if ( config.ios == null ) config.ios = {}
    if ( config.ios.infoPlist == null ) config.ios.infoPlist = {}

    config.ios.infoPlist.NSCameraUsageDescription = props.cameraPermission ?? (
      config.ios.infoPlist.NSCameraUsageDescription as string | undefined
    ) ?? CAMERA_USAGE
    const androidPermissions = [ 'android.permission.CAMERA' ]

    return withPlugins( config, [ [
      AndroidConfig.Permissions.withPermissions,
      androidPermissions
    ] ] )
  }

export default createRunOncePlugin(
  withCamera,
  pkg.name,
  pkg.version
)
