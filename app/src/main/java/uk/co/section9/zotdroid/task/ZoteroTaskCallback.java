package uk.co.section9.zotdroid.task;

/**
 * Created by oni on 21/07/2017.
 */

import java.util.Vector;

import uk.co.section9.zotdroid.data.zotero.Attachment;
import uk.co.section9.zotdroid.data.zotero.Collection;
import uk.co.section9.zotdroid.data.zotero.Note;
import uk.co.section9.zotdroid.data.zotero.Record;

/**
 * Special callback for the Items once all items have completed
 */
public interface ZoteroTaskCallback {
    // Called when items tasks finish (either one batch or all batches)
    void onItemsCompletion(boolean success, String message, String version);
    void onItemCompletion(boolean success, String message, int new_index, int total, Vector<Record> records, Vector<Attachment> attachments, Vector<Note> notes, String version);
    void onItemCompletion(boolean success, String message, Vector<Record> records, Vector<Attachment> attachments, Vector<Note> notes, String version);

    // Called when collections tasks finish (either one batch, or all batches)
    void onCollectionsCompletion(boolean success, String message, String version);
    void onCollectionCompletion(boolean success, String message, int new_index, int total, Vector<Collection> collections, String version);
    void onCollectionCompletion(boolean success, String message, Vector<Collection> collections, String version);

    // Called when we get the latest version number back from the server
    void onItemVersion(boolean success, String message, Vector<String> items, String version);
    void onCollectionVersion(boolean success, String message, Vector<String> collections, String version);

    // Called when the various sync tasks have fully completed
    void onSyncDelete (boolean success, String message, Vector<String> items, Vector<String> collections, String version);
    void onSyncItemsVersion (boolean success, String message, String version);
    void onSyncCollectionsVersion (boolean success, String message, String version);

    // Called when all of the above sync tasks are done
    void onSyncCompletion(boolean success, String message, String version);

    // Called when we have pushed back to the server
    void onPushItemsCompletion(boolean success, String message, String version);

}