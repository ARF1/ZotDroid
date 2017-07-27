package uk.co.section9.zotdroid.data.task;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Vector;

import uk.co.section9.zotdroid.ZoteroBroker;

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
        super.execute(BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/deleted?since=" + _since_version + "&format=versions");
    }

    protected void onPostExecute(String rstring) {

        Vector<String> item_keys = new Vector<String>();
        Vector<String> collection_keys = new Vector<String>();

        try {
            JSONObject jObject = new JSONObject(rstring);
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
            _callback.onSyncDelete(this, true, "New items to delete", item_keys, collection_keys );

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"Error in parsing JSON Object.");
            _callback.onSyncDelete(this, false,"Erro in parsing JSON Object.", null, null);
            return;
        }

    }
}

