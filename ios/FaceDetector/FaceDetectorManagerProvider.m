#import <FaceDetectorCamera/FaceDetectorManagerProvider.h>
#import <FaceDetectorCamera/FaceDetectorManager.h>
#import <FaceDetectorCamera/FaceDetectorManagerProviderInterface.h>
#import <ExpoModulesCore/EXDefines.h>

@implementation FaceDetectorManagerProvider

EX_REGISTER_MODULE();

+ (const NSArray<Protocol *> *)exportedInterfaces {
  return @[@protocol(FaceDetectorManagerProviderInterface)];
}

- (id<FaceDetectorManagerInterface>)createFaceDetectorManager {
  return [[FaceDetectorManager alloc] init];
}

@end
