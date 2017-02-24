#import <Cordova/CDVPlugin.h>
#import <UIKit/UIKit.h>
#import "GTLRDrive.h"

@class OIDAuthState;
@class GTMAppAuthFetcherAuthorization;
@class OIDServiceConfiguration;

@interface GoogleDrive : CDVPlugin

@property(nonatomic, nullable) GTMAppAuthFetcherAuthorization *authorization;

- (void)downloadFile:(CDVInvokedUrlCommand*)command;
- (void)uploadFile:(CDVInvokedUrlCommand*)command;
- (void)fileList:(CDVInvokedUrlCommand*)command;
- (void)deleteFile:(CDVInvokedUrlCommand*)command;

@end