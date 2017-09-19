package uk.co.section9.zotdroid;

/**
 * Created by oni on 24/03/2017.
 *
 *
 */

import oauth.signpost.*;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp3.CommonsHttp3OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.http.HttpParameters;
import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ZoteroBroker {

    public static final String TAG = "zotdroid.ZoteroBroker";

    /** Application key -- available from Zotero */
    public static String CONSUMER_KEY = "50e538ada5d8c4f40e01";
    public static String CONSUMER_SECRET = "ef03b60b207aef632c24";

    public static String ACCESS_TOKEN = "stuffandting";
    public static String TOKEN_SECRET = "stuffandting";

    public static String USER_ID = "toast";

    /** This is the zotero:// protocol we intercept
     * It probably shouldn't be changed. */
    public static final String CALLBACK_URL = "zotero://";

    /** This is the Zotero API server. Those who set up independent
     * Zotero installations will need to change this. */
    public static final String API_BASE = "https://api.zotero.org";

    /** These are the API GET-only methods */
    public static final String ITEM_FIELDS = "/itemFields";
    public static final String ITEM_TYPES = "/itemTypes";
    public static final String ITEM_TYPE_CREATOR_TYPES = "/itemTypeCreatorTypes";
    public static final String CREATOR_FIELDS = "/creatorFields";
    public static final String ITEM_NEW = "/items/new";

    /* These are the manipulation methods */
    // /users/1/items GET, POST, PUT, DELETE
    public static final String ITEMS = "/users/USERID/items";
    public static final String COLLECTIONS = "/users/USERID/collections";

    public static final String TAGS = "/tags";
    public static final String GROUPS = "/groups";

    static CommonsHttp3OAuthConsumer Consumer;
    static OAuthProvider Provider;

    // TODO - replace this with actually testing the tokens and such (they may be outtdated or filled in wrong)
    private static boolean Authed = false;

    /**
     * A small class that returns a full result from any requests
     */
    public static class AuthResult {
        public String log;
        public boolean result;
        public String authUrl;
        public String userID;
        public String userKey;
        public String userSecret;
    }

    /** And these are the OAuth endpoints we talk to.
     *
     * We embed the requested permissions in the endpoint URLs; see
     * http://www.zotero.org/support/dev/server_api/oauth#requesting_specific_permissions
     * for more details.
     */
    public static final String OAUTH_REQUEST = "https://www.zotero.org/oauth/request?" +
            "library_access=1&" +
            "notes_access=1&" +
            "write_access=1&" +
            "all_groups=write";
    public static final String OAUTH_ACCESS = "https://www.zotero.org/oauth/access?" +
            "library_access=1&" +
            "notes_access=1&" +
            "write_access=1&" +
            "all_groups=write";
    public static final String OAUTH_AUTHORIZE = "https://www.zotero.org/oauth/authorize?" +
            "library_access=1&" +
            "notes_access=1&" +
            "write_access=1&" +
            "all_groups=write";

    public static boolean isAuthed() {
        return Authed;
    }

    public interface ZoteroAuthCallback {
        void onAuthCompletion(boolean result);
    }

    public static void passCreds(Activity activity, ZoteroAuthCallback callback){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        ACCESS_TOKEN = settings.getString("settings_user_key",ACCESS_TOKEN);
        TOKEN_SECRET = settings.getString("settings_user_secret",TOKEN_SECRET);
        USER_ID = settings.getString("settings_user_id",USER_ID);

        Thread thread = new Thread()  {
            public void run() {
                // Only one address please
                boolean result = false;
                URL url = null;
                try {
                    url = new URL("https://api.zotero.org/keys/" + TOKEN_SECRET);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                HttpsURLConnection urlConnection = null;
                try {
                    urlConnection = (HttpsURLConnection) url.openConnection();
                    try {
                        BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        StringBuilder total = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) {
                            total.append(line).append('\n');
                        }
                        Authed = true;
                    } catch (IOException e) {
                        Authed = false;
                    } finally {
                        urlConnection.disconnect();
                    }
                } catch (IOException e) {
                    Authed = false;
                }

            }
        };
        thread.start();
    }

    /**
     * Set the creds from the main activity shared preferences
     * @param activity
     */

    public static void setCreds(Activity activity){

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = settings.edit();
        // For Zotero, the key and secret are identical, it seems
        editor.putString("settings_user_key", ACCESS_TOKEN);
        editor.putString("settings_user_secret", TOKEN_SECRET);
        editor.putString("settings_user_id", USER_ID);
        editor.commit();
    }

    /**
     *
     * @return an AuthResult stating whether or not we succeeded
     */
    public static AuthResult getAuthURL ()  {

        AuthResult res = new AuthResult();
        try {

            // https://stackoverflow.com/questions/21183407/linkedin-oauth-signpost-exception-oauthcommunicationexception-communication-w#21673071
            if (android.os.Build.VERSION.SDK_INT > 9) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); StrictMode.setThreadPolicy(policy);
            }

            Consumer = new CommonsHttp3OAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
            Provider = new DefaultOAuthProvider(OAUTH_REQUEST, OAUTH_ACCESS, OAUTH_AUTHORIZE);

            Log.d(TAG,"Fetching request token from Zotero...");

            res.authUrl = Provider.retrieveRequestToken(Consumer, CALLBACK_URL);

            Log.d(TAG,"Request token: " + Consumer.getToken());
            Log.d(TAG,"Token secret: " + Consumer.getTokenSecret());

            ACCESS_TOKEN = Consumer.getToken();
            TOKEN_SECRET = Consumer.getTokenSecret();

            Consumer.setTokenWithSecret(ACCESS_TOKEN, TOKEN_SECRET);

            res.log = "";
            res.result = true;

        } catch (OAuthMessageSignerException e) {
            res.log = "Signer Exception:" + e.getMessage();
            Log.d(TAG, res.log);
            res.result = false;
        } catch (OAuthNotAuthorizedException e) {
            res.log = "Not Authorised Exception:" + e.getMessage();
            System.out.println(res.log);
            res.result = false;
        } catch (OAuthExpectationFailedException e) {
            res.log = "Expectation Exception:" + e.getMessage();
            Log.d(TAG, res.log);
            res.result = false;
        } catch (OAuthCommunicationException e) {
            res.log = "Communication Exception:" + e.getMessage();
            Log.d(TAG, res.log);
            res.result = false;
        }
        return res;
    }

    /**
     * Finish our OAuth bit and save the bits to our settings.
     * @param uri
     * @return an AuthResult stating whether or not we succeeded
     */

    public static AuthResult finishOAuth(Uri uri){
        AuthResult res = new AuthResult();
        if (uri != null) {
            final String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
            try {
                Provider.retrieveAccessToken( Consumer, verifier);
                HttpParameters params = Provider.getResponseParameters();
                res.userID = params.getFirst("userID");
                Log.d(TAG, "uid: " + res.userID);
                res.userKey = ACCESS_TOKEN = Consumer.getToken();
                Log.d(TAG, "ukey: " + res.userKey);
                res.userSecret = TOKEN_SECRET = Consumer.getTokenSecret();
                Log.d(TAG, "usecret: " + res.userSecret);
                res.result = Authed = true;
                USER_ID = res.userID;

            } catch (OAuthMessageSignerException e) {
                res.log = e.getMessage();
                res.result = Authed = false;
            } catch (OAuthNotAuthorizedException e) {
                res.log = e.getMessage();
                res.result = Authed = false;
            } catch (OAuthExpectationFailedException e) {
                res.log = e.getMessage();
                res.result = Authed = false;
            } catch (OAuthCommunicationException e) {
                res.log = "Error communicating with server. Check your time settings, network connectivity, and try again. OAuth error: " + e.getMessage();
                res.result = Authed = false;
            }
        }
        return res;
    }
}
