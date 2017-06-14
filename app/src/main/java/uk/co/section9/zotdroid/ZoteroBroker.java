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

public class ZoteroBroker {

    /** Application key -- available from Zotero */
    public static final String CONSUMER_KEY = "93a5aac13612aed2a236";
    public static final String CONSUMER_SECRET = "196d86bd1298cb78511c";

    public static final String ACCESS_TOKEN = "196d86bd1298cb78511c";
    public static final String TOKEN_SECRET = "196d86bd1298cb78511c";

    /** This is the zotero:// protocol we intercept
     * It probably shouldn't be changed. */
    public static final String CALLBACK_URL = "zotero://";

    /** This is the Zotero API server. Those who set up independent
     * Zotero installations will need to change this. */
    public static final String APIBASE = "https://api.zotero.org";

    /** These are the API GET-only methods */
    public static final String ITEMFIELDS = "/itemFields";
    public static final String ITEMTYPES = "/itemTypes";
    public static final String ITEMTYPECREATORTYPES = "/itemTypeCreatorTypes";
    public static final String CREATORFIELDS = "/creatorFields";
    public static final String ITEMNEW = "/items/new";

    /* These are the manipulation methods */
    // /users/1/items GET, POST, PUT, DELETE
    public static final String ITEMS = "/users/USERID/items";
    public static final String COLLECTIONS = "/users/USERID/collections";

    public static final String TAGS = "/tags";
    public static final String GROUPS = "/groups";

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
     * @param authUrl - blank string to be filled with the auth URL we are following
     * @param log - blank string filled with any messages we need
     * @return a boolean stating whether or not we succeeded
     */
    public boolean getAuthURL (String authUrl, String log)  {

        try {
            CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);

            OAuthProvider provider = new DefaultOAuthProvider(OAUTH_REQUEST, OAUTH_ACCESS, OAUTH_AUTHORIZE);

            System.out.println("Fetching request token from Zotero...");

            authUrl = provider.retrieveRequestToken(consumer, CALLBACK_URL);
            System.out.println("Request token: " + consumer.getToken());
            System.out.println("Token secret: " + consumer.getTokenSecret());

            consumer.setTokenWithSecret(ACCESS_TOKEN, TOKEN_SECRET);

            //startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));

        } catch (OAuthMessageSignerException e) {
            log = e.getMessage();
            System.out.println(log);
            return false;
        } catch (OAuthNotAuthorizedException e) {
            log = e.getMessage();
            System.out.println(log);
            return false;
        } catch (OAuthExpectationFailedException e) {
            log = e.getMessage();
            System.out.println(log);
            return false;
        } catch (OAuthCommunicationException e) {
            log = e.getMessage();
            System.out.println(log);
            return false;
        }
        return true;
    }

}
