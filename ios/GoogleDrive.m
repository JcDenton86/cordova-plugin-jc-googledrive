#import "GoogleDrive.h"
#import <AppAuth/AppAuth.h>
#import <GTMAppAuth/GTMAppAuth.h>
#import "GTLRUtilities.h"
#import "AppDelegate.h"


static NSString *kClientID = @"";
static NSString *kRedirectURI =@":/oauthredirect";
static NSString *kAuthorizerKey = @"";

@interface GoogleDrive () <OIDAuthStateChangeDelegate,OIDAuthStateErrorDelegate>
@property (nonatomic, readonly) GTLRDriveService *driveService;
@end

@implementation GoogleDrive {}

- (void)pluginInitialize {
    kAuthorizerKey = [[[NSBundle mainBundle] infoDictionary] objectForKey:(NSString*)kCFBundleNameKey];
    NSLog(@"%@",kAuthorizerKey);
    NSMutableArray *ids = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleURLTypes"];
    NSArray *reversedClientId = [ids filteredArrayUsingPredicate:[NSPredicate predicateWithFormat:@"CFBundleURLName == %@", @"reversedClientId"]];
    NSArray *clientId = [ids filteredArrayUsingPredicate:[NSPredicate predicateWithFormat:@"CFBundleURLName == %@", @"clientId"]];
    kRedirectURI = [[[[reversedClientId valueForKey:@"CFBundleURLSchemes"] objectAtIndex:0 ] objectAtIndex:0] stringByAppendingString:kRedirectURI];
    kClientID = [[[clientId valueForKey:@"CFBundleURLSchemes"] objectAtIndex:0 ] objectAtIndex:0];
    [self loadState];
    //NSLog(@"%@",kRedirectURI);
    //NSLog(@"%@",kClientID);
}


- (void)downloadFile:(CDVInvokedUrlCommand*)command
{
    NSString* destPath = [command.arguments objectAtIndex:0];
    NSString* fileid = [command.arguments objectAtIndex:1];
    if([destPath stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]].length>0){
        dispatch_async(dispatch_get_main_queue(), ^{
            if(self.authorization.canAuthorize){
                    [self downloadAFile:command destPath:destPath fid:fileid];
                    NSLog(@"Already authorized app. No need to ask user again");
            } else{
                [self runSigninThenHandler:command onComplete:^{
                    [self downloadAFile:command destPath:destPath fid:fileid];
                }];
            }
        });
    } else {
        [self.commandDelegate sendPluginResult:[CDVPluginResult
                                                resultWithStatus:CDVCommandStatus_ERROR
                                                messageAsString:@"One of the parameters is empty"]
                                    callbackId:command.callbackId];
    }
}

- (void)uploadFile:(CDVInvokedUrlCommand*)command
{
    NSString* path = [command.arguments objectAtIndex:0];
    BOOL appfolder = [[command.arguments objectAtIndex:1] boolValue];
    if([path stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]].length>0){
        dispatch_async(dispatch_get_main_queue(), ^{
            if(self.authorization.canAuthorize){
                    [self uploadAFile:command fpath:path appFolder:appfolder];
                    NSLog(@"Already authorized app. No need to ask user again");
            } else{
                [self runSigninThenHandler:command onComplete:^{
                    [self uploadAFile:command fpath:path appFolder:appfolder];
                }];
            }
        });
    } else {
            [self.commandDelegate sendPluginResult:[CDVPluginResult
                                                    resultWithStatus:CDVCommandStatus_ERROR
                                                    messageAsString:@"One of the parameters is empty"]
                                        callbackId:command.callbackId];
    }

}

- (void)fileList:(CDVInvokedUrlCommand*)command{

    BOOL appfolder = [[command.arguments objectAtIndex:0] boolValue];
    dispatch_async(dispatch_get_main_queue(), ^{
        if(self.authorization.canAuthorize){
            [self fetchFileList:command appFolder:appfolder];
            NSLog(@"Already authorized app. No need to ask user again");
        } else{
            [self runSigninThenHandler:command onComplete:^{
                [self fetchFileList:command appFolder:appfolder];
            }];
        }
    });
}

- (void)deleteFile:(CDVInvokedUrlCommand*)command{

    NSString* fileid = [command.arguments objectAtIndex:0];
    if([fileid stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]].length>0){
            dispatch_async(dispatch_get_main_queue(), ^{
                if(self.authorization.canAuthorize){
                    [self deleteSelectedFile:command fid:fileid];
                    NSLog(@"Already authorized app. No need to ask user again");
                } else{
                    [self runSigninThenHandler:command onComplete:^{
                        [self deleteSelectedFile:command fid:fileid];
                    }];
                }
        });
    } else{
        [self.commandDelegate sendPluginResult:[CDVPluginResult
                                                resultWithStatus:CDVCommandStatus_ERROR
                                                messageAsString:@"One of the parameters is empty"]
                                    callbackId:command.callbackId];
    }
}

