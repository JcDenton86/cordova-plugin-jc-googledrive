package gr.jcdenton;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.app.Activity.RESULT_OK;

public class GoogleDrive extends CordovaPlugin implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GoogleDrivePlugin";
    private static final int REQUEST_CODE_RESOLUTION = 3;
    private GoogleApiClient mGoogleApiClient;
    private String mAction = "";
    private String toLocalDest;
    private String fileid;
    private String localFPath;
    private boolean appFolder, listOfFiles;
    private CallbackContext mCallbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView){
        super.initialize(cordova, webView);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(cordova.getActivity())
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        Log.i(TAG,"Plugin initialized");
    }

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        mCallbackContext = callbackContext;
        mAction = action;
        if ("downloadFile".equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        //initialize global args before checking Google client connection status, as they will result null in onConnected() callback
                        toLocalDest = args.getString(0);
                        fileid = args.getString(1);
                        if(mGoogleApiClient.isConnected()) {
                            if (toLocalDest.trim().length() > 0 && fileid.trim().length() > 0)
                                downloadFile(toLocalDest, fileid);
                            else
                                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "one of the parameters is empty"));
                        } else
                            mGoogleApiClient.connect();
                    } catch (JSONException ex){
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,ex.getLocalizedMessage()));
                    }
                }
            });
            return true;
        } else if("uploadFile".equals(action)){
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        localFPath = args.getString(0);
                        appFolder = args.getBoolean(1);
                        if(mGoogleApiClient.isConnected()) {
                            if(localFPath.trim().length()>0)
                                uploadFile(localFPath, appFolder);
                            else
                                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,"one of the parameters is empty"));
                        } else
                            mGoogleApiClient.connect();
                    }catch(JSONException ex){
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,ex.getLocalizedMessage()));
                    }
                }
            });
            return true;
        } else if("fileList".equals(action)){
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        appFolder = args.getBoolean(0);
                        if(mGoogleApiClient.isConnected()) {
                            fileList(appFolder);
                        } else
                            mGoogleApiClient.connect();
                    }catch(JSONException ex){
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,ex.getLocalizedMessage()));
                    }

                }
            });
            return true;
        } else if("deleteFile".equals(action)){
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        fileid = args.getString(0);
                        if(mGoogleApiClient.isConnected()) {
                            if (fileid.trim().length() > 0)
                                deleteFile(fileid);
                            else
                                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "one of the parameters is empty"));
                        }else
                            mGoogleApiClient.connect();
                    } catch (JSONException ex){
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,ex.getLocalizedMessage()));
                    }
                }
            });
            return true;
        } else if("requestSync".equals(action)){
              cordova.getThreadPool().execute(new Runnable() {
                  @Override
                  public void run() {
                      try {
                          listOfFiles = args.getBoolean(0);
                          if (mGoogleApiClient.isConnected())
                              requestSync(listOfFiles);
                          else
                              mGoogleApiClient.connect();
                      }catch (JSONException ex){
                          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,ex.getLocalizedMessage()));
                      }
                  }
              });
              return true;
        }
        return false;
    }

    private void downloadFile(final String destPath,String fileid) {
        DriveFile.DownloadProgressListener listener = new DriveFile.DownloadProgressListener() {
            @Override
            public void onProgress(long bytesDownloaded, long bytesExpected) {
                int progress = (int) (bytesDownloaded * 100 / bytesExpected);
                Log.d(TAG, String.format("Loading progress: %d percent", progress));
                //mProgressBar.setProgress(progress);
            }
        };
        final DriveFile file = DriveId.decodeFromString(fileid).asDriveFile();
        file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, listener)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(@NonNull final DriveApi.DriveContentsResult result) {
                        final DriveContents driveContents = result.getDriveContents();

                        if (!result.getStatus().isSuccess()) {
                            mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,"Something went wrong with file download"));
                            return;
                        }
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    InputStream inputStream = driveContents.getInputStream();
                                    OutputStream outputStream = new FileOutputStream(destPath);//driveContents.getOutputStream();
                                    if (inputStream != null) {
                                        byte[] data = new byte[1024];
                                        while (inputStream.read(data) != -1) {
                                            outputStream.write(data);
                                        }
                                        inputStream.close();
                                    }
                                    outputStream.close();
                                    try {
                                        mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, new JSONObject().put("fileId",file.getDriveId())));
                                    } catch (JSONException ex) {
                                        Log.i(TAG, ex.getMessage());
                                    }
                                } catch (IOException e) {
                                    Log.e(TAG, e.getMessage());
                                }
                            }
                        }.start();
                    }
                });
    }

    private void uploadFile(final String fpath, final boolean appFolder) {
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        final DriveContents driveContents = result.getDriveContents();

                        if (!result.getStatus().isSuccess()) {
                            mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Failed to create new contents"));
                            return;
                        }

                        new Thread() {
                            @Override
                            public void run() {
                                Log.i(TAG, "New contents created.");
                                OutputStream outputStream = driveContents.getOutputStream();
                                Uri fPathURI = Uri.fromFile(new File(fpath));
                                try{
                                    InputStream inputStream = cordova.getActivity().getContentResolver().openInputStream(fPathURI);
                                    if (inputStream != null) {
                                        byte[] data = new byte[1024];
                                        while (inputStream.read(data) != -1) {
                                            outputStream.write(data);
                                        }
                                        inputStream.close();
                                    }
                                    outputStream.close();
                                } catch (IOException e) {
                                    Log.e(TAG, e.getMessage());
                                }

                                String fname = fPathURI.getLastPathSegment();
                                //Log.i(TAG,fname);
                                MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                        .setMimeType("application/octet-stream")
                                        .setTitle(fname)
                                        .build();
                                DriveFolder uploadFolder = Drive.DriveApi.getRootFolder(mGoogleApiClient);
                                if(appFolder)
                                    uploadFolder = Drive.DriveApi.getAppFolder(mGoogleApiClient);

                                uploadFolder
                                        .createFile(mGoogleApiClient, metadataChangeSet, driveContents)
                                        .setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
                                            @Override
                                            public void onResult(DriveFolder.DriveFileResult result) {
                                                if (result.getStatus().isSuccess()) {
                                                    try {
                                                        mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, new JSONObject().put("fileId", result.getDriveFile().getDriveId())));
                                                    } catch (JSONException ex) {
                                                        Log.i(TAG, ex.getMessage());
                                                    }
                                                    Log.i(TAG, result.getDriveFile().getDriveId() + "");
                                                }
                                            }
                                        });
                            }
                        }.start();
                    }
                });
    }

    private void fileList(final boolean appFolder) {

        Query.Builder qb = new Query.Builder();
        qb.addFilter(Filters.and(
                Filters.eq(SearchableField.MIME_TYPE, "application/octet-stream"),
                Filters.eq(SearchableField.TRASHED, false)));

        if(appFolder) {
            DriveId appFolderId = Drive.DriveApi.getAppFolder(mGoogleApiClient).getDriveId();
            qb.addFilter(Filters.in(SearchableField.PARENTS, appFolderId));
        }

        Query query = qb.build();

        Drive.DriveApi.query(mGoogleApiClient, query)
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,"failed to retrieve file list"));
                            return;
                        }
                        MetadataBuffer flist = result.getMetadataBuffer();
                        JSONArray response = new JSONArray();
                        for (Metadata file: flist
                                ) {
                            try {
                                response.put(new JSONObject().put("name", file.getTitle()).put("modifiedTime", file.getCreatedDate().toString()).put("id", file.getDriveId()));
                            }catch (JSONException ex){}
                        }
                        JSONObject flistJSON = new JSONObject();
                        try{
                            flistJSON.put("flist", response);
                        } catch (JSONException ex){}
                        mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,flistJSON));
                        flist.release();
                        //Log.i(TAG,flist.toString());
                    }
                });
    }

    private void deleteFile(String fileid){
        DriveId.decodeFromString(fileid).asDriveFile().getMetadata(mGoogleApiClient).setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
            @Override
            public void onResult(@NonNull DriveResource.MetadataResult result) {
                if (!result.getStatus().isSuccess()) {
                    mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,"Something went wrong with file "));
                    return;
                }
                final Metadata metadata = result.getMetadata();
                //Log.i(TAG, metadata.getTitle());
                DriveFile f = metadata.getDriveId().asDriveFile();
                if(metadata.isTrashable() && !metadata.isTrashed()){
                    f.trash(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (!status.isSuccess()) {
                                mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,"failed to trash file with id "+metadata.getDriveId() ));
                                return;
                            }
                            mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
                        }
                    });
                    //this will permanently remove file from backup app folder.
                } else {
                    f.delete(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (!status.isSuccess()) {
                                mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,"failed to delete file with id "+metadata.getDriveId() ));
                                return;
                            }
                            mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
                        }
                    });
                }
            }
        });
    }

    private void requestSync(final boolean listOfFiles){
        Drive.DriveApi.requestSync(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (!status.isSuccess()) {
                    mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,status+""));
                }
                if(listOfFiles) {
                    //after syncing with Google Drive fetch files from private app's folder
                    fileList(true);
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK) {
            mGoogleApiClient.connect();
        } else {
            mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,"user cancelled authorization"));
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(cordova.getActivity(), result.getErrorCode(), 0).show();
            return;
        }
        try {
            Log.i(TAG,"trying to resolve issue...");
            cordova.setActivityResultCallback(this);//
            result.startResolutionForResult(cordova.getActivity(), REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "API client connected.");
        if(mAction.equals("downloadFile")){
            downloadFile(toLocalDest,fileid);
        } else if(mAction.equals("uploadFile")){
            uploadFile(localFPath,appFolder);
        } else if(mAction.equals("fileList")){
            fileList(appFolder);
        } else if(mAction.equals("deleteFile")){
            deleteFile(fileid);
        } else if (mAction.equals("requestSync")){
            requestSync(listOfFiles);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }
}