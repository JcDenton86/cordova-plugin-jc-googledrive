# Cordova Google Drive plugin

This plugin allows you to authenticate and access user's Google Drive account, using the new Rest API as recommended (Read more [here](https://github.com/google/google-api-objectivec-client-for-rest)).

## Installation

### Prerequistics
``
$ git clone https://github.com/JcDenton86/cordova-plugin-jc-googledrive.git
``

1. Open the [Library page](https://console.developers.google.com/apis/library) and enable the Google Drive API for a project (create a new project if you don't have one)
2. Open the [Credentials page](https://console.developers.google.com/apis/credentials) in the API Console and follow the steps below in the given order:
    
    * Click Create credentials > OAuth client ID.
    * Select Android or iOS.
    * In the Package name field (or Bundle ID for iOS), enter your app's package name.
    * On Android: Paste the SHA1 fingerprint into the form where requested (on iOS skip this)
        * read more under "Get an Android certificate and register your application", [here](https://developers.google.com/drive/android/get-started)
    * Click Create.

### Install with cordova-cli

If you are using [cordova-cli](https://github.com/apache/cordova-cli), install
with:

    cordova plugin add cordova-plugin-jc-googledrive --variable IOS_REVERSED_CLIENT_ID=com.googleusercontent.apps.YOUR_CLIENT_ID --variable IOS_CLIENT_ID=YOUR_CLIENT_ID.apps.googleusercontent.com
    
The IOS_REVERSED_CLIENT_ID is the "iOS URL Scheme" on the Google Developer's Console. The variables are only for iOS platform. You can omit them if you are developing for Android.

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

__important__: in case you use xCode to build or run the app on simulator or device you should open the `.xcworkspace` file instead of .xcodeproj

## Usage (Javascript)

If you are using jQuery, AngularJS, WinJS or any Promise/A library, promise style is supported.

#### downloadFile

The `downloadFile` function follows the proper authentication procedure. If the user allows the app to access Google Drive, this method will download the file requested (fileId) and will save it to the path you indicated (toLocalDest).

```javascript
var toLocalDest = "path/to/local/destination/file.ext";
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

The `uploadFile` function will upload a file selected from a given local path (fpath) to the root folder of user's drive. You can upload files to the app's folder or root folder based on the boolean value of the second parameter the method takes. If you set the value to `true`, it will upload files under app's folder.
The difference between the two folders is the access permission of the file uploaded. Files under app folder, are no visible to the user and only the application has access to them. These files are uploaded under backup section with the rest of the applications.
This is useful for files with sensitive data, such as databases etc. where the user must not have access or when your app support some kind of backup action.

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

#### deleteFile

The `deleteFile` function removes from root directory previously uploaded or created file. The file is moved to the user's drive trash.

```javascript
var fileid = "GoogleDrive_FileID";
window.plugins.gdrive.deleteFile(fileid,
   function(res){
   console.log(res);
   },
   function(err){
   console.log(err);
   }
);
```

#### fileList

The `fileList` function shows a list of files, created or uploaded by the application and have not been trashed. Selecting a file, you can have access to the drive fileId and in the created date. You can list files from app's folder or root folder based on the boolean value of the first parameter the method takes. If you set the value to true, it will fetch files under app's folder.

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

#### requestSync (currently only on Android)

The `requestSync` function forces a synchronization with the Drive server. This might be useful in case fileList returns an empty array of files.

```javascript
var returnFiles = true;
window.plugins.gdrive.requestSync(returnFiles,
   function(res){
   console.log(res);
   },
   function(err){
   console.log(err);
   }
);
```
The success callback will return a JSON object. The attribute `flist` will hold an array with the files found on user's application folder on Drive.

## Contribution

You are more than welcome to provide features and help with the development.
Leaving issues or requests is accepted but my free time in not enough which means that I will try to support this plugin as long as my free time allows.  

## Credits

This plugin has been created by [Jeries Besharat](http://students.ceid.upatras.gr/~besarat)
Other people that have contributed and commited features and improvements:

* [Dionysios Papathanopoulos](https://se.linkedin.com/in/dionysios-papathanopoulos-1353a649)