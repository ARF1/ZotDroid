package uk.co.section9.zotdroid.data;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import uk.co.section9.zotdroid.ZoteroBroker;
import uk.co.section9.zotdroid.data.ZoteroRecord;
import uk.co.section9.zotdroid.data.ZoteroAttachment;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by oni on 06/07/2017.
 * Basically sends all our commands and such to Zotero and returns the replies
 */

public class ZoteroNet {

    public static final String TAG = "zotdroid.ZoteroNet";
    public static final String BASE_URL = "https://api.zotero.org";

    private ZoteroItemsTask _current_task       = null;
    private boolean         _processing_task    = false;

    /**
     * Generic task that executes in the background, making requests of Zotero
     * and returning string data.
     */
    private class ZoteroTask extends AsyncTask<String,Integer,String> {

        protected String doInBackground(String... address) {
            // [0] is address
            // after that, each pair is a set of headers we want to send

            String result = "";
            URL url = null;
            try {
                url = new URL(address[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return result;
            }

            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Zotero-API-Key", ZoteroBroker.TOKEN_SECRET);

                for (int i = 1; i < address.length; i+=2){
                    urlConnection.setRequestProperty(address[i], address[i+1]);
                }

                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line).append('\n');
                    }
                    result = total.toString();

                    String headers = "";
                    // Here, we check for any of the special Zotero headers we might need
                    if (urlConnection.getHeaderField("Total-Results") != null){
                        headers += "Total-Results : " + urlConnection.getHeaderField("Total-Results") + ", ";
                    }

                    // TODO - pagination might be a bit tricky.
                    result = "{ " + headers + " results : " + result + "}";

                } catch (IOException e) {
                    InputStream in = new BufferedInputStream(urlConnection.getErrorStream());
                    e.printStackTrace();
                    result = "FAIL";
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                InputStream in = new BufferedInputStream(urlConnection.getErrorStream());
                // TODO - do something with the error streams at some point
                e.printStackTrace();
                result = "FAIL";
            }
            return result;
        }
    }

    /**
     * Special callback for the Items once all items have completed
     */
    public interface ZoteroTaskCallback {
        void onItemsCompletion(boolean success);
        void onItemCompletion(boolean success, float progress, String message, Vector<ZoteroRecord> records, Vector<ZoteroAttachment> attachments);
    }

    /**
     * Extend our items task so that we can do some processing on the data returned
     * and then call the callback.
     */
    private class ZoteroItemsTask extends ZoteroTask {

        ZoteroTaskCallback callback;
        int startItem = 0;
        int itemLimit = 25; // Seems to be what Zotero likes by default (despite the API)

        ZoteroItemsTask(ZoteroTaskCallback callback, int start, int limit) {
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
        protected void execute(){
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
                callback.onItemsCompletion(false);
                stop();
                return;
            }

            boolean success = true;

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
                        success = false;
                        // TODO - callbackComplete here?
                    }
                }

                callback.onItemCompletion(success, ((float)startItem / (float)total) * 100.0f, "", records, attachments);

                if (!_processing_task){
                    stop();
                    return; // Don't fire any callbacks
                }

                // We fire off another task from here if success and we have more to go
                if (success && startItem + 1 < total){
                    new ZoteroItemsTask(callback, startItem + itemLimit ,itemLimit).execute();
                } else {
                    callback.onItemsCompletion(success);
                    stop();
                }

            } catch (JSONException e) {
                e.printStackTrace();
                Log.d(TAG,"Error in parsing JSON Object.");
                success = false;
                callback.onItemsCompletion(false);
                stop();
            }
        }
    }

    /**
     * Cancel whatever the current operation is
     */
    public void stop() {
        if (_current_task != null) {
            _processing_task = false;
            _current_task = null;
        }
    }



    /**
     * Fire off the getItems part.
     * This function makes multiple requests and therefore, we need two levels of callbacks
     * Informing the user at each step.
     * @param callback
     */

    public boolean getItems(ZoteroTaskCallback callback) {

        if (_current_task == null) {
            _processing_task = true;
            _current_task = new ZoteroItemsTask(callback, 0, 25);
            _current_task.execute();
            return true;
        }
        return false;
    }

}
