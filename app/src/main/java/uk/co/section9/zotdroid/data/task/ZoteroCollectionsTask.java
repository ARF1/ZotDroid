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

    public ZoteroCollectionsTask(ZoteroTaskCallback callback, int start, int limit) {
        this.callback = callback;
        this.startItem = start;
        this.itemLimit = limit;
    }

    /**
     * Actually execute the async task.
     * Not quite an override but it sets up the string for us when we do actually execute, so things are in sync
     * For some reason, it only works if I pass the start (and possibly limit) as URL params instead of headers
     * but the desc and dateAdded seem ok. It could be an integer thing I suspect
     */
    public void startZoteroTask() {
        super.execute(BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/collections?start=" + Integer.toString(this.startItem),
                "start", Integer.toString(this.startItem),
                "limit", Integer.toString(this.itemLimit),
                "direction", "desc",
                "sort", "dateAdded");
    }

    protected void onPostExecute(String rstring) {
        Vector<ZoteroCollection> collections =  new Vector<ZoteroCollection>();

        // Check we didn't get a failure on that rsync call
        if (rstring == "FAIL"){
            callback.onCollectionsCompletion(this, false, rstring, null);
            return;
        }

        // TODO - Not sure if we have to request multiple times for collections :/

        try {
            JSONObject jObject = new JSONObject(rstring);

            int total = 0;
            try {
                total = jObject.getInt("Total-Results");
            } catch (JSONException e) {
                total = 0;
                Log.i(TAG,"No Total-Results in request.");
            }

            JSONArray jArray = jObject.getJSONArray("results");

            for (int i=0; i < jArray.length(); i++) {
                try {
                    JSONObject jobjtop = jArray.getJSONObject(i);
                    JSONObject jobj = jobjtop.getJSONObject("data");
                    collections.add(processEntry(jobj));
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.onCollectionsCompletion(this, false, "", null);
                    return;
                }
            }

            callback.onCollectionsCompletion(this, true, "", collections);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"Error in parsing JSON Object.");
            callback.onCollectionsCompletion(this, false,"Error in parsing JSON Object.", null);

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
