package uk.co.section9.zotdroid.data;

/**
 * Created by oni on 21/07/2017.
 */

import java.util.Vector;

/**
 * Created by oni on 14/07/2017.
 */

public class ZoteroCollection {

    public static final String TAG = "zotdroid.data.RecordsTable.ZoteroCollection";

    public String get_zotero_key() {
        return _zotero_key;
    }

    public void set_zotero_key(String key) {
        this._zotero_key = key;
    }

    public String get_title() {
        return _title;
    }

    public void set_title(String title) { this._title = title;}

    public String get_parent() {
        return _parent;
    }

    public void set_parent(String parent) {
        this._parent = parent;
    }


    // For now, we cover all the bases we need for all possible items
    // Eventually we might have separate record tables

    protected String    _title;
    protected String    _zotero_key;
    protected String    _parent;
    protected Vector<ZoteroCollection> _sub_collections; // In-efficient in memory terms but whatever
    public String toString() {
        return _title;
    }

    public ZoteroCollection(){
        _sub_collections = new Vector<ZoteroCollection>();
    }
}