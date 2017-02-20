function GoogleDrive() {}

GoogleDrive.prototype.downloadDB = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "GoogleDrive", "downloadDB", []);
};

GoogleDrive.prototype.uploadDB = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "GoogleDrive", "uploadDB", []);
};

GoogleDrive.install = function () {
    if (!window.plugins) {
        window.plugins = {};
    }

    window.plugins.gdrive = new GoogleDrive();
    console.log(window.plugins.gdrive);
    return window.plugins.gdrive;
};

cordova.addConstructor(GoogleDrive.install);