package uk.co.section9.zotdroid.webdav;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import javax.net.ssl.HttpsURLConnection;

import uk.co.section9.zotdroid.auth.ZoteroBroker;

/**
 * Created by oni on 13/07/2017.
 */

public class ZoteroDownload {

    public static final String TAG = "zotdroid.ZoteroDownload";
    public static final String API_BASE = "https://api.zotero.org"; // TODO - duplicate to remove

    private AsyncTask<String,Integer,String> _request;
    private WebDavTest _test;

    /**
     * A class that is used to test the WebDav connection
     */
    private class WebDavTest extends AsyncTask<String,Integer,String> {

        ZoteroWebDavCallback callback;

        public WebDavTest(ZoteroWebDavCallback callback){
            this.callback = callback;
        }

        protected String doInBackground(String... address) {
            String result = "SUCCESS";
            URL url = null;

            try {
                url = new URL(address[0]);
            } catch (MalformedURLException e) {
                result = "Malformed address. Check your WebDav Server setting.";
                e.printStackTrace();
                return result;
            }

            final String username = address[1];
            final String password = address[2];
            HttpsURLConnection urlConnection = null;

            try {
                String basic_auth = getB64Auth(username,password);

                if (url.getProtocol().contentEquals("https") ) {
                    urlConnection = (HttpsURLConnection) url.openConnection();
                    urlConnection.setRequestProperty("Authorization", basic_auth);
                    urlConnection.connect();

                    try {
                        String line = urlConnection.getContent().toString();
                    } catch (IOException e) {
                        InputStream in = new BufferedInputStream(urlConnection.getErrorStream());
                        e.printStackTrace();
                        result = e.getMessage();
                    } finally {
                        urlConnection.disconnect();
                    }
                } else {
                    result = "Connection must be HTTPS";
                }
            } catch (IOException e) {
                result = e.getMessage();
            }

            return result;
        }

        protected void onPostExecute(String rstring) {
            Log.i(TAG, rstring);
            if (rstring != "SUCCESS"){
                callback.onWebDavComplete(false, rstring);
                return;
            }
            callback.onWebDavComplete(true, "success");
        }
    }

    /**
     * Given two strings, return a proper basic auth string
     * @param login
     * @param pass
     * @return
     */
    private String getB64Auth (String login, String pass) {
        String source=login+":"+pass;
        String ret="Basic "+ Base64.encodeToString(source.getBytes(),Base64.URL_SAFE|Base64.NO_WRAP);
        return ret;
    }

    /**
     * The async derived class that actually performs the real work.
     */
    private class WebDavRequest extends AsyncTask<String,Integer,String> {

        ZoteroWebDavCallback callback;
        public WebDavRequest(ZoteroWebDavCallback callback){
            this.callback = callback;
        }

