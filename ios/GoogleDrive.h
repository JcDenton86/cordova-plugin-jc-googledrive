#import <Cordova/CDVPlugin.h>
#import <UIKit/UIKit.h>
#import "GTLRDrive.h"

@class OIDAuthState;
@class GTMAppAuthFetcherAuthorization;
@class OIDServiceConfiguration;

@interface GoogleDrive : CDVPlugin

//@property (nonatomic, strong) GTLRDriveService *service;
@property(nonatomic, nullable) GTMAppAuthFetcherAuthorization *authorization;

- (void)downloadFile:(CDVInvokedUrlCommand*)command;
- (void)uploadFile:(CDVInvokedUrlCommand*)command;
- (void)fileList:(CDVInvokedUrlCommand*)command;

@end