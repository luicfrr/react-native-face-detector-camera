#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>

@interface CSBufferOrientationCalculator : NSObject

+ (CGAffineTransform)pointTransformForInterfaceOrientation:(UIInterfaceOrientation)orientation
                                            forBufferWidth:(CGFloat)bufferWidth
                                           andBufferHeight:(CGFloat)bufferHeight
                                             andVideoWidth:(CGFloat)videoWidth
                                            andVideoHeight:(CGFloat)videoHeight
                                               andMirrored:(BOOL)mirrored;

@end
