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

Visit the [Android quickstart](https://developers.google.com/drive/android/get-started) and follow the __only__ the getting started page

### Install with cordova-cli

If you are using [cordova-cli](https://github.com/apache/cordova-cli), install
with:

    cordova plugin add cordova-plugin-googledrive --variable IOS_REVERSED_CLIENT_ID=com.googleusercontent.apps.YOUR_CLIENT_ID --variable IOS_CLIENT_ID=YOUR_CLIENT_ID.apps.googleusercontent.com
    
The REVERSED_CLIENT_ID is the "iOS URL Scheme" on the Google Developer's Console.

##### Read only for iOS

This plugin requires some additions to make it work on __iOS__ properly:

Firstly, make sure you have installed [cocoapods](https://cocoapods.org/) because it is needed to install some dependencies using `pod`. 

Secondly and after succesfull installation of the dependencies:

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

## Usage (Javascript)

If you are using jQuery, AngularJS, WinJS or any Promise/A library, promise style is supported.

#### downloadFile

The `downloadFile` function follows the proper authentication procedure. If the user allows the app to access Google Drive, this method will download the file requested (fileId) and will save it to the path you indicated (toLocalDest).

```javascript
var toLocalDest = "path/to/local/destination/";
var fileid = "GoogleDrive_FileID";
window.plugins.gdrive.downloadFile(toLocalDest, fileid,
   function (response) {
       //simple response message with the status
   },
   function (error){
     	console.log(error);
   }
);
```

#### uploadFile

The `uploadFile` function will upload a file selected from a given local path (fpath) to the root folder of user's drive.

```javascript
var fpath = "path/to/local/file.ext";
window.plugins.gdrive.uploadFile(fpath,
   function (response) {
   		//simple response message with the status
   },
   function (error){
     	console.log(error);
   }
);
```

#### fileList

The `fileList` function shows a list of files, created or uploaded by the application and have not been trashed. Selecting a file, you can have access to the drive fileId and in the created date (only on Android).

```javascript
window.plugins.gdrive.fileList(
   function(res){
        //the files are under res.flist;
	console.log(res);
   },
   function(err){
	console.log(err);
   }
);
```
The success callback will return a JSON object. The attribute `flist` will hold an array with the files found on user's Drive. 

## RoadMap
This plugin is under heavy development and it has been created as a requirement on a personal mobile project

Functions to be included:

- deleteFile
- createFolderWithName
- uploadFileInFolder (will allow folder selection)

## Contribution

You are more than welcome to provide features and help with the development.
Leaving issues or requests is accepted but my free time in not enough which means that I will try to support this plugin as long as my free time allows.  

## Credits

This plugin has been created by [Jeries Besharat](http://students.ceid.upatras.gr/~besarat)
Other people that have contributed and commited features and improvements:

* [Dionysios Papathanopoulos](https://se.linkedin.com/in/dionysios-papathanopoulos-1353a649)