        protected String doInBackground(String... address) {
            String result = "SUCCESS";
            URL url = null;

            // Credentials are address[1] / username and address[2] / password
            // filename is address[3]
            String username = address[1];
            String password = address[2];
            String filename = address[3];
            String file_path = address[4];
            String final_filename = address[5];

            try {
                url = new URL(address[0] + "/" + filename);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                result = "Malformed URL." + e.getMessage();
                return result;
            }

            Log.i(TAG, filename + ", " + url.toString());
            String basic_auth = getB64Auth(username, password);

            File file = new File(file_path, final_filename);
            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Authorization", basic_auth);

                try {
                    // https://stackoverflow.com/questions/7887078/android-saving-file-to-external-storage#7887114
                    if (file.exists()) file.delete();
                    file.getParentFile().mkdirs();

                    // Now do the reading but save to a file
                    byte[] bytes = new byte[1024]; // read in 1024 chunks

                    InputStream is = urlConnection.getInputStream();
                    BufferedInputStream buf = new BufferedInputStream(is);

                    FileOutputStream out = new FileOutputStream(file);
                    ZipInputStream zin = new ZipInputStream (buf);
                    zin.getNextEntry();

                    int total_bytes_read = 0;
                    int total = urlConnection.getContentLength();
                    Log.i(TAG,"Bytes Available: " + Integer.toString(total));

                    int bytes_read = 0;
                    while ((bytes_read = zin.read(bytes, 0, bytes.length)) > 0) {
                        out.write(bytes, 0, bytes_read);
                        total_bytes_read += bytes_read;
                        // For some reason, this seems to go over 100% - I guess because there are headers
                        // or some other data not included in the getContentLength field, so we cap it.
                        int progress = (int) Math.min(Math.round( (float) total_bytes_read / (float) total * 100.0),100.0);
                        publishProgress(progress);
                    }

                    out.flush();
                    out.close();
                    zin.close();
                    buf.close();

                    // return the full path so we can open it
                    result = file.getAbsolutePath();
                    callback.onWebDavComplete(true, result);

                } catch (IOException e) {
                    result = cleanup(e, file);
                }catch (Exception e) {
                    result = cleanup(e, file);
                } finally {
                    urlConnection.disconnect();
                }
            } catch ( FileNotFoundException e){
                result = cleanup(e, file);
            } catch (IOException e) {
                result = cleanup(e, file);
            }

            return result;
        }

        private String cleanup(Exception e, File file){
            //InputStream in = new BufferedInputStream(urlConnection.getErrorStream());
            // TODO - do something with the error streams at some point
            e.printStackTrace();
            if (file.exists()) { file.delete(); }

            File fileDir = file.getParentFile();
            if (fileDir.exists() && fileDir.listFiles().length == 0) {
                // Directory with Zotero key is empty and no longer needed
                fileDir.delete();
            }
            return "WebDav download error. " + e.getMessage();
        }

        /**
         * Called as the task progresses
         * @param progress
         */

        protected void onProgressUpdate(Integer... progress) {
            Log.i(TAG,"Progress: " + Integer.toString(progress[0]));
            callback.onWebDavProgess(true, Integer.toString(progress[0]));
        }

