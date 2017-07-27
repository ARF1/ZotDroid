package uk.co.section9.zotdroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SubMenu;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import uk.co.section9.zotdroid.data.CollectionsItemsTable;
import uk.co.section9.zotdroid.data.ZoteroAttachment;
import uk.co.section9.zotdroid.data.ZoteroCollection;
import uk.co.section9.zotdroid.data.ZoteroRecord;

/**
 * TODO - if we cancel a sync, we need to not replace anything!
 */

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ZoteroBroker.ZoteroAuthCallback,
ZotDroidOps.ZotDroidCaller {

    public static final String      TAG = "zotdroid.MainActivity";
    private static int              ZOTERO_LOGIN_REQUEST = 1667;

    private Dialog                  _loading_dialog;
    private Dialog                  _download_dialog;
    private Dialog                  _webdav_dialog;

    private ZotDroidOps             _zotdroid_ops;

    private ExpandableListAdapter                   _main_list_adapter;
    private ExpandableListView                      _main_list_view;

    ArrayList< String >                    _main_list_items = new ArrayList< String >  ();
    ArrayList< String >                    _main_list_collections = new ArrayList< String >  ();
    HashMap< String, ArrayList<String> >   _main_list_sub_items =  new HashMap< String, ArrayList<String> >();
    /**
     * onCreate as standard. Attempts to auth and if we arent authed, launches the login screen.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"Creating...");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

      /*  FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        })*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        toggle.syncState();

        //NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        //navigationView.setNavigationItemSelectedListener(this);

        // Setup the main list of items
        _main_list_view = (ExpandableListView) findViewById(R.id.listViewMain);


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

        _zotdroid_ops = new ZotDroidOps(this, this);
        updateList(null);

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
                _zotdroid_ops.stop();
                dialog.dismiss();
            }
        });

        dialog.show();
        return dialog;
    }


    private Dialog launchDownloadDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.fragment_downloading);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int dialogWidth = (int)(displayMetrics.widthPixels * 0.85);
        int dialogHeight = (int)(displayMetrics.heightPixels * 0.85);
        dialog.getWindow().setLayout(dialogWidth, dialogHeight);

        Button cancelButton = (Button) dialog.findViewById(R.id.buttonCancelDownload);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
            }
        });

        dialog.show();
        return dialog;
    }


    /**
     * Reset everything and do a full sync from scratch
     */
    protected void resetAndSync() {
        _loading_dialog = launchLoadingDialog();
        _main_list_items.clear();
        _main_list_collections.clear();
        _zotdroid_ops.resetAndSync();

    }

    /**
     * Do a standard, partial sync
     */
    protected void sync() {
        _loading_dialog = launchLoadingDialog();
        _main_list_items.clear();
        _main_list_collections.clear();
        _zotdroid_ops.sync();

    }

    public void onSyncProgress(float progress) {
        String status_message = "Loading. " + Float.toString( Math.round(progress * 100.0f)) + "% complete.";
        TextView messageView = (TextView) _loading_dialog.findViewById(R.id.textViewLoading);
        messageView.setText(status_message);
        Log.i(TAG,status_message);
    }

    public void onSyncFinish(boolean success, String message) {
        updateList(null);
        _loading_dialog.dismiss();

        Log.i(TAG,"Items Version: " + _zotdroid_ops.getItemsVersion());
        Log.i(TAG,"Collections Version: " + _zotdroid_ops.getCollectionsVersion());
    }

    /**
       LoginActivity returns with some data for us, but we write it to the
        shared preferences here.
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG,"Returned from Zotero Login hopefully.");
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
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        return super.onCreatePanelMenu(featureId, menu);

        // This is where we create our menu from collections
/*        getMenuInflater().inflate(R.menu.activity_main_drawer, menu);

        menu.addSubMenu(Menu.NONE, R.id.collections_menu , Menu.NONE,"Menu1");

        SubMenu themeMenu = menu.findItem(R.id.collections_menu).getSubMenu();

        themeMenu.clear();
        themeMenu.add(0, 1, Menu.NONE, "Automatic");
        themeMenu.add(0, 2, Menu.NONE, "Default");
        themeMenu.add(0, 3, Menu.NONE, "Night");
        themeMenu.add(0, 4, Menu.NONE, "Battery Saving");

        return true;*/
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
            case R.id.action_reset_sync:
                if (ZoteroBroker.isAuthed()) {
                    resetAndSync();
                } else {
                    Log.i(TAG,"Not authed. Performing OAUTH.");
                    Intent loginIntent = new Intent(this, LoginActivity.class);
                    loginIntent.setAction("zotdroid.LoginActivity.LOGIN");
                    this.startActivityForResult(loginIntent,ZOTERO_LOGIN_REQUEST);
                }
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
                //testWebdav();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
/*
        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Refine the list when we click on a collection
     * @param position
     */

    public void chooseCollection(int position) {

        // First is always *All* so subtract one
        if (position > 0 ){
            ZoteroCollection c = _zotdroid_ops.get_collection(position-1);
            if (c != null) {
                updateList(c);
            } else {
                updateList(null);
            }
        } else {
            // The *ALL*
            updateList(null);
        }
    }

    /**
     * Update our list from what is held by ZotDroidOps
     * @param filter - null or a zotero key
     */
    public void updateList(ZoteroCollection filter) {
        _main_list_items.clear();
        _main_list_collections.clear();
        // Make sure we are up to date
        _zotdroid_ops.update();

        Vector<ZoteroRecord> records = _zotdroid_ops.get_records();

        for (ZoteroRecord record : records) {

            if (filter == null || record.inCollection(filter)) {
                _main_list_items.add(record.get_title());
                ArrayList<String> tl = new ArrayList<String>();
                for (ZoteroAttachment attachment : record.get_attachments()) {
                    tl.add(attachment.get_file_name());
                }
                _main_list_sub_items.put(record.get_title(), tl);
            }
        }

        _main_list_adapter = new ZotDroidListAdapter(this,_main_list_items, _main_list_sub_items);
        _main_list_view.setAdapter(_main_list_adapter);

        _main_list_view.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

                //Toast.makeText(getApplicationContext(), "child clicked", Toast.LENGTH_SHORT).show();
                ZoteroRecord record = _zotdroid_ops.get_record(groupPosition);
                if (record != null){
                    _download_dialog = launchDownloadDialog();
                    _zotdroid_ops.startAttachmentDownload(record, childPosition);
                }

                return true;
            }
        });

        // Now create our lefthand drawer from the collections
        ListView drawer_list = (ListView) findViewById(R.id.left_drawer);

        _main_list_collections.add("All");

        for (ZoteroCollection c : _zotdroid_ops._collections) {
            _main_list_collections.add(c.get_title());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, _main_list_collections);
        drawer_list.setAdapter(adapter);

        drawer_list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
            {
                Log.i(TAG,_main_list_collections.get(position).toString());
                chooseCollection(position);
            }
        });
    }

    public void onDownloadProgress(float progress) {
        String status_message = "Progess: " + Float.toString(progress) + "%";
        TextView messageView = (TextView) _download_dialog.findViewById(R.id.textViewDownloading);
        messageView.setText(status_message);
        Log.i(TAG, status_message);
    }

    public void onDownloadFinish(boolean success, String message, String filetype) {

        if (!success) {
            String status_message = "Error: " + message;
            TextView messageView = (TextView) _download_dialog.findViewById(R.id.textViewDownloading);
            messageView.setText(status_message);
            Log.i(TAG, status_message);
        } else {

            Intent intent = new Intent();

            File ff7 =  new File(message);

            if (ff7.exists()){
                intent.setAction(android.content.Intent.ACTION_VIEW);
                Log.i(TAG, "Attempting to open " + message);
                try {
                    intent.setDataAndType(Uri.fromFile(ff7), filetype);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    _download_dialog.dismiss();
                    startActivity(intent);
                    _download_dialog.dismiss();
                } catch (Exception e){
                    Log.d(TAG,"Error opening file");
                    e.printStackTrace();
                }
            } else {
                String status_message = "Error: " + message  + " does not appear to exist.";
                TextView messageView = (TextView) _download_dialog.findViewById(R.id.textViewDownloading);
                messageView.setText(status_message);
                Log.i(TAG, status_message);
            }
        }
    }


    /**
     * Called when we are checking authorisation of our tokens
     * @param result
     */
    @Override
    public void onAuthCompletion(boolean result) {

    }


}
