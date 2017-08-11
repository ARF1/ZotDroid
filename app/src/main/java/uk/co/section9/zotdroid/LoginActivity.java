package uk.co.section9.zotdroid;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;


/**
 * A screen that fires up the OAuth we need for Zotero
 */
public class LoginActivity extends AppCompatActivity  {

    private static final String TAG = "zotdroid.LoginActivity";

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

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        ZoteroBroker.AuthResult res = ZoteroBroker.getAuthURL();
        if (res.result) {
            Log.d(TAG, "Starting Browser based Auth.");
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(res.authUrl)));
        } else {
            System.out.println(res.authUrl + "," + res.log);
            Log.d(TAG, "Error: " + res.log);
            toastError(res.log);
        }
    }

    @Override
    public void onResume() {
        Log.i(TAG, "Resumed.");
        super.onResume();
        Intent intent = getIntent();

        // Just make sure we are returning from the browser
        // This is perhaps a little tricky and may not always work
        // Seems not to when we actually need to get authed :/
        String action = intent.getAction();

        Log.i(TAG,"Action: " + action);
        if (action != null && action.equals("android.intent.action.VIEW")) {
            ZoteroBroker.AuthResult res = finishLogin(intent);
            if (res.result) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("UserID", res.userID);
                resultIntent.putExtra("UserKey", res.userKey);
                resultIntent.putExtra("UserSecret", res.userSecret);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        } else if (action == "zotdroid.LoginActivity.LOGIN" && ZoteroBroker.isAuthed()) {
            // This is the original call resuming that we need to stop
            // For some reason we are ignoring this one :/
            Intent resultIntent = new Intent();
            resultIntent.putExtra("UserID", ZoteroBroker.USER_ID);
            resultIntent.putExtra("UserKey", ZoteroBroker.ACCESS_TOKEN);
            resultIntent.putExtra("UserSecret", ZoteroBroker.TOKEN_SECRET);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
    }

    private ZoteroBroker.AuthResult finishLogin(Intent intent){
        // Here's what we do if we get a share request from the browser
        Uri uri;
        uri = intent.getData();
        ZoteroBroker.AuthResult res = new ZoteroBroker.AuthResult();
        res.result = false;

        if (uri != null) {
            res = ZoteroBroker.finishOAuth(uri);
            // Return to previous activity - the main one I believe
            Log.i(TAG, "Returning from successful login.");
        } else {
            Log.e(TAG, "ERROR: No uri returned from login.");
        }

        return res;
    }

    /**
     * Taken from Zandy and adapted.
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
        finishLogin(intent);
    }
}

