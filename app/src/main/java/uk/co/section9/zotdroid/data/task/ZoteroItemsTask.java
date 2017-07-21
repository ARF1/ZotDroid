package uk.co.section9.zotdroid.data.task;

/**
 * Created by oni on 21/07/2017.
 */

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

import uk.co.section9.zotdroid.ZoteroBroker;
import uk.co.section9.zotdroid.data.ZoteroAttachment;
import uk.co.section9.zotdroid.data.ZoteroRecord;

/**
 * Extend our items task so that we can do some processing on the data returned
 * and then call the callback.
 */
public class ZoteroItemsTask extends ZoteroTask {

    private static final String TAG = "ZoteroItemsTask";

    ZoteroTaskCallback callback;
    int startItem = 0;
    int itemLimit = 25; // Seems to be what Zotero likes by default (despite the API)

    public ZoteroItemsTask(ZoteroTaskCallback callback, int start, int limit) {
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
    public void startZoteroTask(){
        super.execute(BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/items?start=" + Integer.toString(this.startItem),
                "start", Integer.toString(this.startItem),
                "limit", Integer.toString(this.itemLimit ),
                "direction", "desc",
                "sort", "dateAdded");
    }


    protected ZoteroRecord processEntry(JSONObject jobj) {
        ZoteroRecord record = new ZoteroRecord();

        try {
            record.set_zotero_key(jobj.getString("key"));
        } catch (JSONException e){
            // We should always have a key. If we dont then bad things :S
        }

        try {
            record.set_title(jobj.getString("title"));
        } catch (JSONException e) {
            record.set_title("No title");
        }

        try {
            JSONObject creator = jobj.getJSONArray("creators").getJSONObject(0);
            record.set_author(creator.getString("lastName") + ", " + creator.getString("firstName"));
        } catch (JSONException e){
            record.set_author("No author(s)");
        }

        try {
            record.set_parent(jobj.getString("parent"));
        } catch (JSONException e){
            record.set_parent("");
        }

        try {
            record.set_item_type(jobj.getString("itemType"));
        } catch (JSONException e){
            record.set_parent("");
        }
        return record;
    }

    protected ZoteroAttachment processAttachment(JSONObject jobj) {
        ZoteroAttachment attachment = new ZoteroAttachment();

        try {
            attachment.set_zotero_key(jobj.getString("key"));
        } catch (JSONException e){
            // We should always have a key. If we dont then bad things :S
        }

        try {
            attachment.set_file_name(jobj.getString("filename"));
        } catch (JSONException e){
        }

        // TODO - some attachments are top level - do we show these?
        try {
            attachment.set_parent(jobj.getString("parentItem"));
            Log.i(TAG, "n: " + attachment.get_parent());
        } catch (JSONException e){

        }

        try {
            attachment.set_file_type(jobj.getString("contentType"));
        } catch (JSONException e){
        }

        return attachment;
    }

    protected void onPostExecute(String rstring) {
        Vector<ZoteroRecord> records =  new Vector<ZoteroRecord>();
        Vector<ZoteroAttachment> attachments =  new Vector<ZoteroAttachment>();

        // TODO - not so happy with the stop()s everywhere - state is annoying. Need a better
        // interrupt. Has already caused one issue :/

        // Check we didn't get a failure on that rsync call
        if (rstring == "FAIL"){
            callback.onItemsCompletion(this, false, rstring);
            return;
        }

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
                    if (jobj.getString("itemType").contains("attachment")){
                        attachments.add(processAttachment(jobj));
                    } else {
                        records.add(processEntry(jobj));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.onItemCompletion(this, false, "", 0, total, null, null);
                    return;
                }
            }

            callback.onItemCompletion(this, true, "", startItem + jArray.length(), total, records, attachments);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"Error in parsing JSON Object.");
            callback.onItemsCompletion(this, false,"Erro in parsing JSON Object.");
        }
    }
}
