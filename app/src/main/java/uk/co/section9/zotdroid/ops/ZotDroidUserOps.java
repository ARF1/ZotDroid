package uk.co.section9.zotdroid.ops;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.io.File;
import java.util.Vector;
import uk.co.section9.zotdroid.Constants;
import uk.co.section9.zotdroid.R;
import uk.co.section9.zotdroid.Util;
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

    private ZoteroDownload _zotero_download = new ZoteroDownload();
    private ZotDroidWebDavCaller _midnightcaller; // Was listening to Chase and Status ;)
    public static final String TAG = "zotdroid.ZotDroidUserOps";
    private Collection _current_collection = null; // Which collection are we currently lookin at?
    private String _search_term = "";

    public ZotDroidUserOps(ZotDroidDB zotdroid_db, Activity activity, ZotDroidMem mem, ZotDroidWebDavCaller midnightcaller) {
        super(activity, zotdroid_db, mem);
        _midnightcaller = midnightcaller;
    }

    /**
     * Called as WebDav process runs
     *
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
    public void stop() {
        super.stop();
        _zotero_download.stop();
    }

    /**
     * Called when a webdav process completes
     *
     * @param result
     * @param message
     */
    @Override
    public void onWebDavComplete(boolean result, String message) {
        _midnightcaller.onWebDavTestFinish(result, message);
    }

    public void testWebDav() {
        _zotero_download.testWebDav(_activity, this);
    }

    /**
     * A very small class that holds the state for our webdav attachment download
     * Its perhaps a bit complicated but it means we can do multiple requests and
     * not have the main activity worry too much.
     */
    private class OpsDav implements ZoteroWebDavCallback {

        Attachment _attachment;

        private OpsDav(Attachment attachment) {
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
     *
     * @param record
     * @param attachment_idx
     */

    public void startAttachmentDownload(Record record, int attachment_idx) {
        if (attachment_idx < record.get_attachments().size()) {
            Attachment za = record.get_attachments().elementAt(attachment_idx);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(_activity);

            // If the file already exists, then we dont need to download, just return
            File file = new File(Util.getDownloadDirectory(_activity) + za.get_file_name());

            if (file.exists()) {
                OpsDav op = new OpsDav(za);
                op.onWebDavComplete(true, Util.getDownloadDirectory(_activity) + za.get_file_name());
            } else {
                Boolean usewebdav = settings.getBoolean("settings_use_webdav_storage", false);

                if (usewebdav) {
                    String username = settings.getString("settings_webdav_username", "username");
                    String password = settings.getString("settings_webdav_password", "password");
                    String server_address = settings.getString("settings_webdav_address", "address");
                    _zotero_download.downloadAttachment(za.get_zotero_key() + ".zip", Util.getDownloadDirectory(_activity),
                            za.get_file_name(), username, password, server_address, new OpsDav(za));
                } else {
                    _zotero_download.downloadAttachmentZotero(Util.getDownloadDirectory(_activity),
                            za.get_file_name(), za.get_zotero_key(), new OpsDav(za));
                }
            }
        }
        // TODO return somekind of false here
    }

    /**
     * Nuke memory and go back to where we were before, usually after a search
     */
    public void reset() {
        _zotdroid_mem.nukeMemory();
        _search_term = "";
        filter(_current_collection, "", Constants.PAGINATION_SIZE);
    }

    /**
     * Filter the collection based on the previous state of the system and the new state as
     * passed in by searchterm, end index and current collection.
     * @param collection
     * @param searchterm
     * @param end
     */
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
            trv = _zotdroid_db.getRecords(end);
        }

        // Search is an entirely new view across the entire collection, not just what is
        // visible currently. I figure that might be better. This does make the scroll harder
        // TODO - create a method to see if there are more records available in current search
        if (!searchterm.isEmpty() && _search_term != searchterm) {
            _zotdroid_mem.nukeMemory();
            Vector<Record> matching = new Vector<>();
            trv = _zotdroid_db.searchRecords(_current_collection, searchterm, Constants.PAGINATION_SIZE);
            _search_term = searchterm;
        } else if (!searchterm.isEmpty() && _search_term == searchterm){
            trv = _zotdroid_db.searchRecords(_current_collection, searchterm, end);
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

    public void getMoreResults(int more) {
        filter(_current_collection, _search_term, _zotdroid_mem._end_index + more);
    }

    /**
     * See if there are more results to return. If we are currently searching
     * we check if there are more results across the entire collection.
     * @return
     */
    public boolean hasMoreResults() {
        if (_search_term.isEmpty()) {
            int total = _zotdroid_db.getNumRecordsCollection(_current_collection);
            if (_zotdroid_mem._end_index + 1 < total) {
                return true;
            }
        } else {
            int total = _zotdroid_db.getNumRecordsSearch(_current_collection, _search_term);
            if (_zotdroid_mem._end_index + 1 < total) {
                return true;
            }
        }
        return false;
    }

    public void swapCollection(Collection collection) {
        if (collection != _current_collection) {
            filter(collection, "", Constants.PAGINATION_SIZE);
        }
    }

    public void search(String term) {
        filter(_current_collection, term, Constants.PAGINATION_SIZE);
    }

    /**
     * Update straight to the DB. Also set the record to be unsynced
     * this essentially writes a change that needs to be reflected
     * @param r
     */
    public void commitRecord(Record r) {
        r.set_synced(false);
        _zotdroid_db.updateRecord(r);
    }

}

