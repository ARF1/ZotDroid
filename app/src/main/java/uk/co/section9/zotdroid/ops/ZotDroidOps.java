package uk.co.section9.zotdroid.ops;

/**
 * Created by oni on 15/11/2017.
 */

import android.app.Activity;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import uk.co.section9.zotdroid.ZotDroidMem;
import uk.co.section9.zotdroid.data.ZotDroidDB;
import uk.co.section9.zotdroid.data.zotero.Attachment;
import uk.co.section9.zotdroid.data.zotero.Collection;
import uk.co.section9.zotdroid.data.zotero.CollectionItem;
import uk.co.section9.zotdroid.data.zotero.Note;
import uk.co.section9.zotdroid.data.zotero.Record;
import uk.co.section9.zotdroid.task.ZoteroTask;

/**
 * A class that deals in operations. It is abstract and is inherited by Sync and User ops
 */

public class ZotDroidOps {
    protected Activity              _activity;
    protected Vector<ZoteroTask>    _current_tasks;
    protected int                   _num_current_tasks = 0;
    protected ZotDroidDB            _zotdroid_db;
    protected ZotDroidMem           _zotdroid_mem;

    public ZotDroidOps(Activity activity, ZotDroidDB db, ZotDroidMem mem) {
        _activity = activity;
        _zotdroid_db = db;
        _current_tasks = new Vector<ZoteroTask>();
        _zotdroid_mem = mem;
    }

    /**
     * Remove the finished task, and start the next in the queue, returning true
     * Returns false if this was the last task.
     * @return
     */
    protected boolean nextTask() {
        if (_current_tasks.isEmpty()) { return false; }
        _current_tasks.get(0).startZoteroTask();
        _current_tasks.remove(0);
        return true;
    }

    /**
     * Stop the current task and clear all remaining tasks.
     * Also cancel any download operations
     */
    public void stop() {
        for (ZoteroTask t : _current_tasks){ t.cancel(true); }
        _current_tasks.clear();
        _num_current_tasks = 0;
    }

    /**
     * Everything starts with a set of records derived somehow. From these records we can rebuild
     * all our datastructures in memory again, from the database of all our zotero collection
     * @param records
     */

    protected void rebuildMemory(Vector<Record> records) {

        // Now add trv to our working memory - it always starts with records
        for (Record record : records) {
            if (!_zotdroid_mem._key_to_record.containsKey(record.get_zotero_key())) {
                _zotdroid_mem._key_to_record.put(record.get_zotero_key(), record);
                _zotdroid_mem._records.add(record);
            }
        }

        // Move on to any new attachments & notes for our new records
        for (Record record : records) {
            Vector<Attachment> za = _zotdroid_db.getAttachmentsForRecord(record);
            for (Attachment attachment : za){
                record = _zotdroid_mem._key_to_record.get(attachment.get_parent());
                if (record != null) {
                    record.addAttachment(attachment);
                    _zotdroid_mem._attachments.add(attachment);
                }
            }

            Vector<Note> zb = _zotdroid_db.getNotesForRecord(record);
            for (Note note : zb){
                record = _zotdroid_mem._key_to_record.get(note.get_record_key());
                if (record != null) {
                    record.add_note(note);
                    _zotdroid_mem._notes.add(note);
                }
            }
        }

        // Now go with collections
        // We don't paginate collections at this point
        // Most of the time, this will not be required as all the collections will be in place
        // but for now it makes sense to have it here.
        int numrows = _zotdroid_db.getNumCollections();

        Vector<Collection> newcollections = new Vector<>();

        for (int i=0; i < numrows; ++i) {
            Collection collection = _zotdroid_db.getCollection(i);
            if(!_zotdroid_mem._key_to_collection.containsKey(collection.get_zotero_key())) {
                _zotdroid_mem._collections.add(collection);
                newcollections.add(collection);
                _zotdroid_mem._key_to_collection.put(collection.get_zotero_key(), collection);
            }
        }

        // Sort collection via title each time - consistent for the user
        Collections.sort(_zotdroid_mem._collections, new Comparator<Collection>() {
            @Override
            public int compare(Collection c0, Collection c1) {
                return c0.get_title().compareTo(c1.get_title());
            }
        });

        // Now link each collection to it's parent. Memory duplication I fear :/
        for (Collection c : newcollections){
            Collection zp = _zotdroid_mem._key_to_collection.get(c.get_parent());
            if (zp != null){
                zp.add_collection(c);
            }
        }

        // This bit could be slow if there are loads of collections. There will be a faster
        // way to do it I would say.
        // Add the new records to the collections they belong to.
        numrows = _zotdroid_db.getNumCollectionsItems();

        for (int i=0; i < numrows; ++i) {
            CollectionItem ct = _zotdroid_db.getCollectionItem(i);

            for (Collection c : newcollections){
                if (ct.get_collection().contains(c.get_zotero_key())){
                    for (Record r : records){
                        if ( ct.get_item().contains(r.get_zotero_key())){
                            r.addCollection(c);
                            c.add_record(r);
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }


    /**
     * Get a subset of the records in the database, finding any that are new. Then rebuild
     * our memory so it reflects this window
     */

    public boolean populateFromDB(int end) {
        // Start with any new records we want to add into memory
        int numrows = _zotdroid_db.getNumRecords();
        if ( end < 0) { return false; }
        if (end >= numrows) { end = numrows-1; }
        _zotdroid_mem._end_index = end;
        Vector<Record> newrecords = new Vector<>();

        for (int i= /*_zotdroid_mem._start_index*/ 0; i < end; ++i){ // start is always 0 for now
            Record record = _zotdroid_db.getRecord(i);
            if (!_zotdroid_mem._key_to_record.containsKey(record.get_zotero_key())) {
                _zotdroid_mem._key_to_record.put(record.get_zotero_key(), record);
                _zotdroid_mem._records.add(record);
                newrecords.add(record);
            }
        }

        rebuildMemory(newrecords);
        return true;
    }

}
