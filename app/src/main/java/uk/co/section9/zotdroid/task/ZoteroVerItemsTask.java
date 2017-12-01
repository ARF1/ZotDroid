package uk.co.section9.zotdroid.task;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Vector;

import uk.co.section9.zotdroid.Constants;
import uk.co.section9.zotdroid.auth.ZoteroBroker;

/**
 * Created by oni on 27/07/2017.
 */

public class ZoteroVerItemsTask extends ZoteroGet {

    private static final String TAG = "ZoteroVerItemsTask";

    ZoteroTaskCallback _callback;
    String _since_version;

    public ZoteroVerItemsTask (ZoteroTaskCallback callback, String since_version) {
        _callback = callback;
        _since_version = since_version;
    }

    @Override
    public void startZoteroTask() {
        execute(Constants.BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/items?since=" + _since_version + "&format=versions");
    }

    protected void onPostExecute(String rstring) {
        Vector<String> item_keys = new Vector<String>();
        String version = "0000";

        try {
            JSONObject jObject = new JSONObject(rstring);
            version = jObject.getString("Last-Modified-Version");
            JSONObject items = jObject.getJSONObject("results");

            Iterator i = items.keys();
            while (i.hasNext()) {
                String key = (String)i.next();
                item_keys.add(key);
            }
            _callback.onItemVersion(true, "New items to check", item_keys, version);
            return;

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"Error in parsing JSON Object.");
            _callback.onItemVersion(false,"Error in parsing JSON Object.", null, version);
            return;
        }
    }
}

