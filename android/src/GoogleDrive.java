package gr.lexicon.googleDrive;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import android.content.Context;
import android.widget.Toast;

public class GoogleDrive extends CordovaPlugin {
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
      Toast.makeText(webView.getContext(),"downloadDB",Toast.LENGTH_LONG).show();
      callbackContext.success("download called");
  }

  private void uploadDB(CallbackContext callbackContext) {
        Toast.makeText(webView.getContext(),"uploadDB",Toast.LENGTH_LONG).show();
        callbackContext.success("upload called");
  }
}