package uk.co.section9.zotdroid.data.zotero;

/**
 * Created by oni on 21/07/2017.
 */

import java.util.Vector;

/**
 * Created by oni on 14/07/2017.
 */

public class Collection {

    public static final String TAG = "zotdroid.data.Collection";

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

    public void add_collection(Collection c) { _sub_collections.add(c); }

    public Vector<Collection> get_sub_collections() { return _sub_collections;}

    public String get_version() {
        return _version;
    }

    public void set_version(String _version) {
        this._version = _version;
    }

    public Vector<Record> get_records() {return _records;}

    public void add_record(Record r) {_records.add(r); }

    public boolean has_parent() {
        boolean r = true;
        if (_parent.isEmpty()) { r = false; }
        if (_parent.contains("false")) { r = false; }
        return r;
    }

    // For now, we cover all the bases we need for all possible items
    // Eventually we might have separate record tables

    protected String    _title;
    protected String    _zotero_key;
    protected String    _parent;
    protected String    _version;
    protected Vector<Collection> _sub_collections; // In-efficient in memory terms but whatever
    protected Vector<Record> _records;

    public String toString() {
        return _title;
    }
    public Collection(){
        _sub_collections = new Vector<Collection>();
        _records = new Vector<Record>();
        _version = "0000";
        _parent = ""; // Default no parent
    }
}