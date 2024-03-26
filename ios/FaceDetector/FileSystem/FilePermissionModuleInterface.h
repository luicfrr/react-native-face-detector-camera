#import <FaceDetectorCamera/FileSystemInterface.h>

@protocol FilePermissionModuleInterface

- (FileSystemPermissionFlags)getPathPermissions:(NSString *)path;

@end

