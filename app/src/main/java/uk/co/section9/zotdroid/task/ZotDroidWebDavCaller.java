package uk.co.section9.zotdroid.task;

/**
 * Created by oni on 15/11/2017.
 */

public interface ZotDroidWebDavCaller {
    void onDownloadProgress(float progress);
    void onDownloadFinish(boolean success, String message, String type);
    // Called when WebDav Test completes
    void onWebDavTestFinish(boolean success, String message);
}
