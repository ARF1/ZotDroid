package uk.co.section9.zotdroid.ops;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ExpandableListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import uk.co.section9.zotdroid.Constants;
import uk.co.section9.zotdroid.R;
import uk.co.section9.zotdroid.Util;
import uk.co.section9.zotdroid.ZotDroidListAdapter;
import uk.co.section9.zotdroid.ZotDroidMem;
import uk.co.section9.zotdroid.data.ZotDroidDB;
import uk.co.section9.zotdroid.data.zotero.Attachment;
import uk.co.section9.zotdroid.data.zotero.Collection;
import uk.co.section9.zotdroid.data.zotero.Record;
import uk.co.section9.zotdroid.task.ZotDroidWebDavCaller;
import uk.co.section9.zotdroid.webdav.ZoteroDownload;
import uk.co.section9.zotdroid.webdav.ZoteroWebDavCallback;

/**
 * Created by oni on 14/07/2017.
 *
 * This class performs user operations such as search, requesting a different collection,
 * or downloading an attachment.
 *
 */

public class ZotDroidUserOps extends ZotDroidOps implements ZoteroWebDavCallback {

    private ZoteroDownload          _zotero_download = new ZoteroDownload();
    private ZotDroidWebDavCaller    _midnightcaller; // Was listening to Chase and Status ;)
    public static final String      TAG = "zotdroid.ZotDroidUserOps";
    private Collection              _current_collection = null; // Which collection are we currently lookin at?
    private String                  _search_term = "";

    public ZotDroidUserOps(ZotDroidDB zotdroid_db, Activity activity, ZotDroidMem mem, ZotDroidWebDavCaller midnightcaller) {
        super(activity, zotdroid_db, mem);
        _midnightcaller = midnightcaller;
    }

    /**
     * Called as WebDav process runs
     * @param result
     * @param message
     */
    @Override
    public void onWebDavProgess(boolean result, String message) {
        // Ignored for now
    }

    /**
     * Stop the current task and clear all remaining tasks.
     * Also cancel any download operations
     */
    public void stop() { super.stop(); _zotero_download.stop(); }

    /**
     * Called when a webdav process completes
     * @param result
     * @param message
     */
    @Override
    public void onWebDavComplete(boolean result, String message) {
        _midnightcaller.onWebDavTestFinish(result, message);
    }

    public void testWebDav() { _zotero_download.testWebDav(_activity, this);}

    /**
     * A very small class that holds the state for our webdav attachment download
     * Its perhaps a bit complicated but it means we can do multiple requests and
     * not have the main activity worry too much.
     */
    private class OpsDav implements ZoteroWebDavCallback {

        Attachment _attachment;
        private OpsDav(Attachment attachment){
            _attachment = attachment;
        }

        @Override
        public void onWebDavProgess(boolean result, String message) {
            if (result) {
                _midnightcaller.onDownloadProgress(Float.parseFloat(message));
            }
        }

        @Override
        public void onWebDavComplete(boolean result, String message) {
            _midnightcaller.onDownloadFinish(result, message, _attachment.get_file_type());
        }
    }


    /**
     * Download an attachement, unless it already exists, in which case, callback immediately
     * @param record
     * @param attachment_idx
     */

    public void startAttachmentDownload(Record record, int attachment_idx){
        if (attachment_idx < record.get_attachments().size()) {
            Attachment za = record.get_attachments().elementAt(attachment_idx);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(_activity);

            // If the file already exists, then we dont need to download, just return
            File file = new File( Util.getDownloadDirectory(_activity) + za.get_file_name());

            if (file.exists()){
                OpsDav op = new OpsDav(za);
                op.onWebDavComplete(true, Util.getDownloadDirectory(_activity) + za.get_file_name());
            } else {
                Boolean usewebdav = settings.getBoolean("settings_use_webdav_storage",false);

                if (usewebdav) {
                    String username = settings.getString("settings_webdav_username", "username");
                    String password = settings.getString("settings_webdav_password", "password");
                    String server_address = settings.getString("settings_webdav_address", "address");
                    _zotero_download.downloadAttachment(za.get_zotero_key() + ".zip", Util.getDownloadDirectory(_activity),
                            za.get_file_name(), username, password, server_address, new OpsDav(za));
                } else {
                    _zotero_download.downloadAttachmentZotero( Util.getDownloadDirectory(_activity),
                            za.get_file_name(), za.get_zotero_key(),  new OpsDav(za));
                }
            }
        }
        // TODO return somekind of false here
    }

    public void reset() {
        filter(_current_collection,"",_zotdroid_mem._end_index);
    }


    protected void filter(Collection collection, String searchterm, int end) {
        // Swapping collections with no search or enhance
        if (collection != _current_collection) {
            // Swapping collections so nuke entirely
            _zotdroid_mem.nukeMemory();
        }

        _current_collection = collection;
        Vector<Record> trv;
        if (_current_collection != null) {
            trv = _zotdroid_db.getItemsForCollection(collection);
        } else {
            trv =_zotdroid_db.getRecords(end);
        }

        // Perform the search term if there is one
        if (searchterm != ""){
            Vector<Record> matching = new Vector<>();
            for (Record tr : trv) {
                if (tr.search(searchterm)){
                    matching.add(tr);
                }
            }
            trv = matching;
        }

        // Make sure the size is correct
        if (end < trv.size()) {
            _zotdroid_mem._end_index = end;
            trv.setSize(_zotdroid_mem._end_index + 1);
        } else {
            _zotdroid_mem._end_index = trv.size() - 1;
        }

        rebuildMemory(trv);
    }


    public void moreResults(int more) {
        filter(_current_collection, "", _zotdroid_mem._end_index + more);
    }

    public void swapCollection(Collection collection){
        if (collection != _current_collection){
            filter(collection, "", Constants.PAGINATION_SIZE);
        }
    }

    public void search(String term) {
        filter(_current_collection, term, _zotdroid_mem._end_index);
    }

    /**
     * Update our list from what is held by ZotDroidUserOps by choosing a collection
     * and a potential search term
     * We use the internal class variable _filter which is set by clicking the drawer
     * @param searchterm - a term for searching from the search bar
     */
    /*public void search(String searchterm, Collection collection) {

        //_zotdroid_ops.update(); // Make sure we are up to date


        Vector<Record> records = _zotdroid_ops.get_records();

        for (Record record : records) {
            if (_filter == null || record.inCollection(_filter)) {
                if (searchRecord(record, searchterm)) {
                    String tt = record.get_title() + " - " + record.get_author();
                    _main_list_map.put(new Integer(_main_list_items.size()), record);
                    _main_list_items.add(tt);

                    // We add metadata first, followed by attachments (TODO - Add a divider?)
                    ArrayList<String> tl = new ArrayList<String>();
                    tl.add("Title: " + record.get_title());
                    tl.add("Author(s): " + record.get_author());
                    tl.add("Date Added: " + record.get_date_added());
                    tl.add("Date Modified: " + record.get_date_modified());

                    for (Attachment attachment : record.get_attachments()) {
                        tl.add(attachment.get_file_name());
                    }
                    _main_list_sub_items.put(tt, tl);
                }
            }
        }

        _main_list_view.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Record record = null;
                if (id >= Constants.ATTACHMENT_START_INDEX) {
                    record = _main_list_map.get(new Integer((groupPosition)));
                    if (record != null) {
                        _download_dialog = launchDownloadDialog();
                        _zotdroid_ops.startAttachmentDownload(record, childPosition - Constants.ATTACHMENT_START_INDEX);
                    }
                }
                return true;
            }
        });

    }*/
}
