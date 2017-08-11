package uk.co.section9.zotdroid;

import android.app.Activity;
import android.content.ContentValues;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import uk.co.section9.zotdroid.data.ZotDroidDB;
import uk.co.section9.zotdroid.data.ZoteroAttachment;
import uk.co.section9.zotdroid.data.ZoteroCollection;
import uk.co.section9.zotdroid.data.ZoteroCollectionItem;
import uk.co.section9.zotdroid.data.ZoteroRecord;
import uk.co.section9.zotdroid.data.ZoteroSummary;
import uk.co.section9.zotdroid.data.task.ZoteroDelTask;
import uk.co.section9.zotdroid.data.task.ZoteroItemsTask;
import uk.co.section9.zotdroid.data.task.ZoteroCollectionsTask;
import uk.co.section9.zotdroid.data.task.ZoteroSyncColTask;
import uk.co.section9.zotdroid.data.task.ZoteroSyncItemsTask;
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

    private int _num_current_tasks = 0;
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
        nextTask();
    }

    /**
     * A standard sync where we do things a bit more intelligent like
     * We start with the collections, then move onto records / items
     */

    public void sync(){
        ZoteroSummary s = _zotdroid_db.getSummary();
        ZoteroSyncColTask zs = new ZoteroSyncColTask(this, s.get_last_version());
        _current_tasks.add(zs);
        ZoteroSyncItemsTask zt = new ZoteroSyncItemsTask(this,s.get_last_version());
        _current_tasks.add(zt);
        nextTask();
    }

    private void nukeMemory(){
        _records.clear();
        _collections.clear();
        _attachments.clear();
        _key_to_record.clear();
    }

    private boolean nextTask() {
        if (_current_tasks.isEmpty()) { return false; }
        _current_tasks.get(0).startZoteroTask();
        _current_tasks.remove(0);
        return true;
    }

    public void stop() {
        for (ZoteroTask t : _current_tasks){ t.cancel(true); }
        _current_tasks.clear();
        _num_current_tasks = 0;
    }

    public Vector<ZoteroRecord> get_records() {
        return _records;
    }

    public ZoteroRecord get_record(int idx) {
        if (idx > 0 && idx < _records.size()) { return _records.elementAt(idx); }
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
        if (_records.isEmpty()) { populateFromDB(); }
    }

    /**
     * Get all the records and attachments we have in the database and populate what we need in Memory
     */

    public void populateFromDB() {
        nukeMemory();
        // Start with Records
        int numrows = _zotdroid_db.getNumRecords();

        for (int i=0; i < numrows; ++i){
            ZoteroRecord record = _zotdroid_db.getRecord(i);
            _key_to_record.put(record.get_zotero_key(),record);
            _records.add(record);
        }

        // Move on to attachments
        numrows = _zotdroid_db.getNumAttachments();

        for (int i=0; i < numrows; ++i){
            ZoteroAttachment attachment = _zotdroid_db.getAttachment(i);
            ZoteroRecord record = null;
            record = _key_to_record.get(attachment.get_parent());
            if (record != null){ record.addAttachment(attachment); }
            _attachments.add(attachment);
        }

        // Now go with collections

        numrows = _zotdroid_db.getNumCollections();

        for (int i=0; i < numrows; ++i) {
            ContentValues values = null;
            ZoteroCollection collection = _zotdroid_db.getCollection(i);
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
        numrows = _zotdroid_db.getNumCollectionsItems();

        for (int i=0; i < numrows; ++i) {
            ZoteroCollectionItem ct = _zotdroid_db.getCollectionItem(i);

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

    private void checkUpdateRecord(ZoteroRecord record) {
        if (!_zotdroid_db.recordExists(record.get_zotero_key())) {
            _zotdroid_db.writeRecord(record);
            collectionItemsCreate(record); // This is why collections MUST be synced first
        } else {
            // Check the version numbers if this exists and update as necessary
            ZoteroRecord existing = _zotdroid_db.getRecord(record.get_zotero_key());
            if (Integer.valueOf(existing.get_version()) < Integer.valueOf(record.get_version())){
                // Perform an update :)
                _zotdroid_db.updateRecord(record);

                // At this point the record will likely have different collections too
                // so we simply rebuild them from our new fresh record
                _zotdroid_db.removeRecordFromCollections(record.get_zotero_key());
                collectionItemsCreate(record);

            } else {
                Log.d(TAG, "A record brought down has an older/same version.");
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
                _zotdroid_db.updateAttachment(attachment);
            } else {
                Log.d(TAG, "An attachment brought down has an older/same version.");
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
                // Perform an update. This is tricky because we could essentially change a collection
                // and move an object :/
                // For now we just update the values but we should check whether or not this collection is in the trash
                _zotdroid_db.updateCollection(collection);

            } else {
                Log.d(TAG, "A collection brought down has an older version.");
            }
        }
    }

    /**
     * This is called when an item is completed, specifically on a sync task (i.e not a reset and sync task)
     * @param task
     * @param success
     * @param message
     * @param records
     * @param attachments
     * @param version
     */

    @Override
    public void onItemCompletion(ZoteroTask task, boolean success, String message, Vector<ZoteroRecord> records,
                                 Vector<ZoteroAttachment> attachments, String version) {
        _midnightcaller.onSyncProgress( (float)(_num_current_tasks  - _current_tasks.size()) / (float)_num_current_tasks);
        if (success) {
            for (ZoteroRecord record : records){ checkUpdateRecord(record); }
            for (ZoteroAttachment attachment : attachments){ checkUpdateAttachment(attachment); }
            if (!nextTask()) { onItemsCompletion(task, true, message, version); }
        } else {
            stop();
            _midnightcaller.onSyncFinish(success, message);
        }
    }

    /**
     * Called when the sync task completes and we have a stack of results to process.
     * Clears the list and adds what we get from the server
     * This version is called on a reset and sync where we have indices into the data
     * @param success
     */
    @Override
    public void onItemCompletion(ZoteroTask task, boolean success, String message, int new_index,
                                 int total, Vector<ZoteroRecord> records,
                                 Vector<ZoteroAttachment> attachments, String version) {
        String status_message = "";
        if (success) {
            for (ZoteroRecord record : records){ checkUpdateRecord(record); }
            for (ZoteroAttachment attachment : attachments) { checkUpdateAttachment(attachment);}

            _midnightcaller.onSyncProgress((float)new_index / (float)total);
            // We fire off another task from here if success and we have more to go
            if (new_index + 1 <= total){
                ZoteroTask t = new ZoteroItemsTask(this, new_index ,25);
                _current_tasks.add(0,t);
                nextTask();
            } else {
                onItemsCompletion(task,success,message, version);
                return;
            }

        } else {
            Log.e(TAG,"Error returned in onItemCompletion");
            stop();
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
        if (success) {
            ZoteroSummary s = _zotdroid_db.getSummary();
            Log.i(TAG,"Items Complete Version: " + version);
            ZoteroDelTask dt = new ZoteroDelTask(this, s.get_last_version());
            _current_tasks.add(0,dt);
            nextTask();

        } else {
            stop();
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
        if (success) {
            ZoteroSummary s = _zotdroid_db.getSummary();
            Log.i(TAG,"Collections Complete Version: " + version);
            nextTask();
        } else {
           stop();
            _midnightcaller.onSyncFinish(false, message);
        }
    }

    public void onCollectionCompletion(ZoteroTask task, boolean success, String message, Vector<ZoteroCollection> collections, String version) {
        if (success) {
            for (ZoteroCollection collection : collections){
                checkUpdateCollection(collection);
            }
            if (!nextTask()) { onCollectionsCompletion(task, true, message, version); }

        } else {
            stop();
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
                _current_tasks.add(0,t);
                nextTask();

            } else {
                onCollectionsCompletion(task, success ,message, version);
                return;
            }

        } else {
            stop();
            _midnightcaller.onSyncFinish(success, message);
        }
    }

    /**
     * We now have a list of things that have changed
     * @param task
     * @param success
     * @param message
     */
    public void onItemVersion(ZoteroTask task, boolean success, String message, Vector<String> items, String version){

        Log.i(TAG, "Number of items that need updating: " + items.size());
        if (items.size() > 0) {
            Vector<String> keys = new Vector<String>();
            for (int i = 0; i < items.size(); ++i) {
                Log.i(TAG,"Update Key: " + items.get(i));
                keys.add(items.get(i));
                if (keys.size() >= 20) {
                    ZoteroItemsTask zc = new ZoteroItemsTask(this, keys);
                    _current_tasks.add(0, zc);
                    keys.clear();
                }
            }

            if (!keys.isEmpty()) {
                ZoteroItemsTask zc = new ZoteroItemsTask(this, keys);
                _current_tasks.add(0, zc);
            }

            _num_current_tasks = _current_tasks.size(); // Set this so we can have progress bars
            nextTask();
        } else {
            onItemsCompletion(task,success,message,version);
        }
    }

    /**
     * We now have a list of collections that have changed
     * @param task
     * @param success
     * @param message
     */
    public void onCollectionVersion(ZoteroTask task, boolean success, String message, Vector<String> items, String version){

        Log.i(TAG, "Number of collections that need updating: " + items.size());
        // We now need to stagger the download and processing of these
        if (items.size() > 0 ) {
            Vector<String> keys = new Vector<String>();
            for (int i = 0; i < items.size(); ++i) {
                keys.add(items.get(i));
                if (keys.size() >= 20) {
                    ZoteroCollectionsTask zc = new ZoteroCollectionsTask(this, keys);
                    _current_tasks.add(0, zc);
                    keys.clear();
                }
            }

            if (!keys.isEmpty()) {
                ZoteroCollectionsTask zc = new ZoteroCollectionsTask(this, keys);
                _current_tasks.add(0, zc);
            }
            nextTask();
        } else {
            // Nothing to do so we are up-to-date and can complete
            onCollectionsCompletion(task, true, message, version);
        }
    }

    /**
     * Called when our task to find items to delete returns
     * @param task
     * @param success
     * @param message
     * @param items
     * @param collections
     * @param version
     */
    @Override
    public void onSyncDelete(ZoteroTask task, boolean success, String message, Vector<String> items, Vector<String> collections, String version) {
        Log.i(TAG,"Collections to delete: " + collections.size());
        Log.i(TAG,"Items to delete: " + items.size());

        for (String key : items){
            // Could be either but it wont fail if we get the wrong one
            _zotdroid_db.deleteAttachment(key);
            _zotdroid_db.deleteRecord(key);
        }

        for (String key : collections){ _zotdroid_db.deleteCollection(key); }
        if (!nextTask()) { onSyncCompletion(task, true,"Sync completed", version); }
    }

    /**
     * Callback from the first step of syncing, where we check the versions we have against the server
     * @param task
     * @param success
     * @param message
     * @param version
     */
    @Override
    public void onSyncCollectionsVersion(ZoteroTask task, boolean success, String message, String version) {

        if (success) {
            Log.i(TAG,"Current Sync Collections: " + getVersion() + " New Version: " + version);
            // Start with collections sync and then items afterwards
            if (getVersion() != version) {
                ZoteroVerColTask zc = new ZoteroVerColTask(this, getVersion());
                _current_tasks.add(0,zc);
                nextTask();
            } else {
                onCollectionsCompletion(task,true,"Collections are up-to-date.",getVersion());
            }
        } else { stop(); onSyncCompletion(task, false, message, version); }
    }

    @Override
    public void onSyncItemsVersion(ZoteroTask task, boolean success, String message, String version) {
        if (success) {
            Log.i(TAG,"Current Sync Items: " + getVersion() + " New Version: " + version);
            // Now we move onto the records if we need to
            if (getVersion() != version){
                ZoteroVerItemsTask zv = new ZoteroVerItemsTask(this, getVersion());
                _current_tasks.add(0,zv);
                nextTask();
            } else {
                // We dont need to sync records so we are done (aside from deletion)
                onSyncCompletion(task, true, "Sync completed", version);
            }
        } else { stop(); onSyncCompletion(task, false, message, version); }
    }

    /**
     * Once we have both collections and items, we need to properly link them up
     * We create the CollectionItems entries in the database for use later
     */

    private void collectionItemsCreate(ZoteroRecord record) {
        for (String ts : record.get_temp_collections()){
            ZoteroCollectionItem ci = new ZoteroCollectionItem();
            ci.set_item(record.get_zotero_key());
            ci.set_collection(ts);
            _zotdroid_db.writeCollectionItem(ci);
        }
        record.get_temp_collections().clear();
    }

    /**
     * Get the currently held Items Version
     * @return
     */
    public String getVersion() { return _zotdroid_db.getSummary().get_last_version(); }

    /**
     * Final call back - the sync has completed
     * @param task
     * @param success
     * @param message
     * @param version
     */
    public void onSyncCompletion(ZoteroTask task, boolean success, String message, String version) {
        _num_current_tasks = 0;
        _current_tasks.clear();

        if (success) {
            // If we've succeeded then we can write our latest version to the place
            ZoteroSummary s = _zotdroid_db.getSummary();
            s.set_last_version(version);
            _zotdroid_db.writeSummary(s);
        }
        // Call this anyway - it's probably for the best at this point
        this.populateFromDB();
        _midnightcaller.onSyncFinish(success,message);
    }
}
