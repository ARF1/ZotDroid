package uk.co.section9.zotdroid;

import android.app.Activity;
import android.content.ContentValues;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import uk.co.section9.zotdroid.data.AttachmentsTable;
import uk.co.section9.zotdroid.data.CollectionsItemsTable;
import uk.co.section9.zotdroid.data.CollectionsTable;
import uk.co.section9.zotdroid.data.RecordsTable;
import uk.co.section9.zotdroid.data.ZotDroidDB;
import uk.co.section9.zotdroid.data.ZoteroAttachment;
import uk.co.section9.zotdroid.data.ZoteroCollection;
import uk.co.section9.zotdroid.data.ZoteroCollectionItem;
import uk.co.section9.zotdroid.data.ZoteroRecord;
import uk.co.section9.zotdroid.data.ZoteroSummary;
import uk.co.section9.zotdroid.data.task.ZoteroItemsTask;
import uk.co.section9.zotdroid.data.task.ZoteroCollectionsTask;
import uk.co.section9.zotdroid.data.task.ZoteroSyncTask;
import uk.co.section9.zotdroid.data.task.ZoteroTask;
import uk.co.section9.zotdroid.data.task.ZoteroTaskCallback;
import uk.co.section9.zotdroid.data.task.ZoteroVerColTask;
import uk.co.section9.zotdroid.data.task.ZoteroVerItemsTask;

