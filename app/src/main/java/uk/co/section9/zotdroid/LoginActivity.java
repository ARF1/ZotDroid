package uk.co.section9.zotdroid;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.Toast;


import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;

/**
 * A screen that fires up the OAuth we need for Zotero
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    protected ZoteroBroker broker;

    private static final String TAG = "zotdroid.LoginActivity";
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zotero_login);

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

    }

    private void toastError(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG);
            }
        });
    }

    private void populateAutoComplete() {
        getLoaderManager().initLoader(0, null, this);
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        broker = new ZoteroBroker();
        ZoteroBroker.AuthResult res = broker.getAuthURL();

        if (res.result) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(res.authUrl)));
            System.out.println(res.authUrl);
        } else {
            System.out.println(res.authUrl + "," + res.log);
            toastError(res.log);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
/*
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
*/
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }



    @Override
    public void onStart() {
        super.onStart();


    }

    @Override
    public void onStop() {
        super.onStop();


    }

    /*private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }*/


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

        }

        @Override
        protected void onCancelled() {

        }
    }

    /**
     * Receives intents that the app knows how to interpret. These will probably
     * all be URIs with the protocol "zotero://".
     *
     * This is currently only used to receive OAuth responses, but it could be
     * used with things like zotero://select and zotero://attachment in the
     * future.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "Got new intent");

        if (intent == null) return;

        // Here's what we do if we get a share request from the browser
        String action = intent.getAction();
        if (action != null
                && action.equals("android.intent.action.SEND")
                && intent.getExtras() != null) {
            // Browser sends us no data, just extras
            Bundle extras = intent.getExtras();
            for (String s : extras.keySet()) {
                try {
                    Log.d("TAG","Got extra: "+s +" => "+extras.getString(s));
                } catch (ClassCastException e) {
                    Log.e(TAG, "Not a string, it seems", e);
                }
            }

            /*Bundle b = new Bundle();
            b.putString("url", extras.getString("android.intent.extra.TEXT"));
            b.putString("title", extras.getString("android.intent.extra.SUBJECT"));
            this.b=b;*/
            //showDialog(DIALOG_CHOOSE_COLLECTION);
            return;
        }

		/*
		 * It's possible we've lost these to garbage collection, so we
		 * reinstantiate them if they turn out to be null at this point.
		 */
        /*
        if (this.httpOAuthConsumer == null)
            this.httpOAuthConsumer = new CommonsHttpOAuthConsumer(
                    ServerCredentials.CONSUMERKEY,
                    ServerCredentials.CONSUMERSECRET);
        if (this.httpOAuthProvider == null)
            this.httpOAuthProvider = new DefaultOAuthProvider(
                    ServerCredentials.OAUTHREQUEST,
                    ServerCredentials.OAUTHACCESS,
                    ServerCredentials.OAUTHAUTHORIZE);

                    */

		/*
		 * Also double-check that intent isn't null, because something here
		 * caused a NullPointerException for a user.
		 */

        Uri uri;
        uri = intent.getData();

        if (uri != null) {
			/*
			 * TODO The logic should have cases for the various things coming in
			 * on this protocol.
			 */
            final String verifier = uri
                    .getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);

            /*
				    		 * Here, we're handling the callback from the completed OAuth.
				    		 * We don't need to do anything highly visible, although it
				    		 * would be nice to show a Toast or something.
				    		 */
            /*
            new Thread(new Runnable() {
                public void run() {
                    try {

                        httpOAuthProvider.retrieveAccessToken(
                                httpOAuthConsumer, verifier);
                        HttpParameters params = httpOAuthProvider
                                .getResponseParameters();
                        final String userID = params.getFirst("userID");
                        Log.d(TAG, "uid: " + userID);
                        final String userKey = httpOAuthConsumer.getToken();
                        Log.d(TAG, "ukey: " + userKey);
                        final String userSecret = httpOAuthConsumer.getTokenSecret();
                        Log.d(TAG, "usecret: " + userSecret);

                        runOnUiThread(new Runnable(){
                            public void run(){
					    			//
					    			// These settings live in the Zotero preferences tree.
					    			//
                                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                                SharedPreferences.Editor editor = settings.edit();
                                // For Zotero, the key and secret are identical, it seems
                                editor.putString("user_key", userKey);
                                editor.putString("user_secret", userSecret);
                                editor.putString("user_id", userID);

                                editor.commit();

                                setUpLoggedInUser();

                                doSync();

                            }
                        });
                    } catch (OAuthMessageSignerException e) {
                        toastError(e.getMessage());
                    } catch (OAuthNotAuthorizedException e) {
                        toastError(e.getMessage());
                    } catch (OAuthExpectationFailedException e) {
                        toastError(e.getMessage());
                    } catch (OAuthCommunicationException e) {
                        toastError("Error communicating with server. Check your time settings, network connectivity, and try again. OAuth error: " + e.getMessage());
                    }
                }
            }).start();*/
        }
    }

}

