#import <UIKit/UIKit.h>
#import <CoreMedia/CoreMedia.h>
#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <GoogleMLKit/MLKit.h>

typedef float (^FaceDetectionAngleTransformBlock)(float);

@interface FaceDetectorUtils : NSObject

+ (NSDictionary *)constantsToExport;

+ (BOOL)areOptionsEqual:(MLKFaceDetectorOptions *)first
                     to:(MLKFaceDetectorOptions *)second;

+ (MLKFaceDetectorOptions *)mapOptions:(NSDictionary*)options;

+ (MLKFaceDetectorOptions *)newOptions:(MLKFaceDetectorOptions*)options
                            withValues:(NSDictionary *)values;

+ (FaceDetectionAngleTransformBlock)angleTransformerFromTransform:(CGAffineTransform)transform;

+ (int)toCGImageOrientation:(UIImageOrientation)imageOrientation;

+ (NSDictionary*)defaultFaceDetectorOptions;

@end