/**
 * Created by oni on 14/07/2017.
 *
 * This class performs the operations needed between Zotero and our
 * database and in-memory representation of the Zotero Library in question.
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
     * We start with collections as records depend on them
     * When collections completes, we'll start on items
     * This method nukes everything and rebuilds from scratch
     */
    public void resetAndSync() {
        _zotdroid_db.reset();
        nukeMemory(); // Eventualy do something better.
        _current_tasks.add(new ZoteroCollectionsTask(this,0,25));
        _current_tasks.add(new ZoteroItemsTask(this,0,25));

        for (ZoteroTask t : _current_tasks) {
            t.startZoteroTask();
        }
    }

    /**
     * A standard sync where we do things a bit more intelligent like
     */

    public void sync(){

        ZoteroSummary s = _zotdroid_db.getSummary();

        _current_tasks.add(new ZoteroSyncTask(this,s.get_last_version_items(), s.get_last_version_collections()));

        for (ZoteroTask t : _current_tasks) {
            t.startZoteroTask();
        }
    }

    private void nukeMemory(){
        _records.clear();
        _collections.clear();
        _attachments.clear();
        _key_to_record.clear();
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

    public Vector<ZoteroCollection> get_collections() {
        return _collections;
    }

    public ZoteroCollection get_collection(int idx) {
        if (idx >= 0 && idx < _collections.size()) {
            return _collections.elementAt(idx);
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
     * Make sure our internal memory is up to date, either from DB or Zotero
     * Eventually, we will add a proper sync check in here as well.
     */

    public void update(){
        if (_records.isEmpty()) {
            populateFromDB();
        }
    }

    /**
     * Get all the records and attachments we have in the database and populate what we need in Memory
     */

    public void populateFromDB() {

        nukeMemory();

        // Start with Records
        int numrows = _zotdroid_db.getNumRows(RecordsTable.get_table_name());

        for (int i=0; i < numrows; ++i){
            ContentValues values = null;
            values = _zotdroid_db.readRow(RecordsTable.get_table_name(), i);
            ZoteroRecord record = RecordsTable.getRecordFromValues(values);
            _key_to_record.put(record.get_zotero_key(),record);
            _records.add(record);
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

        // Now go with collections

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

        // This bit could be slow if there are loads of collections. There will be a faster
        // way to do it I would say.

        // Add the records to the collections they belong to.
        numrows = _zotdroid_db.getNumRows(CollectionsItemsTable.get_table_name());

        for (int i=0; i < numrows; ++i) {
            ContentValues values = null;
            values = _zotdroid_db.readRow(CollectionsItemsTable.get_table_name(), i);
            ZoteroCollectionItem ct = CollectionsItemsTable.getCollectionFromValues(values);

            for (ZoteroCollection c : _collections){
                if (ct.get_collection().contains(c.get_zotero_key())){
                    for (ZoteroRecord r : _records){
                        if ( ct.get_item().contains(r.get_zotero_key())){
                            r.addCollection(c);
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }

    // TODO - these three are essentially the same if we abstract away so we should
    // We'd have to have overridden methods for writing to the DB, existance and get_key/version

    private void checkUpdateRecord(ZoteroRecord record) {
        if (!_zotdroid_db.recordExists(record.get_zotero_key())) {
            _zotdroid_db.writeRecord(record);
        } else {
            // Check the version numbers if this exists and update as necessary
            ZoteroRecord existing = _zotdroid_db.getRecord(record.get_zotero_key());
            if (Integer.valueOf(existing.get_version()) < Integer.valueOf(record.get_version())){
                // Perform an update :)

            } else {
                Log.d(TAG, "An attachment brought down has an older version.");
            }
        }
    }

    private void checkUpdateAttachment(ZoteroAttachment attachment) {
        if (!_zotdroid_db.attachmentExists(attachment.get_zotero_key())) {
            _zotdroid_db.writeAttachment(attachment);
        } else {
            // Check the version numbers if this exists and update as necessary
            ZoteroAttachment existing = _zotdroid_db.getAttachment(attachment.get_zotero_key());
            if (Integer.valueOf(existing.get_version()) < Integer.valueOf(attachment.get_version())){
                // Perform an update :)

            } else {
                Log.d(TAG, "An attachment brought down has an older version.");
            }
        }
    }

    private void checkUpdateCollection(ZoteroCollection collection) {
        if (!_zotdroid_db.collectionExists(collection.get_zotero_key())) {
            _zotdroid_db.writeCollection(collection);
        } else {
            // Check the version numbers if this exists and update as necessary
            ZoteroCollection existing = _zotdroid_db.getCollection(collection.get_zotero_key());
            if (Integer.valueOf(existing.get_version()) < Integer.valueOf(collection.get_version())){
                // Perform an update :)

            } else {
                Log.d(TAG, "A collection brought down has an older version.");
            }
        }
    }


    @Override
    public void onItemCompletion(ZoteroTask task, boolean success, String message, Vector<ZoteroRecord> records,
                                 Vector<ZoteroAttachment> attachments, String version) {

        if (_current_tasks.isEmpty()) {
            onSyncCompletion(task, true,"Sync completed");
        }

    }

    /**
     * Called when the sync task completes and we have a stack of results to process.
     * Clears the list and adds what we get from the server
     * TODO - eventually move this so all we do in this activity is UX stuff
     * @param success
     */
    @Override
    public void onItemCompletion(ZoteroTask task, boolean success, String message, int new_index,
                                 int total, Vector<ZoteroRecord> records,
                                 Vector<ZoteroAttachment> attachments, String version) {

        _current_tasks.remove(task);

        String status_message = "";
        if (success) {
            for (ZoteroRecord record : records){
                checkUpdateRecord(record);
            }

            for (ZoteroAttachment attachment : attachments) {
                checkUpdateAttachment(attachment);
            }

            _midnightcaller.onSyncProgress((float)new_index / (float)total);

            // We fire off another task from here if success and we have more to go
            if (new_index + 1 <= total){
                ZoteroTask t = new ZoteroItemsTask(this, new_index ,25);
                _current_tasks.add (t);
                t.startZoteroTask();
            } else {
                onItemsCompletion(task,success,message, version);
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
    public void onItemsCompletion(ZoteroTask task, boolean success, String message, String version) {
        _current_tasks.remove(task);
        if (success) {

            ZoteroSummary s = _zotdroid_db.getSummary();
            s.set_last_version_items(version);
            _zotdroid_db.writeSummary(s);
            Log.i(TAG,"Items Complete Version: " + version);

            if (_current_tasks.isEmpty()) {
                onSyncCompletion(task, true,"Sync completed");
            }
        } else {
            for (ZoteroTask t : _current_tasks){
                t.cancel(true);
            }
            _current_tasks.clear();
            _midnightcaller.onSyncFinish(false, message);
        }
    }

    /**
     * Called when all the collections have finished being pulled down and processed
     * @param task
     * @param success
     * @param message
     * @param version
     */

    public void onCollectionsCompletion(ZoteroTask task, boolean success, String message, String version) {
        _current_tasks.remove(task);
        if (success) {
            ZoteroSummary s = _zotdroid_db.getSummary();
            s.set_last_version_collections(version);
            _zotdroid_db.writeSummary(s);
            Log.i(TAG,"Collections Complete Version: " + version);

            if (_current_tasks.isEmpty()){
                onSyncCompletion(task, true,"Sync completed");
            }

        } else {
            for (ZoteroTask t : _current_tasks){
                t.cancel(true);
            }
            _current_tasks.clear();
            _midnightcaller.onSyncFinish(false, message);
        }
    }

    public void onCollectionCompletion(ZoteroTask task, boolean success, String message, Vector<ZoteroCollection> collections, String version) {
        _current_tasks.remove(task);

        if (success) {
            for (ZoteroCollection collection : collections){
                checkUpdateCollection(collection);
            }

            if (_current_tasks.isEmpty()) {
                onCollectionsCompletion(task, true, message, version);
            }

        } else {
            for (ZoteroTask t : _current_tasks){
                t.cancel(true);
            }
            _current_tasks.clear();
            _midnightcaller.onSyncFinish(success, message);
        }

    }

    /**
     * Collections have completed. This is called when we have a new set of collections
     * We need to check if these already exist or not, as these can come from both reset
     * or normal sync
     * TODO - We need to check for any possible conflicts here
     * @param task
     * @param success
     * @param message
     * @param collections
     */

    public void onCollectionCompletion(ZoteroTask task, boolean success, String message, int new_index,
                                       int total, Vector<ZoteroCollection> collections, String version) {
        _current_tasks.remove(task);

        if (success) {
            for (ZoteroCollection collection : collections){
                if (!_zotdroid_db.collectionExists(collection.get_zotero_key())) {
                    _zotdroid_db.writeCollection(collection);
                } else {
                    // Check the version numbers if this exists and update as necessary
                    ZoteroCollection existing = _zotdroid_db.getCollection(collection.get_zotero_key());
                    if (Integer.valueOf(existing.get_version()) < Integer.valueOf(collection.get_version())){
                        // Perform an update :)

                    } else {
                        Log.d(TAG, "A collection brought down has an older version.");
                    }
                }
            }

            // We fire off another task from here if success and we have more to go
            if (new_index + 1 <= total){
                ZoteroTask t = new ZoteroCollectionsTask(this, new_index ,25);
                _current_tasks.add (t);
                t.startZoteroTask();
            } else {
                onCollectionsCompletion(task, success ,message, version);
                return;
            }

        } else {
            for (ZoteroTask t : _current_tasks){
                t.cancel(true);
            }
            _current_tasks.clear();
            _midnightcaller.onSyncFinish(success, message);
        }
    }

    /**
     * We now have a list of things that have changed
     * @param task
     * @param success
     * @param message
     */
    public void onItemVersion(ZoteroTask task, boolean success, String message, Vector<String> items){
        Log.i(TAG, "Number of items that need updating: " + items.size());

        Vector<String> keys = new Vector<String>();
        for (int i =0; i < items.size(); ++i){
            keys.add(items.get(i));
            if (keys.size() >= 20) {
                ZoteroItemsTask zc = new ZoteroItemsTask(this, keys);
                _current_tasks.add(zc);
                keys.clear();
            }
        }

        if (!keys.isEmpty()){
            ZoteroItemsTask zc  = new ZoteroItemsTask(this, keys);
            _current_tasks.add(zc);
        }

        for (ZoteroTask t : _current_tasks) {
            t.startZoteroTask();
        }
    }

    /**
     * We now have a list of collections that have changed
     * @param task
     * @param success
     * @param message
     */
    public void onCollectionVersion(ZoteroTask task, boolean success, String message, Vector<String> items){

        _current_tasks.remove(task);

        Log.i(TAG, "Number of collections that need updating: " + items.size());

        // We now need to stagger the download and processing of these

        Vector<String> keys = new Vector<String>();
        for (int i =0; i < items.size(); ++i){
            keys.add(items.get(i));
            if (keys.size() >= 20) {
                ZoteroCollectionsTask zc  = new ZoteroCollectionsTask(this, keys);
                _current_tasks.add(zc);
                keys.clear();
            }
        }

        if (!keys.isEmpty()){
            ZoteroCollectionsTask zc  = new ZoteroCollectionsTask(this, keys);
            _current_tasks.add(zc);
        }

        for (ZoteroTask t : _current_tasks) {
            t.startZoteroTask();
        }

    }

    /**
     * Called when our task to find items to delete returns
     * @param task
     * @param success
     * @param message
     * @param items
     * @param collections
     */
    @Override
    public void onSyncDelete(ZoteroTask task, boolean success, String message, Vector<String> items, Vector<String> collections) {

    }


    /**
     * Callback from the first step of syncing, where we check the versions we have against the server
     * @param task
     * @param success
     * @param message
     * @param records_version
     * @param collections_version
     */
    @Override
    public void onSyncVersion(ZoteroTask task, boolean success, String message, String records_version, String collections_version) {
        _current_tasks.remove(task);

        if (success) {
            Log.i(TAG,"Current Sync Items: " + getItemsVersion() + " New Version: " + records_version);
            Log.i(TAG,"Current Sync Collections: " + getCollectionsVersion() + " New Version: " + collections_version);

            // Fire up the syncs we need to do
            if (getItemsVersion() != records_version){
                ZoteroVerItemsTask zv = new ZoteroVerItemsTask(this, getItemsVersion());
                _current_tasks.add(zv);
                zv.startZoteroTask();
            }

            if (getCollectionsVersion() != collections_version) {
                ZoteroVerColTask zc = new ZoteroVerColTask(this, getCollectionsVersion());
                _current_tasks.add(zc);
                zc.startZoteroTask();
            }

        } else {
            onSyncCompletion(task, false, message);
        }
    }


    /**
     * Once we have both collections and items, we need to properly link them up
     * We create the CollectionItems entries in the database for use later
     */

    private void collectionItemsCreate() {

        Vector<ZoteroRecord> records = _zotdroid_db.getRecords();

        for (ZoteroRecord record : records){
            for (String ts : record.get_temp_collections()){
                ZoteroCollectionItem ci = new ZoteroCollectionItem();
                ci.set_item(record.get_zotero_key());
                ci.set_collection(ts);
                _zotdroid_db.writeCollectionItem(ci);
            }
            record.get_temp_collections().clear();
        }
    }


    /**
     * Get the currently held Items Version
     * @return
     */
    public String getItemsVersion() { return _zotdroid_db.getSummary().get_last_version_items(); }

    /**
     * Get the currently held Collections Version
     * @return
     */
    public String getCollectionsVersion() { return _zotdroid_db.getSummary().get_last_version_collections(); }


    public void onSyncCompletion(ZoteroTask task, boolean success, String message) {
        collectionItemsCreate();
        _current_tasks.remove(task);
        _midnightcaller.onSyncFinish(success,message);
    }
}
