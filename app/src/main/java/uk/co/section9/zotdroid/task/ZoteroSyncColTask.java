package uk.co.section9.zotdroid.task;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.section9.zotdroid.auth.ZoteroBroker;

/**
 * Created by oni on 27/07/2017.
 * This class looks at the versions and tries to work out what has changed in the meantime.
 */

public class ZoteroSyncColTask extends ZoteroTask {

    private static final String TAG = "ZoteroSyncColTask";

    ZoteroTaskCallback _callback;
    String _last_version_collections;
    String _new_version_collections;

    public ZoteroSyncColTask(ZoteroTaskCallback callback, String last_version_collections) {
        _callback = callback;
        _last_version_collections = last_version_collections;
    }

    @Override
    public void startZoteroTask() {
        execute(BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/collections");
    }

    @Override
    protected void onPostExecute(String rstring) {
        // Check we didn't get a failure on that rsync call
        if (rstring == "FAIL"){
            _callback.onSyncCollectionsVersion(false,"Version grab for items failed.","0000");
            return;
        }

        try {
            JSONObject jObject = new JSONObject(rstring);
            try {
                _new_version_collections = jObject.getString("Last-Modified-Version");
                _callback.onSyncCollectionsVersion(true,"Version grab complete", _new_version_collections);
                return;

            } catch (JSONException e) {
                Log.i(TAG, "No Last-Modified-Version in request.");
                _callback.onSyncCollectionsVersion(false,"Version grab for items failed.","0000");
                return;
            }
        } catch (JSONException e) {

        }
    }
}
