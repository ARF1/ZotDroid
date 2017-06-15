package uk.co.section9.zotdroid;

/**
 * Created by oni on 24/03/2017.
 *
 *
 */

import oauth.signpost.*;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.http.HttpParameters;
import android.net.Uri;
import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ZoteroBroker {

    public static final String TAG = "zotdroid.ZoteroBroker";

    /** Application key -- available from Zotero */
    public static final String CONSUMER_KEY = "50e538ada5d8c4f40e01";
    public static final String CONSUMER_SECRET = "ef03b60b207aef632c24";

    public static String ACCESS_TOKEN = "stuffandting";
    public static String TOKEN_SECRET = "stuffandting";

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

    /**
     * A small class that returns a full result from any requests
     */
    public class AuthResult {
        public String log;
        public boolean result;
        public String authUrl;
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

    /**
     *
     * @return a AuthResult stating whether or not we succeeded
     */
    public AuthResult getAuthURL ()  {

        AuthResult res = new AuthResult();
        try {

            // https://stackoverflow.com/questions/21183407/linkedin-oauth-signpost-exception-oauthcommunicationexception-communication-w#21673071
            if (android.os.Build.VERSION.SDK_INT > 9) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); StrictMode.setThreadPolicy(policy);
            }

            CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
            OAuthProvider provider = new DefaultOAuthProvider(OAUTH_REQUEST, OAUTH_ACCESS, OAUTH_AUTHORIZE);

            Log.d(TAG,"Fetching request token from Zotero...");

            res.authUrl = provider.retrieveRequestToken(consumer, CALLBACK_URL);

            Log.d(TAG,"Request token: " + consumer.getToken());
            Log.d(TAG,"Token secret: " + consumer.getTokenSecret());

            ACCESS_TOKEN = consumer.getToken();
            TOKEN_SECRET = consumer.getTokenSecret();

            consumer.setTokenWithSecret(ACCESS_TOKEN, TOKEN_SECRET);

            res.log = "Success";

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


}
