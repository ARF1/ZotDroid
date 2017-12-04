package uk.co.section9.zotdroid.task;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import uk.co.section9.zotdroid.Constants;
import uk.co.section9.zotdroid.auth.ZoteroBroker;

/**
 * Created by oni on 27/07/2017.
 * This class looks at the versions and tries to work out what has changed in the meantime.
 */

public class ZoteroSyncItemsTask extends ZoteroGet {

    private static final String TAG = "ZoteroSyncColTask";

    ZoteroTaskCallback _callback;
    // TODO - are these actually separate?
    String _last_version_items;
    String _new_version_items;

    public ZoteroSyncItemsTask(ZoteroTaskCallback callback, String last_version_items) {
        _callback = callback;
        _last_version_items = last_version_items;
    }

    @Override
    public void startZoteroTask() {
        execute(Constants.BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/items");
    }

    @Override
    protected void onPostExecute(String rstring) {
        // Check we didn't get a failure on that rsync call
        if (rstring == "FAIL"){
            _callback.onSyncItemsVersion(false,"Version grab for items failed.","0000");
            return;
        }

        try {
            JSONObject jObject = new JSONObject(rstring);
            try {
                _new_version_items = jObject.getString("Last-Modified-Version");
                _callback.onSyncItemsVersion(true,"Version grab complete", _new_version_items);
                return;

            } catch (JSONException e) {
                Log.i(TAG, "No Last-Modified-Version in request.");
                _callback.onSyncItemsVersion(false,"Version grab for items failed.","0000");
                return;
            }
        } catch (JSONException e) {

        }
    }
}
