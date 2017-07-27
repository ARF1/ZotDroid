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

public class ZoteroVerItemsTask extends ZoteroTask {

    private static final String TAG = "ZoteroVerItemsTask";

    ZoteroTaskCallback _callback;
    String _since_version;


    public ZoteroVerItemsTask (ZoteroTaskCallback callback, String since_version) {
        _callback = callback;
        _since_version = since_version;
    }

    @Override
    public void startZoteroTask() {
        super.execute(BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/items?since=" + _since_version + "&format=versions");
    }

    protected void onPostExecute(String rstring) {

        Vector<String> item_keys = new Vector<String>();

        try {
            JSONObject jObject = new JSONObject(rstring);
            JSONObject items = jObject.getJSONObject("results");

            Iterator i = items.keys();
            while (i.hasNext()) {
                String key = (String)i.next();
                item_keys.add(key);
            }
            _callback.onItemVersion(this, true, "New items to check", item_keys);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"Error in parsing JSON Object.");
            _callback.onItemVersion(this, false,"Erro in parsing JSON Object.", null);
            return;
        }

    }
}

