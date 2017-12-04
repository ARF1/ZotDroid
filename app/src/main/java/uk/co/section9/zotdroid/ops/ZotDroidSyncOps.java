package uk.co.section9.zotdroid.ops;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import java.util.Vector;

import uk.co.section9.zotdroid.Constants;
import uk.co.section9.zotdroid.ZotDroidMem;
import uk.co.section9.zotdroid.data.ZotDroidDB;
import uk.co.section9.zotdroid.data.zotero.Attachment;
import uk.co.section9.zotdroid.data.zotero.Collection;
import uk.co.section9.zotdroid.data.zotero.CollectionItem;
import uk.co.section9.zotdroid.data.zotero.Note;
import uk.co.section9.zotdroid.data.zotero.Record;
import uk.co.section9.zotdroid.data.zotero.Summary;
import uk.co.section9.zotdroid.task.ZotDroidSyncCaller;
import uk.co.section9.zotdroid.task.ZoteroCollectionsTask;
import uk.co.section9.zotdroid.task.ZoteroDelTask;
import uk.co.section9.zotdroid.task.ZoteroItemsTask;
import uk.co.section9.zotdroid.task.ZoteroPushItemsTask;
import uk.co.section9.zotdroid.task.ZoteroSyncColTask;
import uk.co.section9.zotdroid.task.ZoteroSyncItemsTask;
import uk.co.section9.zotdroid.task.ZoteroTask;
import uk.co.section9.zotdroid.task.ZoteroTaskCallback;
import uk.co.section9.zotdroid.task.ZoteroVerColTask;
import uk.co.section9.zotdroid.task.ZoteroVerItemsTask;

/**
 * Created by oni on 15/11/2017.
 */

public class ZotDroidSyncOps extends ZotDroidOps implements ZoteroTaskCallback  {

    private ZotDroidSyncCaller      _midnightcaller;

    public static final String TAG = "ZotDroidSyncOps";

