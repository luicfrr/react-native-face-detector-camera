#import <FaceDetectorCamera/FaceDetectorManagerInterface.h>

@protocol FaceDetectorManagerProviderInterface

- (id<FaceDetectorManagerInterface>)createFaceDetectorManager;

@end
