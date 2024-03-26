#import <FaceDetectorCamera/FaceDetectorModule.h>
#import <FaceDetectorCamera/FaceDetector.h>
#import <FaceDetectorCamera/FileSystemInterface.h>
#import <FaceDetectorCamera/FaceDetectorUtils.h>
#import <ExpoModulesCore/EXModuleRegistry.h>
#import <FaceDetectorCamera/FaceEncoder.h>
#import <FaceDetectorCamera/CSBufferOrientationCalculator.h>

@interface FaceDetectorModule ()

@property (nonatomic, weak) EXModuleRegistry *moduleRegistry;

@end

@implementation FaceDetectorModule

static NSFileManager *fileManager = nil;
static NSDictionary *defaultDetectorOptions = nil;

- (instancetype)initWithModuleRegistry:(EXModuleRegistry *)moduleRegistry
{
  self = [super init];
  if (self) {
    _moduleRegistry = moduleRegistry;
    fileManager = [NSFileManager defaultManager];
  }
  return self;
}

// EX_EXPORT_MODULE(ExpoFaceDetector);

- (NSDictionary *)constantsToExport
{
  return [FaceDetectorUtils constantsToExport];
}

- (void)setModuleRegistry:(EXModuleRegistry *)moduleRegistry
{
  _moduleRegistry = moduleRegistry;
}

# pragma mark - Utility methods
// https://gist.github.com/steipete/4666527
+ (int)exifOrientationFor:(UIImageOrientation)orientation
{
  switch (orientation) {
    case UIImageOrientationUp:
      return 1;
    case UIImageOrientationDown:
      return 3;
    case UIImageOrientationLeft:
      return 8;
    case UIImageOrientationRight:
      return 6;
    case UIImageOrientationUpMirrored:
      return 2;
    case UIImageOrientationDownMirrored:
      return 4;
    case UIImageOrientationLeftMirrored:
      return 5;
    case UIImageOrientationRightMirrored:
      return 7;
  }
}

@end
