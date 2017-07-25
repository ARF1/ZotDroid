package uk.co.section9.zotdroid;

import android.app.Activity;
import android.content.ContentValues;
import android.os.Environment;
import android.preference.Preference;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import uk.co.section9.zotdroid.data.AttachmentsTable;
import uk.co.section9.zotdroid.data.CollectionsTable;
import uk.co.section9.zotdroid.data.RecordsTable;
import uk.co.section9.zotdroid.data.ZotDroidDB;
import uk.co.section9.zotdroid.data.ZoteroAttachment;
import uk.co.section9.zotdroid.data.ZoteroCollection;
import uk.co.section9.zotdroid.data.ZoteroRecord;
import uk.co.section9.zotdroid.data.task.ZoteroItemsTask;
import uk.co.section9.zotdroid.data.task.ZoteroCollectionsTask;
import uk.co.section9.zotdroid.data.task.ZoteroTask;
import uk.co.section9.zotdroid.data.task.ZoteroTaskCallback;

/**
 * Created by oni on 14/07/2017.
 *
 *
 * TODO - if we cancel a sync, we need to not replace anything!
 */

public class ZotDroidOps implements ZoteroTaskCallback {

    private ZoteroWebDav            _zotero_webdav = new ZoteroWebDav();
    private ZotDroidDB              _zotdroid_db;
    private Activity                _activity;
    private ZotDroidCaller          _midnightcaller; // Was listening to Chase and Status ;)
    private static String           _download_path;
    private Vector<ZoteroTask>      _current_tasks;

    protected Vector<ZoteroRecord>      _records = new Vector<ZoteroRecord>();
    protected Vector<ZoteroAttachment>  _attachments = new Vector<ZoteroAttachment>();
    protected Map<String,ZoteroRecord>  _key_to_record = new HashMap<String, ZoteroRecord>();
    protected Vector<ZoteroCollection>  _collections = new Vector<ZoteroCollection>();

    public static final String TAG = "zotdroid.ZotDroidOps";

    public ZotDroidOps(Activity activity, ZotDroidCaller midnightcaller) {
        _activity = activity;
        _midnightcaller = midnightcaller;
        _zotdroid_db =  new ZotDroidDB(activity);
        _current_tasks = new Vector<ZoteroTask>();

        // Make our working directory, mostly for attachments
        _download_path = Environment.getExternalStorageDirectory().toString() + "/ZotDroid/";
        File root_dir = new File(_download_path);
        root_dir.mkdirs();
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
        _current_tasks.add(new ZoteroCollectionsTask(this,0,25));
        _current_tasks.add(new ZoteroItemsTask(this,0,25));

        for (ZoteroTask t : _current_tasks) {
            t.startZoteroTask();
        }
    }

    public void stop() {
        for (ZoteroTask t : _current_tasks){
            t.cancel(true);
        }
        _current_tasks.clear();
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

    /**
     * Download an attachement, unless it already exists, in which case, callback immediately
     * @param record
     * @param attachment_idx
     */

    public void startAttachmentDownload(ZoteroRecord record, int attachment_idx){
        if (attachment_idx < record.get_attachments().size()) {
            ZoteroAttachment za = record.get_attachments().elementAt(attachment_idx);

            // If the file already exists, then we dont need to download, just return
            File file = new File( _download_path + za.get_file_name());
            if (file.exists()){
                OpsDav op = new OpsDav(za);
                op.onWebDavComplete(true, _download_path + za.get_file_name());
            } else {
                _zotero_webdav.downloadAttachment(za.get_zotero_key() + ".zip", _download_path, za.get_file_name(), _activity, new OpsDav(za));
            }
        }
        // TODO return somekind of false here
    }

    /**
     * Get all the records and attachments we have in the database and populate what we need in Memory
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

        // DEBUG log for now
        for ( String s : _key_to_record.keySet()){
            Log.d(TAG,s);
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

        // Now finish with collections

        numrows = _zotdroid_db.getNumRows(CollectionsTable.get_table_name());

        for (int i=0; i < numrows; ++i) {
            ContentValues values = null;
            values = _zotdroid_db.readRow(CollectionsTable.get_table_name(), i);
            ZoteroCollection collection = CollectionsTable.getCollectionFromValues(values);
            _collections.add(collection);
        }

        // Go through and create our 'pointers'
        // Could be slow for big collections

        for (ZoteroCollection c : _collections){
            for (ZoteroCollection d : _collections){
                if (d.get_parent() == c.get_zotero_key()){
                    c.add_collection(d);
                }
            }
        }



    }

    /**
     * Called when the sync task completes and we have a stack of results to process.
     * Clears the list and adds what we get from the server
     * TODO - eventually move this so all we do in this activity is UX stuff
     * @param success
     */
    @Override
    public void onItemCompletion(ZoteroTask task, boolean success, String message, int new_index, int total, Vector<ZoteroRecord> records, Vector<ZoteroAttachment> attachments) {
        _current_tasks.remove(task);

        String status_message = "";
        if (success) {
            for (ZoteroRecord record : records){
                //_main_list_items.add(record.toString());
                _zotdroid_db.writeRecord(record);
            }

            for (ZoteroAttachment attachment : attachments) {
                _zotdroid_db.writeAttachment(attachment);
            }

            _midnightcaller.onSyncProgress((float)new_index / (float)total);

            // We fire off another task from here if success and we have more to go
            if (new_index + 1 <= total){
                ZoteroTask t = new ZoteroItemsTask(this, new_index ,25);
                _current_tasks.add (t);
                t.startZoteroTask();
            } else {
                onItemsCompletion(task,success,message);
                return;
            }

        } else {
            Log.e(TAG,"Error returned in onItemCompletion");
            _midnightcaller.onSyncFinish(false, "Error grabbing items from Zotero.");
        }
    }

    /**
     * Called when the sync task completes and we have a stack of results to process.
     * Clears the list and adds what we get from the server
     * @param success
     */
    @Override
    public void onItemsCompletion(ZoteroTask task, boolean success, String message) {
        _current_tasks.remove(task);
        if (success) {
            if (_current_tasks.isEmpty()) {
                _midnightcaller.onSyncFinish(success, message);
            }
        } else {
            for (ZoteroTask t : _current_tasks){
                t.cancel(true);
            }
            _current_tasks.clear();
            _midnightcaller.onSyncFinish(success, message);
        }
    }

    @Override
    public void onCollectionsCompletion(ZoteroTask task, boolean success, String message, Vector<ZoteroCollection> collections) {
        _current_tasks.remove(task);

        if (success) {
            for (ZoteroCollection collection : collections){
                //_main_list_items.add(record.toString());
                _zotdroid_db.writeCollection(collection);
            }

            if (_current_tasks.isEmpty()) {
                _midnightcaller.onSyncFinish(success, message);
            }

        } else {
            for (ZoteroTask t : _current_tasks){
                t.cancel(true);
            }
            _current_tasks.clear();
            _midnightcaller.onSyncFinish(success, message);
        }
    }


}