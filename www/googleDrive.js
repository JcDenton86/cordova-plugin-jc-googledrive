function GoogleDrive() {}

GoogleDrive.prototype.downloadFile = function (destinationURL,fileid,successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "GoogleDrive", "downloadFile", [destinationURL,fileid]);
};

GoogleDrive.prototype.uploadFile = function (fpath,successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "GoogleDrive", "uploadFile", [fpath]);
};

GoogleDrive.prototype.fileList = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "GoogleDrive", "fileList", []);
};

GoogleDrive.install = function () {
    if (!window.plugins) {
        window.plugins = {};
    }

    window.plugins.gdrive = new GoogleDrive();
    return window.plugins.gdrive;
};

cordova.addConstructor(GoogleDrive.install);