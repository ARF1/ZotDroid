package uk.co.section9.zotdroid.task;

/**
 * Created by oni on 15/11/2017.
 */

public interface ZotDroidSyncCaller {
    void onSyncProgress(float progress);
    void onSyncFinish(boolean success, String message);
}
