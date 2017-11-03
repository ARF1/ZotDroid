package uk.co.section9.zotdroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.SearchView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
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
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

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
    private Dialog                  _download_dialog; // TODO - Do we need both?
    private ZotDroidOps             _zotdroid_ops;
    private ZoteroCollection        _filter = null; // Current category we are in.
    private ExpandableListAdapter   _main_list_adapter;
    private ExpandableListView      _main_list_view;

    // Our main list memory locations
    ArrayList< String >                    _main_list_items = new ArrayList< String >  ();
    ArrayList< String >                    _main_list_collections = new ArrayList< String >  ();
    HashMap< String, ArrayList<String> >   _main_list_sub_items =  new HashMap< String, ArrayList<String> >();

    // Our current mapping, given search and similar. List ID to ZoteroRecord basically
    HashMap < Integer, ZoteroRecord >       _main_list_map = new HashMap<Integer, ZoteroRecord>();
    HashMap < Integer, ZoteroCollection >   _collection_list_map = new HashMap<Integer, ZoteroCollection>();

    /**
     * A small class that listens for Intents. Mostly used to change font size on the fly.
     */

    public class PreferenceChangeBroadcastReceiver extends BroadcastReceiver {
        public PreferenceChangeBroadcastReceiver () {}
        private static final String TAG = "PreferenceChangeBroadcastReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == "FONT_SIZE_PREFERENCE_CHANGED"){ changeFontSize();}
        }
    }


    PreferenceChangeBroadcastReceiver _broadcast_receiver = new PreferenceChangeBroadcastReceiver();

    /**
     * onCreate as standard. Attempts to auth and if we arent authed, launches the login screen.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"Creating ZotDroid...");
        setContentView(R.layout.activity_main);

        // Setup the toolbar with the extra search
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        LayoutInflater inflater = LayoutInflater.from(this);
        View sl = inflater.inflate(R.layout.search, null);
        toolbar.addView(sl);
        SearchView sv = (SearchView) findViewById(R.id.recordsearch);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterList(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // I put a check in here to reset if everything is blank
                if (newText.isEmpty()){
                    filterList();
                }
                return false;
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        toggle.syncState();
        // Setup the main list of items
        _main_list_view = (ExpandableListView) findViewById(R.id.listViewMain);
        // Pass this activity - ZoteroBroker will look for credentials
        ZoteroBroker.passCreds(this,this);
        _zotdroid_ops = new ZotDroidOps(this, this);
        filterList();
        setDrawer();
        // Set the font preference stuff
        IntentFilter filter = new IntentFilter("FONT_SIZE_PREFERENCE_CHANGED");
        this.registerReceiver(_broadcast_receiver,filter);
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

        ProgressBar pb = (ProgressBar) dialog.findViewById(R.id.progressBarDownload);
        pb.setVisibility(View.VISIBLE);

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

    private void changeFontSize() {
        TextView groupTitle = (TextView) _main_list_view.findViewById(R.id.main_list_group);
        TextView groupSubText = (TextView) _main_list_view.findViewById(R.id.main_list_subtext);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String font_size = settings.getString("settings_font_size","medium");

        if (groupTitle != null ) {
            if (font_size.contains("small")) {
                groupTitle.setTextAppearance(this, R.style.MainList_Title_Small);
            } else if (font_size.contains("medium")) {
                groupTitle.setTextAppearance(this, R.style.MainList_Title_Medium);
            } else if (font_size.contains("large")) {
                groupTitle.setTextAppearance(this, R.style.MainList_Title_Large);
            } else {
                groupTitle.setTextAppearance(this, R.style.MainList_Title_Medium);
            }
        }
        if (groupSubText != null ) {
            if (font_size.contains("small")){ groupSubText.setTextAppearance(this, R.style.MainList_SubText_Small);}
            else if (font_size.contains("medium")){ groupSubText.setTextAppearance(this, R.style.MainList_SubText_Medium);}
            else if (font_size.contains("large")) { groupSubText.setTextAppearance(this, R.style.MainList_SubText_Large);}
            else { groupSubText.setTextAppearance(this, R.style.MainList_SubText_Medium);}
        }

        // This is expensive but I think it's what we have to do really.
        filterList();
        setDrawer();
    }

    /**
     * Launch a loading dialog for showing progress and the like
     * @return
     */

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
                _zotdroid_ops.stop();
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
        _zotdroid_ops.resetAndSync();
    }

    /**
     * Do a standard, partial sync if we can, else resetAndSync
     */
    protected void sync() {
        if (!_zotdroid_ops.sync()) {
            resetAndSync();
            return;
        }
        _loading_dialog = launchLoadingDialog();
    }

    protected void startTestWebDav() {
        _loading_dialog = launchLoadingDialog();
        String status_message = "Testing Webdav Connection.";
        TextView messageView = (TextView) _loading_dialog.findViewById(R.id.textViewLoading);
        messageView.setText(status_message);
        _zotdroid_ops.testWebDav();
    }


    public void onSyncProgress(float progress) {
        String status_message = "Loading. " + Float.toString( Math.round(progress * 100.0f)) + "% complete.";
        TextView messageView = (TextView) _loading_dialog.findViewById(R.id.textViewLoading);
        messageView.setText(status_message);
        Log.i(TAG,status_message);
    }

    public void onSyncFinish(boolean success, String message) {
        filterList();
        setDrawer();
        _loading_dialog.dismiss();
        Log.i(TAG,"Sync Version: " + _zotdroid_ops.getVersion());
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

  /*  @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }*/

    /*@Override
    public void onResume(){
        super.onResume();

        // If we are now authed, lets sync
        //if (ZoteroBroker.isAuthed()){
        //    sync();
        //}
    }*/

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
                //this.startActivityForResult(new Intent(this, SettingsActivity.class), 1);
                Intent si = new Intent(this,SettingsActivity.class);
                startActivity(si);
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
                startTestWebDav();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Filter the current list just by the collection, using internal _filter class param
     */
    public void filterList() {
        filterList("");
    }

    /**\
     * Check a record to see if anything matches the search term. We ignore case.
     * @param record
     * @param searchterm
     */
    private boolean searchRecord(ZoteroRecord record, String searchterm) {
        if (searchterm == ""){ return true; }
        if (record.get_author().toLowerCase().contains(searchterm.toLowerCase())) { return true;}
        else if (record.get_title().toLowerCase().contains(searchterm.toLowerCase())){ return true; }
        return false;
    }

    /**
     * Update our list from what is held by ZotDroidOps by choosing a collection
     * and a potential search term
     * We use the internal class variable _filter which is set by clicking the drawer
     * @param searchterm - a term for searching from the search bar
     */
    private void filterList( String searchterm) {
        _main_list_items.clear();
        _main_list_map.clear();
        _zotdroid_ops.update(); // Make sure we are up to date

        // Possibly a better way to pass font size but for now
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String font_size = "medium";
        font_size = settings.getString("settings_font_size",font_size);

        _main_list_adapter = new ZotDroidListAdapter(this,_main_list_items, _main_list_sub_items,font_size);
        _main_list_view.setAdapter(_main_list_adapter);

        Vector<ZoteroRecord> records = _zotdroid_ops.get_records();

        for (ZoteroRecord record : records) {
            if (_filter == null || record.inCollection(_filter)) {
                if (searchRecord(record, searchterm)) {
                    String tt = record.get_title() + " - " + record.get_author();
                    _main_list_map.put(new Integer(_main_list_items.size()), record);
                    _main_list_items.add(tt);
                    ArrayList<String> tl = new ArrayList<String>();
                    for (ZoteroAttachment attachment : record.get_attachments()) {
                        tl.add(attachment.get_file_name());
                    }
                    _main_list_sub_items.put(tt, tl);
                }
            }
        }

        _main_list_view.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                ZoteroRecord record = null;
                record = _main_list_map.get(new Integer((groupPosition)));

                if (record != null) {
                    _download_dialog = launchDownloadDialog();
                    _zotdroid_ops.startAttachmentDownload(record, childPosition);
                }
                return true;
            }
        });

    }

    private void recSetDrawer(ZoteroCollection c, int level) {
        _collection_list_map.put(_main_list_collections.size(),c);
        String indent = "";
        for (int i = 0; i < level; i++) { indent += "..."; }
        _main_list_collections.add(indent + c.get_title());
        for (ZoteroCollection cc : c.get_sub_collections()) {
            recSetDrawer(cc,level + 1);
        }
    }



    /**
     * A subroutine to set the left-hand collections drawer
     */
    public void setDrawer() {
        // Now create our lefthand drawer from the collections
        ListView drawer_list = (ListView) findViewById(R.id.left_drawer);
        _main_list_collections.clear();
        _collection_list_map.clear();
        _collection_list_map.put(new Integer(0),null);
        _main_list_collections.add("All");

        // Firstly, get the top level collections
        Vector<ZoteroCollection> toplevels = new Vector<ZoteroCollection>();
        for (ZoteroCollection c : _zotdroid_ops._collections) {
            if (!c.has_parent()){ toplevels.add(c); }
        }

        for (ZoteroCollection c: toplevels){
            recSetDrawer(c,0);
        }

        // Override the adapter so we can set the fontsize
        drawer_list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, _main_list_collections) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                TextView tv = (TextView) row;
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.getContext());
                String font_size = settings.getString("settings_font_size","medium");
                // Set fonts here too!
                if (font_size.contains("small")) {
                    tv.setTextAppearance(this.getContext(), R.style.SideList_Small);
                } else if (font_size.contains("medium")) {
                    tv.setTextAppearance(this.getContext(), R.style.SideList_Medium);
                } else if (font_size.contains("large")) {
                    tv.setTextAppearance(this.getContext(), R.style.SideList_Large);
                } else {
                    tv.setTextAppearance(this.getContext(), R.style.SideList_Medium);
                }

                return row;
            }
        });


        // On-click show only these items in a particular collection and set the title to reflect this.
        drawer_list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
            {
                _filter = _collection_list_map.get(position);
                filterList();
                Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
                if (_filter != null) {
                    Toast.makeText(getApplicationContext(), "Selecting: " + _filter.get_title(), Toast.LENGTH_SHORT).show();
                    toolbar.setTitle("ZotDroid: " + _filter.get_title());
                } else {
                    Toast.makeText(getApplicationContext(), "Selecting: ALL", Toast.LENGTH_SHORT).show();
                    toolbar.setTitle("ZotDroid:");
                }
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
     * Called when a webdav test process finishes.
     * @param success
     * @param message
     */
    @Override
    public void onWebDavTestFinish(boolean success, String message) {
        String status_message = "Connection Failed: " + message;
        if (success) {
            status_message = "Connection succeded";
        }
        TextView messageView = (TextView) _loading_dialog.findViewById(R.id.textViewLoading);
        messageView.setText(status_message);
        Button button = (Button) _loading_dialog.findViewById(R.id.buttonCancel);
        button.setText("Dismiss");

        ProgressBar pb = (ProgressBar) _loading_dialog.findViewById(R.id.progressBarDownload);
        pb.setVisibility(View.INVISIBLE);
    }

    /**
     * Called when we are checking authorisation of our tokens
     * @param result
     */
    @Override
    public void onAuthCompletion(boolean result) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(_broadcast_receiver);
    }
}
