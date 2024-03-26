import * as React from 'react'
import {
  CameraCapturedPicture,
  CameraPictureOptions,
  CameraProps,
  CameraViewRef
} from './Camera.types'
import FaceDetectorCamera from './FaceDetectorCamera'
import { ensureNativeProps } from './utils/props'

const EventThrottleMs = 500
const _PICTURE_SAVED_CALLBACKS = {}
let _GLOBAL_PICTURE_ID = 1

function ensurePictureOptions( options?: CameraPictureOptions ): CameraPictureOptions {
  const pictureOptions: CameraPictureOptions =
    !options || typeof options !== 'object' ? {} : options

  if ( !pictureOptions.quality ) {
    pictureOptions.quality = 1
  }
  if ( pictureOptions.onPictureSaved ) {
    const id = _GLOBAL_PICTURE_ID++
    _PICTURE_SAVED_CALLBACKS[ id ] = pictureOptions.onPictureSaved
    pictureOptions.id = id
    pictureOptions.fastMode = true
  }
  return pictureOptions
}

function _onPictureSaved( {
  nativeEvent,
}: {
  nativeEvent: { data: CameraCapturedPicture; id: number }
} ) {
  const { id, data } = nativeEvent
  const callback = _PICTURE_SAVED_CALLBACKS[ id ]
  if ( callback ) {
    callback( data )
    delete _PICTURE_SAVED_CALLBACKS[ id ]
  }
}

export default class CameraView extends React.Component<CameraProps> {
  static defaultProps: CameraProps = {
    facing: 'front'
  };

  _cameraHandle?: number | null
  _cameraRef = React.createRef<CameraViewRef>();
  _lastEvents: { [ eventName: string ]: string } = {};
  _lastEventsTimes: { [ eventName: string ]: Date } = {};

  /**
   * Takes a picture and saves it to app's cache directory. Photos are rotated to match device's orientation
   * (if `options.skipProcessing` flag is not enabled) and scaled to match the preview. Anyway on Android it is essential
   * to set ratio prop to get a picture with correct dimensions.
   * > **Note**: Make sure to wait for the [`onCameraReady`](#oncameraready) callback before calling this method.
   * @param options An object in form of `CameraPictureOptions` type.
   * @return Returns a Promise that resolves to `CameraCapturedPicture` object, where `uri` is a URI to the local image file on iOS,
   * Android, and a base64 string on web (usable as the source for an `Image` element). The `width` and `height` properties specify
   * the dimensions of the image. `base64` is included if the `base64` option was truthy, and is a string containing the JPEG data
   * of the image in Base64--prepend that with `'data:image/jpg;base64,'` to get a data URI, which you can use as the source
   * for an `Image` element for example. `exif` is included if the `exif` option was truthy, and is an object containing EXIF
   * data for the image--the names of its properties are EXIF tags and their values are the values for those tags.
   *
   * > The local image URI is temporary. Use [`FileSystem.copyAsync`](https://docs.expo.dev/versions/latest/sdk/filesystem/#filesystemcopyasyncoptions)
   * > to make a permanent copy of the image.
   */
  async takePictureAsync(
    options?: CameraPictureOptions
  ): Promise<CameraCapturedPicture | undefined> {
    const pictureOptions = ensurePictureOptions( options )

    return await this._cameraRef.current?.takePicture( pictureOptions )
  }

  _onCameraReady = () => {
    if ( this.props.onCameraReady ) {
      this.props.onCameraReady()
    }
  };

  _onMountError = ( { nativeEvent }: { nativeEvent: { message: string } } ) => {
    if ( this.props.onMountError ) {
      this.props.onMountError( nativeEvent )
    }
  };

  _onObjectDetected = ( callback?: Function ) =>
    ( { nativeEvent }: { nativeEvent: any } ) => {
      const { type } = nativeEvent
      if (
        this._lastEvents[ type ] &&
        this._lastEventsTimes[ type ] &&
        JSON.stringify( nativeEvent ) === this._lastEvents[ type ] &&
        new Date().getTime() - this._lastEventsTimes[ type ].getTime() < EventThrottleMs
      ) {
        return
      }

      if ( callback ) {
        callback( nativeEvent )
        this._lastEventsTimes[ type ] = new Date()
        this._lastEvents[ type ] = JSON.stringify( nativeEvent )
      }
    };

  render() {
    const nativeProps = ensureNativeProps( this.props )
    const onFacesDetected = this._onObjectDetected( this.props.onFacesDetected )

    return (
      <FaceDetectorCamera
        { ...nativeProps }
        ref={ this._cameraRef }
        onCameraReady={ this._onCameraReady }
        onMountError={ this._onMountError }
        onFacesDetected={ onFacesDetected }
        onPictureSaved={ _onPictureSaved }
      />
    )
  }
}