    public ZotDroidSyncOps(ZotDroidDB zotdroid_db, Activity activity, ZotDroidMem mem, ZotDroidSyncCaller midnightcaller) {
        super(activity, zotdroid_db, mem);
        _midnightcaller = midnightcaller;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    /**
     * Perform a sync with Zotero, grabbing all the items
     * We start with collections as records depend on them
     * When collections completes, we'll start on items
     * This method nukes everything and rebuilds from scratch
     */
    public void resetAndSync() {
        _zotdroid_db.reset();
        _zotdroid_mem.nukeMemory();
        _current_tasks.add(new ZoteroCollectionsTask(this,0,25));
        _current_tasks.add(new ZoteroItemsTask(this,0,25));
        nextTask();
    }

    /**
     * A standard sync where we do things a bit more intelligent like
     * We start with the collections, then move onto records / items
     * Returns true if syncing is possible, OR false if a full reset and
     * sync is needed (i.e the DB is empty)
     */

    public boolean sync(){
        Summary s = _zotdroid_db.getSummary();
        if (s.get_last_version() == "0000"){ return false;}
        ZoteroSyncColTask zs = new ZoteroSyncColTask(this, s.get_last_version());
        _current_tasks.add(zs);
        ZoteroSyncItemsTask zt = new ZoteroSyncItemsTask(this,s.get_last_version());
        _current_tasks.add(zt);

        Vector<Record> changed_records = new Vector<>();
        // TODO - Can only do a max of 50 here
        for (Record r : _zotdroid_mem._records) {
            if (!r.is_synced()) {
                changed_records.add(r);
            }
        }
        if (changed_records.size() > 0) {
            ZoteroPushItemsTask zp = new ZoteroPushItemsTask(this, changed_records, s.get_last_version());
            _current_tasks.add(zp);
        }

        nextTask();
        return true;
    }

    /**
     * Given a record, do we add it anew or alter an existing?
     */
    private void checkUpdateRecord(Record record) {
        if (!_zotdroid_db.recordExists(record)) {
            _zotdroid_db.writeRecord(record);
            collectionItemsCreate(record); // This is why collections MUST be synced first
        } else {
            // Check the version numbers if this exists and update as necessary
            Record existing = _zotdroid_db.getRecord(record.get_zotero_key());
            if (existing != null) {
                if (Integer.valueOf(existing.get_version()) < Integer.valueOf(record.get_version())) {
                    // Perform an update :)
                    _zotdroid_db.updateRecord(record);
                    record.set_synced(true);
                    // At this point the record will likely have different collections too
                    // so we simply rebuild them from our new fresh record
                    _zotdroid_db.removeRecordFromCollections(record);
                    collectionItemsCreate(record);
                }
            }
            // Also update in memory now that we have that too
            // Not sure we need to do this but perhaps
            /*for (Record r : _zotdroid_mem._records){
                if (r.get_zotero_key().equals(record.get_zotero_key())){
                    r.copyFrom(record);
                }
            }*/
        }
    }

    /**
     * Given an attachment, do we add it anew or alter an existing?
     * @param attachment
     */
    private void checkUpdateAttachment(Attachment attachment) {
        if (!_zotdroid_db.attachmentExists(attachment)) {
            _zotdroid_db.writeAttachment(attachment);
        } else {
            // Check the version numbers if this exists and update as necessary
            Attachment existing = _zotdroid_db.getAttachment(attachment.get_zotero_key());
            if (existing != null) {
                if (Integer.valueOf(existing.get_version()) < Integer.valueOf(attachment.get_version())) {
                    // Perform an update :)
                    _zotdroid_db.updateAttachment(attachment);
                }
            }
        }
    }

    /**
     * Given an attachment, do we add it anew or alter an existing?
     * @param note
     */
    private void checkUpdateNote(Note note) {
        if (!_zotdroid_db.noteExists(note)) {
            _zotdroid_db.writeNote(note);
        } else {
            // Check the version numbers if this exists and update as necessary
            Note existing = _zotdroid_db.getNote(note.get_zotero_key());
            if (existing != null) {
                if (Integer.valueOf(existing.get_version()) < Integer.valueOf(note.get_version())) {
                    // Perform an update :)
                    _zotdroid_db.updateNote(note);
                }
            }
        }
    }

    /**
     * Given a collection, do we add it anew or alter existing?
     * @param collection
     */
    private void checkUpdateCollection(Collection collection) {
        if (!_zotdroid_db.collectionExists(collection)) {
            _zotdroid_db.writeCollection(collection);
        } else {
            // Check the version numbers if this exists and update as necessary
            Collection existing = _zotdroid_db.getCollection(collection.get_zotero_key());
            if (existing != null) {
                if (Integer.valueOf(existing.get_version()) < Integer.valueOf(collection.get_version())) {
                    // Perform an update. This is tricky because we could essentially change a collection
                    // and move an object :/
                    // For now we just update the values but we should check whether or not this collection is in the trash
                    _zotdroid_db.updateCollection(collection);

                }
            }
        }
    }

    /**
     * This is called when an item is completed, specifically on a sync task (i.e not a reset and sync task)
     * @param success
     * @param message
     * @param records
     * @param attachments
     * @param version
     */

    @Override
    public void onItemCompletion(boolean success, String message, Vector<Record> records,
                                 Vector<Attachment> attachments, Vector<Note> notes, String version) {
        _midnightcaller.onSyncProgress( (float)(_num_current_tasks  - _current_tasks.size()) / (float)_num_current_tasks);
        if (success) {
            for (Record record : records){ checkUpdateRecord(record); }
            for (Attachment attachment : attachments){ checkUpdateAttachment(attachment); }
            for (Note note : notes) {checkUpdateNote(note); }
            if (!nextTask()) { onItemsCompletion(true, message, version); }
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
    public void onItemCompletion(boolean success, String message, int new_index,
                                 int total, Vector<Record> records,
                                 Vector<Attachment> attachments,
                                 Vector<Note> notes, String version) {
        String status_message = "";
        if (success) {
            for (Record record : records){ checkUpdateRecord(record); }
            for (Attachment attachment : attachments) { checkUpdateAttachment(attachment);}
            for (Note note : notes) { checkUpdateNote(note);}

            _midnightcaller.onSyncProgress((float)new_index / (float)total);
            // We fire off another task from here if success and we have more to go
            if (new_index + 1 <= total){
                ZoteroTask t = new ZoteroItemsTask(this, new_index ,25);
                _current_tasks.add(0,t);
                nextTask();
            } else {
                onItemsCompletion(success,message, version);
                return;
            }

        } else {
            //Log.e(TAG,"Error returned in onItemCompletion");
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
    public void onItemsCompletion( boolean success, String message, String version) {
        if (success) {
            Summary s = _zotdroid_db.getSummary();
            //Log.i(TAG,"Items Complete Version: " + version);
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
     * @param success
     * @param message
     * @param version
     */

    public void onCollectionsCompletion( boolean success, String message, String version) {
        if (success) {
            Summary s = _zotdroid_db.getSummary();
            //Log.i(TAG,"Collections Complete Version: " + version);
            nextTask();
        } else {
            stop();
            _midnightcaller.onSyncFinish(false, message);
        }
    }

    /**
     * Called when a single collection has been completed
     * @param success
     * @param message
     * @param collections
     * @param version
     */

    public void onCollectionCompletion(boolean success, String message, Vector<Collection> collections, String version) {
        if (success) {
            for (Collection collection : collections){
                checkUpdateCollection(collection);
            }
            if (!nextTask()) { onCollectionsCompletion(true, message, version); }

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
     * @param success
     * @param message
     * @param collections
     */

    public void onCollectionCompletion(boolean success, String message, int new_index,
                                       int total, Vector<Collection> collections, String version) {
        if (success) {
            for (Collection collection : collections){
                if (!_zotdroid_db.collectionExists(collection)) {
                    _zotdroid_db.writeCollection(collection);
                } else {
                    // Check the version numbers if this exists and update as necessary
                    Collection existing = _zotdroid_db.getCollection(collection.get_zotero_key());
                    if (existing != null) {
                        if (Integer.valueOf(existing.get_version()) < Integer.valueOf(collection.get_version())) {
                            // Perform an update :)
                        }
                    }
                }
            }

            // We fire off another task from here if success and we have more to go
            if (new_index + 1 <= total){
                ZoteroTask t = new ZoteroCollectionsTask(this, new_index ,25);
                _current_tasks.add(0,t);
                nextTask();

            } else {
                onCollectionsCompletion( success ,message, version);
                return;
            }
        } else {
            stop();
            _midnightcaller.onSyncFinish(success, message);
        }
    }

    /**
     * We now have a list of things that have changed
     * @param success
     * @param message
     */
    public void onItemVersion( boolean success, String message, Vector<String> items, String version){

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
            onItemsCompletion(success,message,version);
        }
    }

    /**
     * We now have a list of collections that have changed
     * @param success
     * @param message
     */
    public void onCollectionVersion(boolean success, String message, Vector<String> items, String version){

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
            onCollectionsCompletion(true, message, version);
        }
    }

    /**
     * Called when our task to find items to delete returns
     * @param success
     * @param message
     * @param items
     * @param collections
     * @param version
     */
    @Override
    public void onSyncDelete(boolean success, String message, Vector<String> items, Vector<String> collections, String version) {
        //Log.i(TAG,"Collections to delete: " + collections.size());
        //Log.i(TAG,"Items to delete: " + items.size());

        for (String key : items){
            Record r = _zotdroid_db.getRecord(key);
            if (r != null){ _zotdroid_db.deleteRecord(r); }
            Attachment a = _zotdroid_db.getAttachment(key);
            // Could be either but it wont fail if we get the wrong one
            if (a != null ) {_zotdroid_db.deleteAttachment(a);}
        }

        for (String key : collections){
            Collection c = _zotdroid_db.getCollection(key);
            if (c != null ){ _zotdroid_db.deleteCollection(c);}
        }
        if (!nextTask()) { onSyncCompletion(true,"Sync completed", version); }
    }

    /**
     * Callback from the first step of syncing, where we check the versions we have against the server
     * @param success
     * @param message
     * @param version
     */
    @Override
    public void onSyncCollectionsVersion( boolean success, String message, String version) {

        if (success) {
            //Log.i(TAG,"Current Sync Collections: " + getVersion() + " New Version: " + version);
            // Start with collections sync and then items afterwards
            if (getVersion() != version) {
                ZoteroVerColTask zc = new ZoteroVerColTask(this, getVersion());
                _current_tasks.add(0,zc);
                nextTask();
            } else {
                onCollectionsCompletion(true,"Collections are up-to-date.",getVersion());
            }
        } else { stop(); onSyncCompletion(false, message, version); }
    }

    /**
     * Called when we have a list of items to sync
     * @param success
     * @param message
     * @param version
     */
    @Override
    public void onSyncItemsVersion(boolean success, String message, String version) {
        if (success) {
            //Log.i(TAG,"Current Sync Items: " + getVersion() + " New Version: " + version);
            // Now we move onto the records if we need to
            if (getVersion() != version){
                ZoteroVerItemsTask zv = new ZoteroVerItemsTask(this, getVersion());
                _current_tasks.add(0,zv);
                nextTask();
            } else {
                // We dont need to sync records so we are done (aside from deletion)
                onSyncCompletion(true, "Sync completed", version);
            }
        } else { stop(); onSyncCompletion(false, message, version); }
    }

    /**
     * Once we have both collections and items, we need to properly link them up
     * We create the CollectionItems entries in the database for use later
     */

    private void collectionItemsCreate(Record record) {
        for (String ts : record.get_temp_collections()){
            CollectionItem ci = new CollectionItem();
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
     * @param success
     * @param message
     * @param version
     */
    public void onSyncCompletion(boolean success, String message, String version) {
        _num_current_tasks = 0;
        _current_tasks.clear();

        if (success) {
            // If we've succeeded then we can write our latest version to the place
            Summary s = _zotdroid_db.getSummary();
            s.set_last_version(version);
            _zotdroid_db.writeSummary(s);
        }

        populateFromDB(Constants.PAGINATION_SIZE);
        _midnightcaller.onSyncFinish(success,message);
    }

    /**
     * Called when we have finished pushing changes to Zotero
     * TODO - we could update or check the DB here as well - possible send back these records
     * that succeeded and failed?
     * @param success
     * @param message
     * @param version
     */
    @Override
    public void onPushItemsCompletion(boolean success, String message, String version) {
        if (success) {
            if (!nextTask()) { onSyncCompletion(true,"Sync completed", version); }
        }
        onSyncCompletion(false,message, version);
    }

}
