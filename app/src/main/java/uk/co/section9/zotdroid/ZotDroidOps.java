package uk.co.section9.zotdroid;

import android.app.Activity;
import android.content.ContentValues;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import uk.co.section9.zotdroid.data.AttachmentsTable;
import uk.co.section9.zotdroid.data.RecordsTable;
import uk.co.section9.zotdroid.data.ZotDroidDB;
import uk.co.section9.zotdroid.data.ZoteroAttachment;
import uk.co.section9.zotdroid.data.ZoteroNet;
import uk.co.section9.zotdroid.data.ZoteroRecord;

/**
 * Created by oni on 14/07/2017.
 *
 *
 * TODO - if we cancel a sync, we need to not replace anything!
 */

public class ZotDroidOps implements ZoteroNet.ZoteroTaskCallback{

    private ZoteroNet               _zotero_net = new ZoteroNet();
    private ZoteroWebDav            _zotero_webdav = new ZoteroWebDav();
    private ZotDroidDB              _zotdroid_db;
    private Activity                _activity;
    private ZotDroidCaller          _midnightcaller; // Was listening to Chase and Status ;)

    protected Vector<ZoteroRecord> _records = new Vector<ZoteroRecord>();
    protected Vector<ZoteroAttachment> _attachments = new Vector<ZoteroAttachment>();
    protected Map<String,ZoteroRecord> _key_to_record = new HashMap<String, ZoteroRecord>();

    public static final String TAG = "zotdroid.ZotDroidOps";

    public ZotDroidOps(Activity activity, ZotDroidCaller midnightcaller) {
        _activity = activity;
        _midnightcaller = midnightcaller;
        _zotdroid_db =  new ZotDroidDB(activity);
    }


    /**
     * A very small class that holds the state for our webdav attachment download
     * Its perhaps a bit complicated but it means we can do multiple requests and
     * not have the main activity worry too much.
     */
    private class OpsDav implements  ZoteroWebDav.ZoteroWebDavCallback {

        ZoteroAttachment _attachment;
        private OpsDav(ZoteroAttachment attachment){
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
    * Define the callbacks we want to use with our various activities
    */

    public interface ZotDroidCaller {
        void onSyncProgress(float progress);
        void onSyncFinish(boolean success, String message);
        void onDownloadProgress(float progress);
        void onDownloadFinish(boolean success, String message, String type);
    }


    /**
     * Perform a sync with Zotero, grabbing all the items
     */
    public void sync() {
        _zotdroid_db.reset(); // For now, just nuke the database
        _zotero_net.getItems(this);
    }

    public void stop() {
        _zotero_net.stop();
    }

    public Vector<ZoteroRecord> get_records() {
        return _records;
    }

    public ZoteroRecord get_record(int idx) {
        if (idx > 0 && idx < _records.size()) {
            return _records.elementAt(idx);
        }
        return null;
    }

    public void startAttachmentDownload(ZoteroRecord record, int idx){
        if (idx < record.get_attachments().size()) {
            ZoteroAttachment za = record.get_attachments().elementAt(idx);
            _zotero_webdav.downloadAttachment(za.get_zotero_key() + ".zip", za.get_file_name(), _activity, new OpsDav(za));
        }
        // TODO return somekind of false here
    }

    /**
     * Get all the records and attachments we have in the database
     */

    public void populateFromDB() {

        _records.clear();
        _attachments.clear();

        // Start with Records
        int numrows = _zotdroid_db.getNumRows(RecordsTable.get_table_name());

        for (int i=0; i < numrows; ++i){
            ContentValues values = null;
            values = _zotdroid_db.readRow(RecordsTable.get_table_name(), i);
            ZoteroRecord record = RecordsTable.getRecordFromValues(values);
            _key_to_record.put(record.get_zotero_key(),record);
            _records.add(record);
        }

        for ( String s : _key_to_record.keySet()){
            Log.i(TAG,s);
        }

        // Move on to attachments
        numrows = _zotdroid_db.getNumRows(AttachmentsTable.get_table_name());

        for (int i=0; i < numrows; ++i){
            ContentValues values = null;
            values = _zotdroid_db.readRow(AttachmentsTable.get_table_name(), i);
            ZoteroAttachment attachment = AttachmentsTable.getAttachmentFromValues(values);

            ZoteroRecord record = null;
            record = _key_to_record.get(attachment.get_parent());

            if (record != null){
                record.addAttachment(attachment);
            }
            _attachments.add(attachment);
        }
    }

    /**
     * Called when the sync task completes and we have a stack of results to process.
     * Clears the list and adds what we get from the server
     * TODO - eventually move this so all we do in this activity is UX stuff
     * @param success
     */
    @Override
    public void onItemCompletion(boolean success, float progress, String message, Vector<ZoteroRecord> records, Vector<ZoteroAttachment> attachments) {

        String status_message = "";
        if (success) {
            for (ZoteroRecord record : records){
                //_main_list_items.add(record.toString());
                _zotdroid_db.writeRecord(record);
            }

            for (ZoteroAttachment attachment : attachments) {
                _zotdroid_db.writeAttachment(attachment);
            }

            _midnightcaller.onSyncProgress(progress);

        } else {
            Log.d(TAG,"Error returned in onItemCompletion");
            _midnightcaller.onSyncFinish(false, "Error grabbing items from Zotero.");
        }
    }

    /**
     * Called when the sync task completes and we have a stack of results to process.
     * Clears the list and adds what we get from the server
     * @param success
     */
    @Override
    public void onItemsCompletion(boolean success) {
        _midnightcaller.onSyncFinish(success,"");
    }

    /**
     * Webdav testing callback
     * @param result
     * @param message
     */



}
