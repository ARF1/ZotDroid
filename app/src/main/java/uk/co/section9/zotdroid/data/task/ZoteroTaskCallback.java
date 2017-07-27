package uk.co.section9.zotdroid.data.task;

/**
 * Created by oni on 21/07/2017.
 */

import java.util.Vector;

import uk.co.section9.zotdroid.data.ZoteroAttachment;
import uk.co.section9.zotdroid.data.ZoteroCollection;
import uk.co.section9.zotdroid.data.ZoteroRecord;

/**
 * Special callback for the Items once all items have completed
 */
public interface ZoteroTaskCallback {
    void onItemsCompletion(ZoteroTask task, boolean success, String message, String version);

    void onItemCompletion(ZoteroTask task, boolean success, String message, int new_index, int total, Vector<ZoteroRecord> records, Vector<ZoteroAttachment> attachments, String version);
    void onItemCompletion(ZoteroTask task, boolean success, String message, Vector<ZoteroRecord> records, Vector<ZoteroAttachment> attachments, String version);

    void onCollectionsCompletion(ZoteroTask task, boolean success, String message, String version);
    void onCollectionCompletion(ZoteroTask task, boolean success, String message, int new_index, int total, Vector<ZoteroCollection> collections, String version);
    void onCollectionCompletion(ZoteroTask task, boolean success, String message, Vector<ZoteroCollection> collections, String version);

    void onItemVersion(ZoteroTask task, boolean success, String message, Vector<String> items);
    void onCollectionVersion(ZoteroTask task, boolean success, String message, Vector<String> collections);

    void onSyncDelete (ZoteroTask task, boolean success, String message, Vector<String> items, Vector<String> collections);

    void onSyncVersion (ZoteroTask task, boolean success, String message, String records_version, String collections_version);

    void onSyncCompletion(ZoteroTask task, boolean success, String message);
}