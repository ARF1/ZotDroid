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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;

import uk.co.section9.zotdroid.data.RecordsTable;

/**
 * Created by oni on 13/07/2017.
 */

public class ZoteroWebDav {

    public static final String TAG = "zotdroid.ZoteroWebDav";


    /**
     * Small interface for the webdav callbacks
     */
    public interface ZoteroWebDavCallback {
        public void onWebDavTestComplete(boolean result, String message);
        public void onWebDavDownloadComplete(boolean result, String message, String filename);
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
                callback.onWebDavTestComplete(false, rstring);
                return;
            }
            callback.onWebDavTestComplete(true, "success");
        }
    }

    private String getB64Auth (String login, String pass) {
        String source=login+":"+pass;
        String ret="Basic "+ Base64.encodeToString(source.getBytes(),Base64.URL_SAFE|Base64.NO_WRAP);
        return ret;
    }

    private class WebDavRequest extends AsyncTask<String,Integer,String> {

        protected String doInBackground(String... address) {

            String result = "FAIL";
            URL url = null;
            try {
                url = new URL(address[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return result;
            }

            // Credentials are address[1] / username and address[2] / password
            // filename is address[3]

            String username = address[1];
            String password = address[2];
            String filename = address[3];

            String basic_auth = getB64Auth(username,password);

            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();

                try {

                    // https://stackoverflow.com/questions/7887078/android-saving-file-to-external-storage#7887114
                    // TODO - we should totally set the file download path in the settings
                    String root = Environment.getExternalStorageDirectory().toString();
                    File myDir = new File(root + "/attachments");
                    myDir.mkdirs();
                    File file = new File (myDir, filename);
                    if (file.exists ()) file.delete();

                    // Now do the reading but save to a file
                    byte[] bytes = new byte[1024]; // read in 1024 chunks
                    BufferedInputStream buf = new BufferedInputStream( urlConnection.getInputStream());
                    FileOutputStream out = new FileOutputStream(file);

                    int bytes_read = buf.read(bytes, 0, bytes.length);
                    while (bytes_read != -1){
                        out.write(bytes,0,bytes_read);
                        bytes_read = buf.read(bytes, 0, bytes.length);
                    }

                    out.flush();
                    out.close();
                    buf.close();
                    // return the full path so we can open it
                    result = root + "/attachments/" + filename;

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

    public void testWebDav(Activity activity, ZoteroWebDavCallback callback){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        String username = settings.getString("settings_webdav_username","username");
        String password = settings.getString("settings_webdav_password","password");
        String server_address = settings.getString("settings_webdav_address","address");

        new WebDavTest(callback).execute(server_address, username, password);
    }


    public void downloadAttachment(String filename, Activity activity, ZoteroWebDavCallback callback){
        // Get the credentials we need for this
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        String username = settings.getString("settings_webdav_username","username");
        String password = settings.getString("settings_webdav_password","password");
        String server_address = settings.getString("settings_webdav_address","address");

    }
}

