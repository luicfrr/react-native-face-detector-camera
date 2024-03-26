#import <Foundation/Foundation.h>

typedef NS_OPTIONS(unsigned int, FileSystemPermissionFlags) {
  FileSystemPermissionNone = 0,
  FileSystemPermissionRead = 1 << 1,
  FileSystemPermissionWrite = 1 << 2,
};

@protocol FileSystemInterface

@property (nonatomic, readonly) NSString *documentDirectory;
@property (nonatomic, readonly) NSString *cachesDirectory;

- (FileSystemPermissionFlags)permissionsForURI:(NSURL *)uri;
- (nonnull NSString *)generatePathInDirectory:(NSString *)directory withExtension:(NSString *)extension;
- (BOOL)ensureDirExistsWithPath:(NSString *)path;

@end
