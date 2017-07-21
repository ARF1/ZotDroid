package uk.co.section9.zotdroid;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipInputStream;
import javax.net.ssl.HttpsURLConnection;

import uk.co.section9.zotdroid.data.RecordsTable;

/**
 * Created by oni on 13/07/2017.
 */

public class ZoteroWebDav {

    public static final String TAG = "zotdroid.ZoteroWebDav";

    public interface ZoteroWebDavCallback {
        public void onWebDavProgess(boolean result, String message);
        public void onWebDavComplete(boolean result, String message);
    }

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
                e.printStackTrace();
                return result;
            }

            String username = address[1];
            String password = address[2];

            HttpsURLConnection urlConnection = null;
            try {

                String basic_auth = getB64Auth(username,password);
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Authorization", basic_auth);

                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line).append('\n');
                    }

                } catch (IOException e) {
                    InputStream in = new BufferedInputStream(urlConnection.getErrorStream());
                    e.printStackTrace();
                    result = e.getMessage();
                } finally {
                    urlConnection.disconnect();
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

            String result = "*FAIL*";
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
                return result;
            }

            Log.i(TAG, filename + ", " + url.toString());
            String basic_auth = getB64Auth(username, password);

            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Authorization", basic_auth);

                try {
                    // https://stackoverflow.com/questions/7887078/android-saving-file-to-external-storage#7887114
                    File file = new File(file_path + final_filename);
                    if (file.exists()) file.delete();

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
                        int progress = (int) Math.round( (float) total_bytes_read / (float) total * 100.0);
                        publishProgress(progress);
                    }

                    out.flush();
                    out.close();
                    zin.close();
                    buf.close();

                    // return the full path so we can open it
                    result = file_path + final_filename;
                    callback.onWebDavComplete(true, result);

                } catch (IOException e) {
                    InputStream in = new BufferedInputStream(urlConnection.getErrorStream());
                    e.printStackTrace();
                    result = "*FAIL*ioerror";
                } finally {
                    urlConnection.disconnect();
                }
            } catch ( FileNotFoundException e){
                e.printStackTrace();
                result = "*FAIL*File not found.";
            } catch (IOException e) {
                InputStream in = new BufferedInputStream(urlConnection.getErrorStream());
                // TODO - do something with the error streams at some point
                e.printStackTrace();
                result = "*FAIL*ioerror.";
            }

            return result;
        }

        protected void onProgressUpdate(Integer... progress) {
            Log.i(TAG,"Progress: " + Integer.toString(progress[0]));
            callback.onWebDavProgess(true, Integer.toString(progress[0]));
        }


        protected void onPostExecute(String rstring) {

            Log.i(TAG, "Post Execute: " + rstring);
            if (rstring.startsWith("*FAIL*")){
                callback.onWebDavComplete(false, rstring.replace("*FAIL*",""));
                return;
            }
            callback.onWebDavComplete(true, rstring);
        }
    }

    public void testWebDav(Activity activity, ZoteroWebDavCallback callback){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        String username = settings.getString("settings_webdav_username","username");
        String password = settings.getString("settings_webdav_password","password");
        String server_address = settings.getString("settings_webdav_address","address");

        new WebDavTest(callback).execute(server_address, username, password);
    }


    public void downloadAttachment(String filename, String file_path, String final_filename, Activity activity, ZoteroWebDavCallback callback){
        // Get the credentials we need for this
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        String username = settings.getString("settings_webdav_username","username");
        String password = settings.getString("settings_webdav_password","password");
        String server_address = settings.getString("settings_webdav_address","address");

        new WebDavRequest(callback).execute(server_address, username, password, filename, file_path, final_filename);

    }
}

