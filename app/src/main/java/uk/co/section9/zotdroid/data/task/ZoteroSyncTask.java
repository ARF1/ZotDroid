package uk.co.section9.zotdroid.data.task;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.section9.zotdroid.ZoteroBroker;

/**
 * Created by oni on 27/07/2017.
 * This class looks at the versions and tries to work out what has changed in the meantime.
 */

public class ZoteroSyncTask extends ZoteroTask {

    private static final String TAG = "ZoteroSyncTask";

    ZoteroTaskCallback _callback;
    // TODO - are these actually separate?
    String _last_version_items;
    String _last_version_collections;
    String _new_version_items;
    String _new_version_collections;
    boolean _done_items = false;

    public ZoteroSyncTask(ZoteroTaskCallback callback, String last_version_items, String last_version_collections) {
        _callback = callback;
        _last_version_items = last_version_items;
        _last_version_collections = last_version_collections;
        _done_items = false;
    }

    @Override
    public void startZoteroTask() {
        super.execute(BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/items");
    }

    @Override
    protected void onPostExecute(String rstring) {
        // Check we didn't get a failure on that rsync call
        if (rstring == "FAIL"){
            _callback.onSyncVersion(this,false,"Version grab for items failed.","0000","0000");
            return;
        }

        try {
            JSONObject jObject = new JSONObject(rstring);
            try {
                if (_done_items) {
                    _new_version_collections = jObject.getString("Last-Modified-Version");
                    _callback.onSyncVersion(this,true,"Version grab complete", _new_version_items, _new_version_collections);
                    return;
                } else {
                    _new_version_items = jObject.getString("Last-Modified-Version");
                    _done_items = true;

                    // Bit messy this but it works well enough for us I think.
                    ZoteroSyncTask zs = new ZoteroSyncTask(_callback,_new_version_items, _last_version_collections);
                    zs._new_version_items = _new_version_items;
                    zs._done_items = true;
                    zs.execute(BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/collections");
                }

            } catch (JSONException e) {
                Log.i(TAG, "No Last-Modified-Version in request.");
                _callback.onSyncVersion(this,false,"Version grab for items failed.","0000","0000");
                return;
            }
        } catch (JSONException e) {

        }
    }
}
