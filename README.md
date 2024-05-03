## 📚 Introduction

`react-native-face-detector-camera` is an Expo module that uses device's front camera and MLKit to detect faces in real-time and visualize the detected face on the screen.

If this package helped you please give it a ⭐ on [GitHub](https://github.com/luicfrr/react-native-face-detector-camera).

## ❗ Warning

This package was created to meet a private project's need. New features are unlikely to be added. Bug fixes will be prioritized for those impacting project stability.

Knowing this, you're free to use this package as it is.

## 🏗️ Features

- Real-time face detection using front camera
- Adjustable face detection
- Take pictures using front camera only
- Customizable styles

## 🧰 Installation

```bash
yarn add react-native-face-detector-camera
```

## Plugin configuration

You can configure `react-native-face-detector-camera` using built-in config plugin. This plugin allows you to change camera permission message that cannot be set at runtime and require you to build a new binary to take effect.

Example:
```
{
  "expo": {
    ...,
    "plugins": [
      ["react-native-face-detector-camera",{
          "cameraPermission": "Allow $(PRODUCT_NAME) to access your camera"
      }]
    ]
  }
}
```

## 💡 Usage
```jsx
import {
  useEffect,
  useRef
} from 'react'
import {
  StyleSheet,
  View
} from 'react-native'
import {
  CameraView,
  useCameraPermissions,
  FaceDetectorMode,
  FaceDetectorClassifications,
  FaceDetectionResult
} from 'react-native-face-detector-camera'

function App() {
  const [
    status,
    requestPermission
  ] = useCameraPermissions()
  const camera = useRef<CameraView>( null )

  useEffect( () => {
    ( async () => {
      if ( status?.granted ) return
      await requestPermission()
    } )()
  }, [ status ] )

  function processFaceDetection( {
    faces
  }: FaceDetectionResult ) {
    console.log( 'faces', faces )
    if ( faces.length <= 0 ) return

    const {
      size,
      origin
    } = faces[ 0 ].bounds
    
    // handle face position on screen
    if( 
      // size.width|height >= ...
      // size.width|height <= ...
      // origin.x >= ...
      // origin.x <= ...
      // origin.y >= ...
      // origin.y <= ...
     ) {
      handleTakePicture()
    }
  }

  function handleTakePicture() {
    if ( !camera.current ) {
      console.log( 'camera ref is not valid' )
      return
    }

    camera.current.takePictureAsync( {
      skipProcessing: true,
      onPictureSaved: ( async ( {
        uri
      } ) => {
        console.log( 'picture saved event', uri )
      } )
    } )
  }

  return (
    <View
      style={ StyleSheet.absoluteFill }
    >
      <CameraView
          ref={ camera }
          style={ StyleSheet.absoluteFill }
          facing={ 'front' }
          onCameraReady={ () => { console.log( 'camera is ready' ) } }
          onMountError={ () => { console.log( 'camera mount error' ) } }
          faceDetectorSettings={ {
            mode: FaceDetectorMode.fast,
            runClassifications: FaceDetectorClassifications.all,
            minDetectionInterval: 200
          } }
          onFacesDetected={ processFaceDetection }
        />
    </View>
  )
}
```

## 🔎 About

Min Android/IOS versions:

- `Android SDK`: `26` (Android 8)
- `IOS`: `13.4`

## 👷 Built With

- [React Native](https://reactnative.dev/)
- [Google MLKit](https://developers.google.com/ml-kit)
- [Expo Module](https://docs.expo.dev/modules/get-started/)

## 📚 Author

Made with ❤️ by [luicfrr](https://github.com/luicfrr)
