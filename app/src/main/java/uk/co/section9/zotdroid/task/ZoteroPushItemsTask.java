package uk.co.section9.zotdroid.task;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Vector;

import uk.co.section9.zotdroid.Constants;
import uk.co.section9.zotdroid.auth.ZoteroBroker;
import uk.co.section9.zotdroid.data.zotero.Record;

/**
 * Created by oni on 01/12/2017.
 */

// TODO - eventually we will do this in batches of 50
public class ZoteroPushItemsTask extends ZoteroPost  {

    private static final String TAG = "ZoteroPushItemsTask";

    ZoteroTaskCallback _callback;
    // TODO - are these actually separate?
    String _last_version_items;
    Vector<Record> _changed_records;
    String _url = "";

    public void startZoteroTask(){
        _url = Constants.BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/items";
        JSONArray jtop = new JSONArray();

        for (Record r : _changed_records) {
            jtop.put(r.to_json());
        }

        execute(_url, jtop.toString());
    }

    protected void onPostExecute(String rstring) {
        String version = "0000";
        // Check we didn't get a failure on that rsync call
        if (rstring == "FAIL") {
            _callback.onPushItemsCompletion(false, rstring, version);
            return;
        }

        JSONObject jObject = null;
        try {
            jObject = new JSONObject(rstring);
            version = jObject.getString("Last-Modified-Version");
            JSONObject results = jObject.getJSONObject("results");

            // We need to check the changed results to make sure we re-sync back
            JSONObject successes = results.getJSONObject("success");
            JSONObject failures = results.getJSONObject("failed");
            //JSONObject unchanged = results.getJSONObject("unchanged");

            if (successes.length() > 0) {
                for (int i = 0; i < successes.names().length(); i++) {
                    JSONArray names = successes.names();

                    for (int j= 0; j < names.length(); j++) {
                        String label = names.getString(j);
                        String key = successes.getString(label);
                        // Run through our records, set the synced flag
                        for (Record  r : _changed_records){
                            if (r.get_zotero_key().equals(key)){
                                r.set_synced(true);
                            }
                        }
                    }

                }
            }
            if (failures.length() > 0) {
                for (int i = 0; i < failures.names().length(); i++) {
                    JSONArray names = failures.names();

                    for (int j= 0; j < names.length(); j++) {
                        String label = names.getString(j);
                        String key = failures.getString(label);
                        Log.i(TAG, "NAME " + key);
                    }

                }
                _callback.onPushItemsCompletion(false, "Some items did not sync correctly.", version);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            _callback.onPushItemsCompletion(false, "Failure in pushing items to Zotero.", version);
        }

        _callback.onPushItemsCompletion(true, rstring, version);
    }

    public ZoteroPushItemsTask(ZoteroTaskCallback callback, Vector<Record> changed_records, String last_version_items) {
        _callback = callback;
        _changed_records = changed_records;
        _last_version_items = last_version_items;
    }

}
