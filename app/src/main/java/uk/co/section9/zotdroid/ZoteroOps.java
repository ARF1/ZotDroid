package uk.co.section9.zotdroid;

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
import uk.co.section9.zotdroid.data.RecordsTable.ZoteroRecord;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by oni on 06/07/2017.
 * Basically sends all our commands and such to Zotero and returns the replies
 */

public class ZoteroOps {

    public static final String TAG = "zotdroid.ZoteroOps";
    public static final String BaseURL = "https://api.zotero.org";

    private ZoteroItemsTask currentTask = null;
    private boolean processingTask = false;

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
                    result = "{ " + headers + " data : " + result + "}";

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
    interface ZoteroTaskCallback {
        void onItemsCompletion(boolean success);
        void onItemCompletion(boolean success, Vector<ZoteroRecord> results);
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
            super.execute(BaseURL + "/users/" + ZoteroBroker.USER_ID + "/items?start=" + Integer.toString(this.startItem),
                    "start", Integer.toString(this.startItem),
                    "limit", Integer.toString(this.itemLimit ),
                    "direction", "desc",
                    "sort", "dateAdded");
        }

        protected void onPostExecute(String rstring) {
            Vector<ZoteroRecord> results =  new Vector<ZoteroRecord>();
            // For some reason, proper objects are not returned so I create one for our JSON

            // Check we didn't get a failure on that rsync call
            if (rstring == "FAIL"){
                callback.onItemsCompletion(false);
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

                JSONArray jArray = jObject.getJSONArray("data");

                for (int i=0; i < jArray.length(); i++) {
                    try {
                        JSONObject oneObject = jArray.getJSONObject(i);
                        oneObject = oneObject.getJSONObject("data");

                        ZoteroRecord record = new ZoteroRecord();

                        record.setKey(oneObject.getString("key"));

                        try {
                            record.setTitle(oneObject.getString("title"));
                        } catch (JSONException e) {
                            record.setTitle("No title");
                        }

                        try {
                            JSONObject creator = oneObject.getJSONArray("creators").getJSONObject(0);
                            record.setAuthor(creator.getString("lastName") + ", " + creator.getString("firstName"));
                        } catch (JSONException e){
                            record.setAuthor("No author(s)");
                        }

                        try {
                            record.setParent(oneObject.getString("parent"));
                        } catch (JSONException e){
                            record.setParent("");
                        }

                        record.setItemType(oneObject.getString("itemType"));

                        results.add(record);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        success = false;
                        // TODO - callbackComplete here?
                    }
                }

                callback.onItemCompletion(success, results);

                if (!processingTask){
                    return; // Don't fire any callbacks
                }

                // We fire off another task from here if success and we have more to go
                if (success && startItem + 1 < total){
                    new ZoteroItemsTask(callback, startItem + itemLimit ,itemLimit).execute();
                } else {
                    callback.onItemsCompletion(success);
                }

            } catch (JSONException e) {
                e.printStackTrace();
                Log.d(TAG,"Error in parsing JSON Object.");
                success = false;
                callback.onItemsCompletion(false);
            }
        }
    }

    /**
     * Cancel whatever the current operation is
     */
    public void stop() {
        if (currentTask != null) {
            processingTask = false;
            currentTask = null;
        }
    }



    /**
     * Fire off the getItems part.
     * This function makes multiple requests and therefore, we need two levels of callbacks
     * Informing the user at each step.
     * @param callback
     */

    public boolean getItems(ZoteroTaskCallback callback) {

        if (currentTask == null) {
            processingTask = true;
            currentTask = new ZoteroItemsTask(callback, 0, 25);
            currentTask.execute();
            return true;
        }
        return false;
    }

}