- (void)downloadAFile:(CDVInvokedUrlCommand*)command destPath:(NSString*)destPath fid:(NSString*)fileid {
    NSURL *fileToDownloadURL = [NSURL fileURLWithPath:destPath];
    //NSLog(@"%@", fileToDownloadURL);
    //NSLog(@"%@",fileid);

    GTLRDriveService *service = self.driveService;
    GTLRQuery *query = [GTLRDriveQuery_FilesGet queryForMediaWithFileId:fileid];
    [service executeQuery:query
        completionHandler:^(GTLRServiceTicket *callbackTicket,
                            GTLRDataObject *object,
                            NSError *callbackError) {
            NSError *errorToReport = callbackError;
            NSError *writeError;
            if (callbackError == nil) {
                BOOL didSave = [object.data writeToURL:fileToDownloadURL
                                               options:NSDataWritingAtomic
                                                 error:&writeError];
                if (!didSave) {
                    errorToReport = writeError;
                }
            }
            CDVPluginResult* pluginResult = nil;
            if (errorToReport == nil) {
                NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
                [result setObject:@"File downloaded succesfully and saved to path" forKey:@"message"];
                [result setObject:[fileToDownloadURL path] forKey:@"destPath"];
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
            } else {
                [callbackTicket cancelTicket];
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[errorToReport localizedDescription]];
            }
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
}

- (void)fetchFileList:(CDVInvokedUrlCommand*)command appFolder:(BOOL)appfolder {
    GTLRDriveService *service = self.driveService;
    GTLRDriveQuery_FilesList *query = [GTLRDriveQuery_FilesList query];

    query.fields = @"nextPageToken,files(id,name,trashed,modifiedTime)";
    if(appfolder)
        query.spaces = @"appDataFolder";

    //query.orderBy=@"modifiedDate";

    [service executeQuery:query
        completionHandler:^(GTLRServiceTicket *callbackTicket,
                            GTLRDrive_FileList *fileList,
                            NSError *callbackError) {
            // Callback
            NSArray *notTrashed = [[fileList files] filteredArrayUsingPredicate:[NSPredicate predicateWithFormat:@"trashed == %d", 0]];
            NSMutableArray *res = [[NSMutableArray alloc] init];
            if (notTrashed.count > 0) {
                for (GTLRDrive_File *file in notTrashed) {
                    [res addObject:file.JSON];
                }
            }
            CDVPluginResult* pluginResult = nil;
            if (callbackError == nil) {
                NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
                [result setObject:@"Retrived file list succesfully!" forKey:@"message"];
                [result setObject:res forKey:@"flist"];
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
            } else {
                [callbackTicket cancelTicket];
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[callbackError localizedDescription]];
            }
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
}


-(void)uploadAFile:(CDVInvokedUrlCommand*)command fpath:(NSString*) fpath appFolder:(BOOL)appfolder{

    NSURL *fileToUploadURL = [NSURL fileURLWithPath:fpath];
    NSLog(@"%@", fileToUploadURL);

    NSError *fileError;
    if (![fileToUploadURL checkPromisedItemIsReachableAndReturnError:&fileError]) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[@"No Local File Found: " stringByAppendingString:fpath]]
                                    callbackId:command.callbackId];
    }
    //NSString *libs = [NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, YES) objectAtIndex: 0];
    //NSLog(@"Detected Library path: %@", libs);

    GTLRDriveService *service = self.driveService;

    GTLRUploadParameters *uploadParameters =
    [GTLRUploadParameters uploadParametersWithFileURL:fileToUploadURL
                                             MIMEType:@"application/octet-stream"];

    uploadParameters.useBackgroundSession = YES;

    GTLRDrive_File *backUpFile = [GTLRDrive_File object];
    if(appfolder)
        backUpFile.parents = @[@"appDataFolder"];
    backUpFile.name = [fpath lastPathComponent];
    //NSLog(@"%@",backUpFile.name);

    GTLRDriveQuery_FilesCreate *query =
    [GTLRDriveQuery_FilesCreate queryWithObject:backUpFile
                               uploadParameters:uploadParameters];

    //TODO: show native progress indicator
    query.executionParameters.uploadProgressBlock = ^(GTLRServiceTicket *callbackTicket,
                                                      unsigned long long numberOfBytesRead,
                                                      unsigned long long dataLength) {
        //double maxValue = (double)dataLength;
        //double doubleValue = (double)numberOfBytesRead;
    };

    [service executeQuery:query
        completionHandler:^(GTLRServiceTicket *callbackTicket,
                            GTLRDrive_File *uploadedFile,
                            NSError *callbackError) {

            //NSLog(@"%@", NSStringFromClass([uploadedFile class]));
            CDVPluginResult* pluginResult = nil;
            if (callbackError == nil) {
                NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
                [result setObject:@"File uploaded succesfully!" forKey:@"message"];
                [result setObject:[NSString stringWithFormat:@"%@", [NSDate date]] forKey:@"created_date"];
                [result setObject:[uploadedFile identifier] forKey:@"fileId"];
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
            } else {
                [callbackTicket cancelTicket];
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[callbackError localizedDescription]];
            }
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
}

