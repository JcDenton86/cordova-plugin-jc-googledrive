function GoogleDrive() {}

GoogleDrive.prototype.downloadFile = function (destinationURL,fileid,successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "GoogleDrive", "downloadFile", [destinationURL,fileid]);
};

GoogleDrive.prototype.uploadFile = function (fpath,appfolder,successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "GoogleDrive", "uploadFile", [fpath,appfolder]);
};

GoogleDrive.prototype.fileList = function (appfolder,successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "GoogleDrive", "fileList", [appfolder]);
};

GoogleDrive.prototype.deleteFile = function (fileid,successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "GoogleDrive", "deleteFile", [fileid]);
};

GoogleDrive.prototype.requestSync = function(returnFiles,successCallback,errorCallback){
    cordova.exec(successCallback,errorCallback,"GoogleDrive","requestSync",[returnFiles]);
};

GoogleDrive.install = function () {
    if (!window.plugins) {
        window.plugins = {};
    }

    window.plugins.gdrive = new GoogleDrive();
    return window.plugins.gdrive;
};

cordova.addConstructor(GoogleDrive.install);