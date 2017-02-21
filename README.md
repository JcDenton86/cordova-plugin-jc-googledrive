# Cordova Google Drive plugin

Access Google Drive Rest API using the recommended library. Read more [here](https://github.com/google/google-api-objectivec-client-for-rest).

## Installation

### Prerequistics
``
$ git clone https://github.com/JcDenton86/cordova-plugin-googledrive.git
``

#### For iOS

Visit the [iOS quickstart](https://developers.google.com/drive/ios/quickstart) guide and complete __only__ step 1 (Turn on the Drive API).

#### For Android

...

### Install with cordova-cli

If you are using [cordova-cli](https://github.com/apache/cordova-cli), install
with:

    cordova plugin add cordova-plugin-googledrive --variable IOS_REVERSED_CLIENT_ID=com.googleusercontent.apps.1234567890-abcdefghijklmnop12qrstuvwxyz --variable IOS_CLIENT_ID=1234567890-abcdefghijklmnop12qrstuvwxyz.apps.googleusercontent.com
    
This plugin requires some additions to make it work on __iOS__ properly:

The plugin will install the dependencies using `pod`. So first make sure you have installed [cocoapods](https://cocoapods.org/).

Open `AppDelegate.m` file and add 1) these header files along with the rest on the top and 2) the code block before the `@end` command:

```
#import "AppAuth.h"
#import "GoogleDrive.h"

//....

- (BOOL)application:(UIApplication *)app
            openURL:(NSURL *)url
            options:(NSDictionary<NSString *, id> *)options {
    
    if ([_currentAuthorizationFlow resumeAuthorizationFlowWithURL:url]) {
        _currentAuthorizationFlow = nil;
        return YES;
    }
    
    return NO;
}
//...
@end

```
Open `AppDelegate.h` file and paste this code before the `@end` command :
```
@protocol OIDAuthorizationFlowSession;

@interface AppDelegate : CDVAppDelegate {}
@property(nonatomic, strong, nullable) id<OIDAuthorizationFlowSession> currentAuthorizationFlow;

```

That's it! You are ready to use the plugin. 

## Use from Javascript

If you are using jQuery, AngularJS, WinJS or any Promise/A library, promise style is supported. Use something like:

    var toLocalDest = "path/to/local/destination/";
    var fileid = "GoogleDrive_FileID";
    window.plugins.gdrive.downloadFile(toLocalDest, fileid, function (response) {
        //simple response message with the status
    });
    
    var fpath = "path/to/local/file.ext";
    window.plugins.gdrive.uploadFile(fpath,function (response) {
        //simple response message with the status
    });
    
    window.plugins.gdrive.fileList(function(res){
        //the files are under res.flist;
        console.log(res);
    },function(err){console.log(err);});
    
Javascript Methods currently supported:

#### downloadFile

Downloads previously uploaded file on user's Google Drive

#### uploadFile

Uploads to user's Google Drive a local copy of a file

#### fileList

fetch all the files created/uploaded by the app (which have not been trashed)

## Contribution
This plugin is under heavy development and it has been created as a requirement on a personal mobile project. However, you are more than welcome to provide features and help with the development.

Leaving issues or requests is accepted but my free time in not enough which means that I will try to support this plugin as long as my free time allows.  

## Credits

This plugin has been created by [Jeries Besharat](http://studens.ceid.upatras.gr/~besarat)
Other people that have contributed and commited features and improvements:

* [Dionisios Papathanopoulos](https://se.linkedin.com/in/dionysios-papathanopoulos-1353a649)