- (void)deleteSelectedFile:(CDVInvokedUrlCommand*)command fid:(NSString*) fileid{
    GTLRDriveService *service = self.driveService;

    GTLRDriveQuery_FilesDelete *query = [GTLRDriveQuery_FilesDelete queryWithFileId:fileid];
    [service executeQuery:query completionHandler:^(GTLRServiceTicket *callbackTicket,
                                                    id nilObject,
                                                    NSError *callbackError) {
        CDVPluginResult* pluginResult = nil;
        if (callbackError == nil) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[callbackError localizedDescription]];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)createAFolder:(CDVInvokedUrlCommand*)command dirName:(NSString*) title{
    GTLRDriveService *service = self.driveService;

    GTLRDrive_File *folderObj = [GTLRDrive_File object];
    folderObj.name = title;
    folderObj.mimeType = @"application/vnd.google-apps.folder";

    GTLRDriveQuery_FilesCreate *query =
    [GTLRDriveQuery_FilesCreate queryWithObject:folderObj
                               uploadParameters:nil];

    [service executeQuery:query
        completionHandler:^(GTLRServiceTicket *callbackTicket,
                            GTLRDrive_File *folderItem,
                            NSError *callbackError) {
            // Callback
            CDVPluginResult* pluginResult = nil;
            if (callbackError == nil) {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"OK!"];
            } else {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[callbackError localizedDescription]];
            }
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];

}

- (void)runSigninThenHandler:(CDVInvokedUrlCommand*)command onComplete:(void (^)(void))handler{
    NSURL *redirectURI = [NSURL URLWithString:kRedirectURI];

    OIDServiceConfiguration *configuration = [GTMAppAuthFetcherAuthorization configurationForGoogle];
    NSArray<NSString *> *scopes = @[ kGTLRAuthScopeDriveFile, OIDScopeEmail,
                                     kGTLRAuthScopeDriveAppdata];
    OIDAuthorizationRequest *request = [[OIDAuthorizationRequest alloc] initWithConfiguration:configuration
                                                                                     clientId:kClientID
                                                                                 clientSecret:nil
                                                                                       scopes:scopes
                                                                                  redirectURL:redirectURI
                                                                                 responseType:OIDResponseTypeCode
                                                                         additionalParameters:nil];

    AppDelegate *appDelegate = (AppDelegate *)[UIApplication sharedApplication].delegate;
    NSLog(@"Initiating authorization request with scope: %@", request.scope);

    appDelegate.currentAuthorizationFlow = [OIDAuthState authStateByPresentingAuthorizationRequest:request
                                                                          presentingViewController:self.viewController
                                                                                          callback:^(OIDAuthState *_Nullable authState,
                                                                                                     NSError *_Nullable error) {
                                                                                              if (authState) {
                                                                                                  GTMAppAuthFetcherAuthorization *authorization =
                                                                                                  [[GTMAppAuthFetcherAuthorization alloc] initWithAuthState:authState];

                                                                                                  [self setGtmAuthorization:authorization];
                                                                                                  self.driveService.authorizer = authorization;
                                                                                                  NSLog(@"Got authorization tokens. Access token: %@",
                                                                                                        authState.lastTokenResponse.accessToken);
                                                                                                  if (handler) handler();
                                                                                              } else {
                                                                                                  [self setGtmAuthorization:nil];
                                                                                                  [self.commandDelegate sendPluginResult:                                                                                                   [CDVPluginResult resultWithStatus:                                                                                                CDVCommandStatus_ERROR messageAsString:[error localizedDescription]] callbackId:command.callbackId];                                                                                                NSLog(@"Authorization error: %@", [error localizedDescription]);
                                                                                              }
                                                                                          }];
}

- (void)setGtmAuthorization:(GTMAppAuthFetcherAuthorization*)authorization {
    if ([_authorization isEqual:authorization]) {
        return;
    }
    _authorization = authorization;
    [self stateChanged];
}

- (void)stateChanged {
    [self saveState];
}

- (void)didChangeState:(OIDAuthState *)state {
    [self stateChanged];
}

- (void)saveState {
    if (_authorization.canAuthorize) {
        [GTMAppAuthFetcherAuthorization saveAuthorization:_authorization
                                        toKeychainForName:kAuthorizerKey];
    } else {
        [GTMAppAuthFetcherAuthorization removeAuthorizationFromKeychainForName:kAuthorizerKey];
    }
}

- (void)loadState {
    GTMAppAuthFetcherAuthorization* authorization = [GTMAppAuthFetcherAuthorization authorizationFromKeychainForName:kAuthorizerKey];
    [self setGtmAuthorization:authorization];
}

- (void)authState:(OIDAuthState *)state didEncounterAuthorizationError:(NSError *)error {
    NSLog(@"Received authorization error: %@", error);
}

- (GTLRDriveService *)driveService {
    static GTLRDriveService *service;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        service = [[GTLRDriveService alloc] init];
        service.shouldFetchNextPages = YES;
        service.retryEnabled = YES;
    });
    return service;
}

@end