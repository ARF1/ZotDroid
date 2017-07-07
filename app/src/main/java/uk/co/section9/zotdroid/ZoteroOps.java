package uk.co.section9.zotdroid;

import android.os.AsyncTask;

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

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by oni on 06/07/2017.
 * Basically sends all our commands and such to Zotero and returns the replies
 */

public class ZoteroOps {

    public static final String TAG = "zotdroid.ZoteroOps";

    public static final String BaseURL = "https://api.zotero.org";

    // TODO - Make this a tree like structure and return these?
    public static class ZoteroRecord {
        String key;
        String parent;
        String title;
        String type;
        String author; // TODO - Just one for now but we will add more

        public String toString() {
            return title + " - " + author;
        }
    }

    /**
     * Generic task that executes in the background, making requests of Zotero
     * and returning string data.
     */
    private class ZoteroTask extends AsyncTask<String,Integer,String> {

        protected String doInBackground(String... address) {
            // Only one address please
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
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line).append('\n');
                    }
                    result = total.toString();
                } catch (IOException e) {
                    InputStream in = new BufferedInputStream(urlConnection.getErrorStream());
                    e.printStackTrace();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                InputStream in = new BufferedInputStream(urlConnection.getErrorStream());
                // TODO - do something with the error streams at some point
                e.printStackTrace();
            }
            return result;
        }
    }

    /**
     * Special callback for the Items
     */
    interface ZoteroItemsCallback {
        void onItemsCompletion(Vector<ZoteroRecord> results);
    }

    /**
     * Extend our items task so that we can do some processing on the data returned
     * and then call the callback.
     */
    private class ZoteroItemsTask extends ZoteroTask {

        ZoteroItemsCallback callbackFunc;

        ZoteroItemsTask(ZoteroItemsCallback callback) {
            this.callbackFunc = callback;
        }

        protected void onPostExecute(String rstring) {
            Vector<ZoteroRecord> results =  new Vector<ZoteroRecord>();
            // For some reason, proper objects are not returned so I create one for our JSON
            rstring = "{ data : " + rstring + "}";
            //Log.i(TAG,rstring);

            try {
                JSONObject jObject = new JSONObject(rstring);
                JSONArray jArray = jObject.getJSONArray("data");

                for (int i=0; i < jArray.length(); i++) {
                    try {
                        JSONObject oneObject = jArray.getJSONObject(i);
                        oneObject = oneObject.getJSONObject("data");

                        ZoteroRecord record = new ZoteroRecord();

                        record.key = oneObject.getString("key");
                        try {
                            record.title = oneObject.getString("title");
                        } catch (JSONException e) {
                            record.title = "No title";
                        }

                        try {
                            JSONObject creator = oneObject.getJSONArray("creators").getJSONObject(0);
                            record.author = creator.getString("lastName") + ", " + creator.getString("firstName");
                        } catch (JSONException e){
                            record.author = "No author(s)";
                        }
                        try {
                            record.parent = oneObject.getString("parent");
                        } catch (JSONException e){
                            record.parent = "";
                        }

                        record.type = oneObject.getString("itemType");

                        results.add(record);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            callbackFunc.onItemsCompletion(results);
        }
    }


    private static String makeConnection (String address) {
        String result = "";
        URL url = null;
        try {
            url = new URL(address);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return result;
        }

        HttpsURLConnection urlConnection = null;
        try {
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Zotero-API-Key", ZoteroBroker.TOKEN_SECRET);
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line).append('\n');
                }
                result = total.toString();
            } catch (IOException e) {
                InputStream in = new BufferedInputStream(urlConnection.getErrorStream());
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }
        } catch (IOException e) {
            InputStream in = new BufferedInputStream(urlConnection.getErrorStream());
            // TODO - do something with the error streams at some point
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Fire off the getItems part.
     * @param callback
     */

    public void getItems(ZoteroItemsCallback callback) {
        new ZoteroItemsTask(callback).execute(BaseURL + "/users/" + ZoteroBroker.USER_ID + "/items");
    }

}
