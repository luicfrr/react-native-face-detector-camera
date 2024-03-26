import {
  useEffect,
  useRef,
  useState
} from 'react'
import {
  Button,
  StyleSheet,
  View,
  Image,
  Text,
  Platform
} from 'react-native'
import {
  CameraView,
  useCameraPermissions,
  FaceDetectorMode,
  FaceDetectorClassifications,
  FaceDetectionResult
} from 'react-native-face-detector-camera'
import Animated, {
  useAnimatedStyle,
  useSharedValue
} from 'react-native-reanimated'

export default function App() {
  const isIos = Platform.OS === 'ios'
  const [
    status,
    requestPermission
  ] = useCameraPermissions()
  const [
    imageUri,
    setImageUri
  ] = useState<string>( '' )
  const [
    cameraMounted,
    setCameraMounted
  ] = useState<boolean>( true )
  const camera = useRef<CameraView>( null )
  const aFaceW = useSharedValue( 0 )
  const aFaceH = useSharedValue( 0 )
  const aFaceX = useSharedValue( 0 )
  const aFaceY = useSharedValue( 0 )
  const animatedStyle = useAnimatedStyle( () => ( {
    position: 'absolute',
    borderWidth: 4,
    borderLeftColor: 'green',
    borderRightColor: 'green',
    borderBottomColor: 'green',
    borderTopColor: 'blue',
    width: aFaceW.value,
    height: aFaceH.value,
    left: aFaceX.value,
    top: aFaceY.value
  } ) )

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
    aFaceW.value = size.width
    aFaceH.value = size.height
    aFaceX.value = origin.x
    aFaceY.value = origin.y
  }

  function handleTakePicture() {
    if ( !camera.current ) {
      console.log( 'camera ref is not valid' )
      return
    }

    camera.current?.takePictureAsync( {
      skipProcessing: true,
      onPictureSaved: ( async ( {
        uri
      } ) => {
        console.log( 'picture saved event' )
        setImageUri( () => uri )
      } )
    } )
  }

  return (
    <View
      style={ StyleSheet.absoluteFill }
    >
      { cameraMounted ? <>
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

        <Animated.View
          style={ animatedStyle }
        />
      </> : <Text
        style={ {
          backgroundColor: 'red',
          color: 'white',
          padding: 20,
          position: 'absolute',
          top: 0,
          right: 0,
          left: 0,
          bottom: 0,
          textAlign: 'center'
        } }
      >
        Camera unmounted
      </Text> }

      <View
        style={ {
          position: 'absolute',
          top: isIos ? 30 : 10,
          left: 0,
          right: 0
        } }
      >
        <Button
          onPress={ () => {
            console.log( `${ cameraMounted ? 'Unm' : 'M' }ounting camera` )
            setCameraMounted( current => !current )
          } }
          title={ `${ cameraMounted ? 'Unm' : 'M' }ount camera` }
        />
      </View>

      <View
        style={ {
          position: 'absolute',
          bottom: isIos ? 20 : 0,
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center'
        } }
      >
        <View
          style={ {
            width: 50,
            height: 50,
            margin: 'auto'
          } }
        >
          <Button
            onPress={ handleTakePicture }
            title={ 'Take pic' }
          />
        </View>
      </View>

      { imageUri !== '' && <Image
        style={ {
          width: 90,
          height: 160,
          position: 'absolute',
          bottom: 10,
          right: 10,
          zIndex: 99999
        } }
        source={ {
          uri: imageUri
        } }
      /> }
    </View>
  )
}
