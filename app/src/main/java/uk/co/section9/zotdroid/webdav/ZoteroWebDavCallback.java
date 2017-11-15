package uk.co.section9.zotdroid.webdav;

/**
 * Created by oni on 15/11/2017.
 */

public interface ZoteroWebDavCallback {
    void onWebDavProgess(boolean result, String message);
    void onWebDavComplete(boolean result, String message);
}
