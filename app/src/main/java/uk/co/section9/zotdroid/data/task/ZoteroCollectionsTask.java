package uk.co.section9.zotdroid.data.task;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

import uk.co.section9.zotdroid.ZoteroBroker;
import uk.co.section9.zotdroid.data.ZoteroCollection;

/**
 * Created by oni on 21/07/2017.
 */

public class ZoteroCollectionsTask extends ZoteroTask {

    private static final String TAG = "ZoteroCollectionsTask";

    private ZoteroTaskCallback callback;
    private int startItem = 0;
    private int itemLimit = 25;

    private String _url = "";
    private boolean _reset_mode = true;

    public ZoteroCollectionsTask(ZoteroTaskCallback callback, int start, int limit) {
        this.callback = callback;
        this.startItem = start;
        this.itemLimit = limit;
        _reset_mode = true;
        _url = BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/collections?start=" + Integer.toString(this.startItem);
    }

    public ZoteroCollectionsTask(ZoteroTaskCallback callback, Vector<String> keys) {
        this.callback = callback;
        _reset_mode = false;
        _url = BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/collections?collectionKey=";

        for (String key: keys){
            _url += key + ",";
        }
        _url = _url.substring(0, _url.length()-1);
    }

    /**
     * Actually execute the async task.
     * Not quite an override but it sets up the string for us when we do actually execute, so things are in sync
     * For some reason, it only works if I pass the start (and possibly limit) as URL params instead of headers
     * but the desc and dateAdded seem ok. It could be an integer thing I suspect
     * If _reset_mode is not true then we are returning objects that need to be updated, not fresh objects
     */
    public void startZoteroTask() {

        if (_reset_mode) {
            super.execute(_url,
                    "start", Integer.toString(this.startItem),
                    "limit", Integer.toString(this.itemLimit),
                    "direction", "desc",
                    "sort", "dateAdded");
        } else {
            super.execute(_url,
                    "direction", "desc",
                    "sort", "dateAdded");
        }
    }

    protected void onPostExecute(String rstring) {
        Vector<ZoteroCollection> collections =  new Vector<ZoteroCollection>();

        // Check we didn't get a failure on that rsync call
        if (rstring == "FAIL"){
            callback.onCollectionsCompletion(this, false, rstring, "0000");
            return;
        }

        // TODO - Not sure if we have to request multiple times for collections :/

        try {
            JSONObject jObject = new JSONObject(rstring);

            int total = 0;
            try {
                total = jObject.getInt("Total-Results");
            } catch (JSONException e) {
                Log.i(TAG,"No Total-Results in request.");
            }

            String version = "0000";
            try {
                version = jObject.getString("Last-Modified-Version");
            } catch (JSONException e) {
                Log.i(TAG,"No Last-Modified-Version in request.");
            }

            JSONArray jArray = jObject.getJSONArray("results");

            for (int i=0; i < jArray.length(); i++) {
                try {
                    JSONObject jobjtop = jArray.getJSONObject(i);
                    JSONObject jobj = jobjtop.getJSONObject("data");
                    collections.add(processEntry(jobj));
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.onCollectionsCompletion(this, false, "", version);
                    return;
                }
            }

            if (_reset_mode) {
                callback.onCollectionCompletion(this, true, "", startItem + jArray.length(), total, collections, version);
            } else {
                callback.onCollectionCompletion(this, true, "", collections, version);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"Error in parsing JSON Object.");
            callback.onCollectionsCompletion(this, false,"Error in parsing JSON Object.", "0000");
        }
    }

    protected ZoteroCollection processEntry(JSONObject jobj) {
        ZoteroCollection collection = new ZoteroCollection();

        try {
            collection.set_zotero_key(jobj.getString("key"));
        } catch (JSONException e) {
            // We should always have a key. If we dont then bad things :S
        }

        try {
            collection.set_title(jobj.getString("name"));
        } catch (JSONException e) {
            collection.set_title("No title");
        }

        try {
            collection.set_parent(jobj.getString("parentCollection"));
        } catch (JSONException e) {
            collection.set_parent("");
        }

        return collection;
    }
}
