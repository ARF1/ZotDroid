package uk.co.section9.zotdroid.task;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import uk.co.section9.zotdroid.auth.ZoteroBroker;

/**
 * Created by oni on 01/12/2017.
 */


public abstract class ZoteroPost extends ZoteroTask {

    public abstract void startZoteroTask();

    protected String doInBackground(String... address) {
        // [0] is address
        // [1] is the post body in json
        // after that, each pair is a set of headers we want to send

        String result = "";
        URL url = null;
        try {
            url = new URL(address[0]);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return result;
        }

        String post_body = address[1];

        HttpsURLConnection urlConnection = null;
        try {
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "applicaiton/json; charset=utf-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Zotero-API-Key", ZoteroBroker.TOKEN_SECRET);

            for (int i = 2; i < address.length; i+=2){
                urlConnection.setRequestProperty(address[i], address[i+1]);
            }

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
            writer.write(post_body);
            writer.flush();
            writer.close();

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

                if (urlConnection.getHeaderField("Last-Modified-Version") != null){
                    headers += "Last-Modified-Version : " + urlConnection.getHeaderField("Last-Modified-Version") + ", ";
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