        /**
         * Called once a task has completed
         * @param rstring
         */
        protected void onPostExecute(String rstring) {
            Log.i(TAG, "Post Execute: " + rstring);
            if (rstring != "SUCCESS"){
                callback.onWebDavComplete(false, rstring);
                return;
            }
            callback.onWebDavComplete(true, rstring);
        }
    }

    /**
     * The async derived class that downloads from Zotero's servers
     * This usually results in a redirect via a 304 to some final link
     * We also don't end up with zipped file for some reason.
     */
    private class ZoteroRequest extends AsyncTask<String,Integer,String> {
        ZoteroWebDavCallback callback;
        public ZoteroRequest(ZoteroWebDavCallback callback){
            this.callback = callback;
        }

        protected String doInBackground(String... address) {
            String result = "SUCCESS";
            URL url = null;
            String file_path = address[1];
            String final_filename = address[2];

            try {
                url = new URL(address[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                result = "Malformed URL.";
                return result;
            }

            File file = new File(file_path, final_filename);
            HttpsURLConnection urlConnection = null;
            try {

                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setInstanceFollowRedirects(true);
                urlConnection.setRequestProperty("Zotero-API-Key", ZoteroBroker.TOKEN_SECRET);

                try {
                    // https://stackoverflow.com/questions/7887078/android-saving-file-to-external-storage#7887114
                    if (file.exists()) file.delete();
                    file.getParentFile().mkdirs();

                    // Now do the reading but save to a file
                    byte[] bytes = new byte[1024]; // read in 1024 chunks
                    InputStream is = urlConnection.getInputStream();
                    BufferedInputStream buf = new BufferedInputStream(is);
                    Map<String, List<String>> headers = urlConnection.getHeaderFields();
                    String tt = headers.get("Content-Length").get(0);
                    Log.i(TAG,"Bytes Available: " + tt);
                    int total = Integer.decode(tt);
                    FileOutputStream out = new FileOutputStream(file);
                    int total_bytes_read = 0;
                    int bytes_read = 0;

                    while ((bytes_read = buf.read(bytes, 0, bytes.length)) > 0) {
                        out.write(bytes, 0, bytes_read);
                        total_bytes_read += bytes_read;
                        // For some reason, this seems to go over 100% - I guess because there are headers
                        // or some other data not included in the getContentLength field, so we cap it.
                        int progress = (int) Math.min(Math.round( (float) total_bytes_read / (float) total * 100.0),100.0);
                        publishProgress(progress);
                    }

                    out.flush();
                    out.close();
                    buf.close();

                    // return the full path so we can open it
                    result = file.getAbsolutePath();
                    callback.onWebDavComplete(true, result);

                } catch (IOException e) {
                    result = cleanup(e, file);
                } catch (Exception e) {
                    result = cleanup(e, file);
                } finally {
                    urlConnection.disconnect();
                }
            } catch ( FileNotFoundException e){
                result = cleanup(e, file);
            } catch (IOException e) {
                result = cleanup(e, file);
            }

            return result;
        }

        private String cleanup(Exception e, File file){
            //InputStream in = new BufferedInputStream(urlConnection.getErrorStream());
            // TODO - do something with the error streams at some point
            e.printStackTrace();
            if (file.exists()) { file.delete(); }

            File fileDir = file.getParentFile();
            if (fileDir.exists() && fileDir.listFiles().length == 0) {
                // Directory with Zotero key is empty and no longer needed
                fileDir.delete();
            }
            return "Attachment download error. " + e.getMessage();
        }

        /**
         * Called as the task progresses
         * @param progress
         */

        protected void onProgressUpdate(Integer... progress) {
            Log.i(TAG,"Progress: " + Integer.toString(progress[0]));
            callback.onWebDavProgess(true, Integer.toString(progress[0]));
        }

        /**
         * Called once a task has completed
         * @param rstring
         */
        protected void onPostExecute(String rstring) {
            Log.i(TAG, "Post Execute: " + rstring);
            if (rstring != "SUCCESS"){
                callback.onWebDavComplete(false, rstring);
                return;
            }
            callback.onWebDavComplete(true, rstring);
        }
    }

    /**
     * Stop any current download request.
     */
    public void stop() {
        if (_request != null) { _request.cancel(true); }
        if (_test != null) { _test.cancel(true);}
    }

    /**
     * Test the webdav connection to see if it works at all
     * @param activity
     * @param callback
     */
    public void testWebDav(Activity activity, ZoteroWebDavCallback callback){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        String username = settings.getString("settings_webdav_username","username");
        String password = settings.getString("settings_webdav_password","password");
        String server_address = settings.getString("settings_webdav_address","address");
        _test = new WebDavTest(callback);
        _test.execute(server_address, username, password);
    }


    /**
     * Actually download a file using the personal webdav and save it to the SDCard if possible.
     * @param filename
     * @param file_path
     * @param final_filename
     * @param username
     * @param password
     * @param server_address
     * @param callback
     */
    public void downloadAttachment(String filename, String file_path, String final_filename, String username, String password, String server_address, ZoteroWebDavCallback callback){
        // Get the credentials we need for this
        _request = new WebDavRequest(callback);
        _request.execute(server_address, username, password, filename, file_path, final_filename);
    }

    /**
     * Download the file using the Zotero provided storage
     * @param file_path
     * @param final_filename
     * @param itemkey
     * @param callback
     */
    public void downloadAttachmentZotero(String file_path, String final_filename, String itemkey, ZoteroWebDavCallback callback){
        // Get the credentials we need for this
        _request = new ZoteroRequest(callback);
        String server_address = API_BASE + "/users/" + ZoteroBroker.USER_ID + "/items/" + itemkey + "/file";
        _request.execute(server_address, file_path, final_filename);
    }

}


