import { requireNativeViewManager } from 'expo-modules-core'
import * as React from 'react'

import { CameraNativeProps } from './Camera.types'

const FaceDetectorCamera: React.ComponentType<CameraNativeProps> =
  requireNativeViewManager( 'FaceDetectorCamera' )

export default FaceDetectorCamera
