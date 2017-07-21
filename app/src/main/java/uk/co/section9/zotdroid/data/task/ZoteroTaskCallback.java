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
    void onItemsCompletion(ZoteroTask task, boolean success, String message);
    void onItemCompletion(ZoteroTask task, boolean success, String message, int new_index, int total, Vector<ZoteroRecord> records, Vector<ZoteroAttachment> attachments);
    void onCollectionsCompletion(ZoteroTask task, boolean success, String message, Vector<ZoteroCollection> collections);
}