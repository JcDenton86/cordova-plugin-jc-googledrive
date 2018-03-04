/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
function displayFileList(files) {
    var output = "<div>";
    for (var i = files.length - 1; i >= 0; i--) {
        var file = files[i];
        output += "<p class='file-entry'>Name: " + file.name + ", " + file.modifiedTime;
        output += ", ID: " + file.id.slice(0, 15) + "…"
        output += "<button class='file-download-btn' data-driveid='"+file.id+"'>Download File</button></p>";
    }
    output += "</div>";
    return output;
}

var app = {
    // Application Constructor
    initialize: function () {
        this.bindEvents();
    },
    // Bind Event Listeners
    //
    // Bind any events that are required on startup. Common events are:
    // 'load', 'deviceready', 'offline', and 'online'.
    bindEvents: function () {
        document.addEventListener('deviceready', this.onDeviceReady, false);
    },
    // deviceready Event Handler
    //
    // The scope of 'this' is the event. In order to call the 'receivedEvent'
    // function, we must explicitly call 'app.receivedEvent(...);'
    onDeviceReady: function () {
        app.receivedEvent('deviceready');
    },

    clickedDownloadFile: function(event) {
        alert("This file has DriveID: '" + this.dataset.driveid + "'");
    },

    clickedListFiles: function(event) {
        var appDirectory = false;
        var resultElement = document.getElementsByClassName('drive-result')[0];
        resultElement.innerHTML = "Listing files…";

        // Rely on displayFileList to ingest the file list and output HTML
        // Then add event listeners to allow download operatiosn occur.
        window.plugins.gdrive.fileList(appDirectory,
            function(success) {
                resultElement.innerHTML = "List Files success: <br>" + displayFileList(success.flist);

                var buttons = document.getElementsByClassName('file-download-btn');
                for (var i = 0; i < buttons.length; i++) {
                    buttons[i].addEventListener('click', app.clickedDownloadFile, false);
                }
            },
            function(error) {
                resultElement.innerHTML = "List Files error: <br><pre>" + JSON.stringify(error, null, " ") + "</pre>";
         });
    },

    clickedUploadFile: function(event) {
        var appDirectory = false;
        var resultElement = document.getElementsByClassName('drive-result')[0];
        resultElement.innerHTML = "Uploading file…";

        window.plugins.gdrive.uploadFile(JSON.stringify(event), appDirectory,
            function(success) {
                resultElement.innerHTML = "Upload success: <br><pre>" + JSON.stringify(success) + "</pre>";
            },
            function(error) {
                resultElement.innerHTML = "Upload error: <br><pre>" + JSON.stringify(success) + "</pre>";
        });
    },

    clickedRequestSync: function(event) {
        // Android-only
        var returnFiles = false;
        var resultElement = document.getElementsByClassName('drive-result')[0];
        resultElement.innerHTML = "Requesting sync…";

        window.plugins.gdrive.requestSync(returnFiles,
            function(success) {
                resultElement.innerHTML = "Request sync success: <br><pre>" + JSON.stringify(success, null, " ") + "</pre>";
            },
            function(error) {
                resultElement.innerHTML = "Request sync error: <br><pre>" + JSON.stringify(error, null, " ") + "</pre>";
        });
    },

    // Update DOM on a Received Event
    receivedEvent: function (id) {
        var parentElement = document.getElementById(id);
        var listeningElement = parentElement.querySelector('.listening');
        var receivedElement = parentElement.querySelector('.received');

        listeningElement.setAttribute('style', 'display:none;');
        receivedElement.setAttribute('style', 'display:block;');

        // Google Drive Development Interface
        var statusElement = document.getElementsByClassName('drive-status')[0];

        if (window.plugins.gdrive !== undefined) {
            statusElement.setAttribute('style', 'display:block;');
            statusElement.innerHTML = "gdrive global plugin loaded";
        } else {
            statusElement.setAttribute('style', 'display:block;');
            statusElement.innerHTML = "gdrive global plugin is undefined";
        }

        var listFilesButton = document.getElementsByClassName('drive-listFiles')[0];
        listFilesButton.addEventListener('click', this.clickedListFiles, false);

        var requestSyncButton = document.getElementsByClassName('drive-requestSync')[0];
        requestSyncButton.addEventListener('click', this.clickedRequestSync, false);

        var uploadFileButton = document.getElementsByClassName('drive-uploadFile')[0];
        uploadFileButton.addEventListener('click', this.clickedUploadFile, false);
    }
};

app.initialize();
