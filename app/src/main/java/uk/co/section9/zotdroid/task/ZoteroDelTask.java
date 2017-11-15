package uk.co.section9.zotdroid.task;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

import uk.co.section9.zotdroid.auth.ZoteroBroker;

/**
 * Created by oni on 27/07/2017.
 */

public class ZoteroDelTask extends ZoteroTask {

    private static final String TAG = "ZoteroVerItemsTask";

    ZoteroTaskCallback _callback;
    String _since_version;


    public ZoteroDelTask(ZoteroTaskCallback callback, String since_version) {
        _callback = callback;
        _since_version = since_version;
    }

    @Override
    public void startZoteroTask() {
       execute(BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/deleted?since=" + _since_version + "&format=versions");
    }

    protected void onPostExecute(String rstring) {

        Vector<String> item_keys = new Vector<String>();
        Vector<String> collection_keys = new Vector<String>();

        try {
            JSONObject jObject = new JSONObject(rstring);

            String version = "0000";
            try {
                version = jObject.getString("Last-Modified-Version");
            } catch (JSONException e) {
                Log.i(TAG,"No Last-Modified-Version in request.");
            }

            jObject = jObject.getJSONObject("results");
            JSONArray items = jObject.getJSONArray("items");

            for (int i=0; i < items.length(); i++) {
                try {
                    String tj = items.getString(i);
                    item_keys.add(tj);
                } catch (JSONException e) {
                }
            }

            JSONArray collections = jObject.getJSONArray("collections");

            for (int i=0; i < collections.length(); i++) {
                try {
                    String tj = collections.getString(i);
                    collection_keys.add(tj);
                } catch (JSONException e) {
                }
            }

            _callback.onSyncDelete(true, "New items to delete", item_keys, collection_keys, version );

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"Error in parsing JSON Object.");
            _callback.onSyncDelete(false,"Error in parsing JSON Object.", null, null, "0000");
            return;
        }

    }
}

