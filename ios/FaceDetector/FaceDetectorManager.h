#import <AVFoundation/AVFoundation.h>
#import <FaceDetectorCamera/FaceDetectorManagerInterface.h>

@interface FaceDetectorManager : NSObject <FaceDetectorManagerInterface>

- (void)setOnFacesDetected:(void (^)(NSArray<NSDictionary *> *))onFacesDetected;

- (void)setIsEnabled:(BOOL)enabled;
- (void)updateSettings:(NSDictionary *)settings;

- (void)maybeStartFaceDetectionOnSession:(AVCaptureSession *)session
                        withPreviewLayer:(AVCaptureVideoPreviewLayer *)previewLayer;
- (void)maybeStartFaceDetectionOnSession:(AVCaptureSession *)session
                        withPreviewLayer:(AVCaptureVideoPreviewLayer *)previewLayer mirrored:(BOOL)mirrored;
- (void)stopFaceDetection;

@end
