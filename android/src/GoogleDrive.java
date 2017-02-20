package gr.lexicon.googleDrive;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gr.lexicon.team.MainActivity;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class GoogleDrive extends CordovaPlugin  implements EasyPermissions.PermissionCallbacks {

    private String dbname = "lexiconMe.db";
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    GoogleAccountCredential mCredential;
    private static final String[] SCOPES = { DriveScopes.DRIVE_METADATA_READONLY };
    private static final String PREF_ACCOUNT_NAME = "accountName";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        //JSONObject arg_object = args.getJSONObject(0);

        if ("downloadDB".equals(action)) {
            downloadDB(callbackContext);
            return true;
        }
        else if("uploadDB".equals(action)){
            uploadDB(callbackContext);
            return true;
        }


        return false;
    }

    private void downloadDB(CallbackContext callbackContext) {
        Toast.makeText(webView.getContext(),"just writing something", Toast.LENGTH_LONG).show();
        //Toast.makeText(webView.getContext(),"downloadDB",Toast.LENGTH_LONG).show();
        File dbfile = cordova.getActivity().getDatabasePath(dbname);
        if (dbfile != null){
            Toast.makeText(webView.getContext(),"file found" + dbfile.getName(), Toast.LENGTH_LONG).show();
            Log.i("test", "file found calling getResultsFromAPi");
            // Initialize credentials and service object.
            mCredential = GoogleAccountCredential.usingOAuth2(
                    cordova.getActivity(), Arrays.asList(SCOPES))
                    .setBackOff(new ExponentialBackOff());
            getResultsFromApi();
        }
        callbackContext.success("download called");
    }

    private void uploadDB(CallbackContext callbackContext) {
        Toast.makeText(webView.getContext(),"uploadDB",Toast.LENGTH_LONG).show();
        callbackContext.success("upload called");
    }



    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        Log.i("test", "Entering getResultsFromAPi 1" + isGooglePlayServicesAvailable());
        Log.i("test", "Entering getResultsFromAPi 2" + isDeviceOnline());
        Log.i("test", "Entering getResultsFromAPi 3" + mCredential.getSelectedAccountName());
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            Log.i("test", "passing second check");
            chooseAccount();
        } else if (! isDeviceOnline()) {
            //Toast.makeText(webView.getContext(),"No Internet Connection!!!",Toast.LENGTH_LONG).show();
        } else {
            Log.i("test", "Creating the Task");
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * An asynchronous task that handles the Drive API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Drive API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Drive API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of up to 10 file names and IDs.
         * @return List of Strings describing files, or an empty list if no files
         *         found.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Get a list of up to 10 files.
            List<String> fileInfo = new ArrayList<String>();
            FileList result = mService.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
//        List<File> files = result.getFiles();
//        if (files != null) {
//            for (File file : files) {
//                fileInfo.add(String.format("%s (%s)\n",
//                        file.getName(), file.getId()));
//            }
//        }
            return fileInfo;
        }
    }


    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) cordova.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        Log.i("test", "choose Account");
        if (EasyPermissions.hasPermissions(cordova.getActivity(), Manifest.permission.GET_ACCOUNTS)) {
            //changed the prefernces here
            String accountName = cordova.getActivity().getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
//                startActivityForResult(
//                        mCredential.newChooseAccountIntent(),
//                        REQUEST_ACCOUNT_PICKER);
                Toast.makeText(webView.getContext(),"Asking for Permissions", Toast.LENGTH_LONG).show();

            }
        } else {
            // TODO: 15/02/2017 The app stops here for now
            Log.i("test", "maybe some problem with the permission get accounts");
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    cordova.getActivity(),
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }


    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(cordova.getActivity());
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(cordova.getActivity());
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog( final int connectionStatusCode) {
       // GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
       // Dialog dialog = apiAvailability.getErrorDialog(context, connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES);
       // dialog.show();
        Toast.makeText(webView.getContext(),"Error with GooglePlay servecs!!! " + REQUEST_GOOGLE_PLAY_SERVICES,Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    }
}