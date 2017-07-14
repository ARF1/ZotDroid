package uk.co.section9.zotdroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Vector;

import uk.co.section9.zotdroid.data.RecordsTable;
import uk.co.section9.zotdroid.data.ZotDroidDB;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ZoteroOps.ZoteroTaskCallback,
        ZoteroBroker.ZoteroAuthCallback, ZoteroWebDav.ZoteroWebDavCallback {

    public static final String      TAG = "zotdroid.MainActivity";
    private static int              ZOTERO_LOGIN_REQUEST = 1667;
    private ZoteroOps               _zotero_ops = new ZoteroOps();
    private ZoteroWebDav            _zotero_webdav = new ZoteroWebDav();
    private Dialog                  _loading_dialog;
    private Dialog                  _webdav_dialog;
    private ZotDroidDB              _zotdroid_db;

    private ArrayAdapter<String>    _main_list_adapter;
    ArrayList<String>               _main_list_items = new ArrayList<String>();

    private int                     _sync_progress;
    /**
     * onCreate as standard. Attempts to auth and if we arent authed, launches the login screen.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"Creating...");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Fire up the database
        _zotdroid_db =  new ZotDroidDB(this);

        // Setup the main list of items
        _main_list_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, _main_list_items);
        ListView myListView = (ListView) findViewById(R.id.listViewMain);
        myListView.setAdapter(_main_list_adapter);

        // Pass this activity - ZoteroBroker will look for credentials
        ZoteroBroker.passCreds(this,this);

        // Now check to see if we need to launch the login process
        // TODO - this is a network op so needs to be in an AsyncTask we need to implement
        /*if (!ZoteroBroker.isAuthed()){
            Log.i(TAG,"Not authed. Performing OAUTH.");
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.setAction("zotdroid.LoginActivity.LOGIN");
            this.startActivityForResult(loginIntent,ZOTERO_LOGIN_REQUEST);
        }*/
        _sync_progress = 0;

        // Attempt to load from the DB
        populateFromDB();

    }

    /**
     * A handy function that loads our dialog to show we are loading.
     * TODO - needs messages to show what we are doing.
     * https://stackoverflow.com/questions/37038835/how-do-i-create-a-popup-overlay-view-in-an-activity-without-fragment
     * @return
     */
    private Dialog launchLoadingDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.fragment_loading);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int dialogWidth = (int)(displayMetrics.widthPixels * 0.85);
        int dialogHeight = (int)(displayMetrics.heightPixels * 0.85);
        dialog.getWindow().setLayout(dialogWidth, dialogHeight);

        Button cancelButton = (Button) dialog.findViewById(R.id.buttonCancel);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _zotero_ops.stop();
                dialog.dismiss();
            }
        });

        dialog.show();
        return dialog;
    }


    /**
     * Start sync with the Zotero server
     */
    protected void sync() {
        _loading_dialog = launchLoadingDialog();
        _sync_progress = 0;
        _main_list_items.clear();
        _main_list_adapter.notifyDataSetChanged();
        _zotdroid_db.reset(); // For now, just nuke the database
        _zotero_ops.getItems(this);
    }

    /**
     * Start sync with the Zotero server
     */
    protected void testWebdav() {
        _zotero_webdav.testWebDav(this, this);
    }

    /**
       LoginActivity returns with some data for us, but we write it to the
        shared preferences here.
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG,"Returned from Zotero Login.");
        if (requestCode == ZOTERO_LOGIN_REQUEST) {
            if (resultCode == Activity.RESULT_OK ) {
                ZoteroBroker.setCreds(this);
            }
            finishActivity(ZOTERO_LOGIN_REQUEST);
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onResume(){
        super.onResume();

        // If we are now authed, lets sync
        //if (ZoteroBroker.isAuthed()){
        //    sync();
        //}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                this.startActivityForResult(new Intent(this, SettingsActivity.class),1);
                return true;
            case R.id.action_sync:
                if (ZoteroBroker.isAuthed()) {
                    sync();
                } else {
                    Log.i(TAG,"Not authed. Performing OAUTH.");
                    Intent loginIntent = new Intent(this, LoginActivity.class);
                    loginIntent.setAction("zotdroid.LoginActivity.LOGIN");
                    this.startActivityForResult(loginIntent,ZOTERO_LOGIN_REQUEST);
                }
                return true;

            case R.id.action_test_webdav:
                testWebdav();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Called when the sync task completes and we have a stack of results to process.
     * Clears the list and adds what we get from the server
     * @param success
     */
    @Override
    public void onItemCompletion(boolean success, Vector<RecordsTable.ZoteroRecord> results) {

        String status_message = "";
        if (success) {
            for (RecordsTable.ZoteroRecord record : results){
                _main_list_items.add(record.toString());
                _zotdroid_db._records_table.writeRecord(record);
            }

            _sync_progress += results.size();
            _main_list_adapter.notifyDataSetChanged();

            status_message = "Loaded " + Integer.toString(_sync_progress) + " records.";
            TextView messageView = (TextView) _loading_dialog.findViewById(R.id.textViewLoading);
            messageView.setText(status_message);
            Log.i(TAG,status_message);

        } else {
            _loading_dialog.dismiss();
            Log.d(TAG,"Error returned in onItemCompletion");
        }
    }

    /**
     * Read from the DB on load
     */
    public void populateFromDB() {
        Vector<RecordsTable.ZoteroRecord> records = _zotdroid_db._records_table.get_records();
        for (RecordsTable.ZoteroRecord record : records){
            _main_list_items.add(record.toString());
        }
        _main_list_adapter.notifyDataSetChanged();
    }

    /**
     * Called when the sync task completes and we have a stack of results to process.
     * Clears the list and adds what we get from the server
     * @param success
     */
    @Override
    public void onItemsCompletion(boolean success) {
        _loading_dialog.dismiss();
    }

    /**
     * Called when we are checking authorisation of our tokens
     * @param result
     */
    @Override
    public void onAuthCompletion(boolean result) {

    }

    /**
     * Webdav testing callback
     * @param result
     * @param message
     */

    @Override
    public void onWebDavTestComplete(boolean result, String message) {

    }

    /**
     * Webdav download completed for some reason
     * @param result
     * @param message
     * @param filename
     */

    @Override
    public void onWebDavDownloadComplete(boolean result, String message, String filename) {

    }
}
