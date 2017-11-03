package uk.co.section9.zotdroid.data;

import java.util.Date;

/**
 * Created by oni on 03/11/2017.
 */

public class ZoteroNote {
    public static final String TAG = "zotdroid.data.ZoteroNote";

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

    public String get_content_type() {
        return _content_type;
    }

    public void set_content_type(String content_type) {
        this._content_type = content_type;
    }

    public String get_item_type() {
        return _item_type;
    }

    public void set_item_type(String item_type) {
        this._item_type = item_type;
    }

    public Date get_date_added() {
        return _date_added;
    }

    public void set_date_added(Date date_added) { this._date_added = date_added; }

    public Date get_date_modified() {
        return _date_modified;
    }

    public void set_date_modified(Date date_added) {
        this._date_modified = date_added;
    }


    protected String    _content_type;
    protected String    _title;
    protected String    _item_type;
    protected Date      _date_added;
    protected Date      _date_modified;
    protected String    _zotero_key;
    protected String    _parent;
    protected String    _note;

    public String toString() { return _note; }

    public ZoteroNote() {
        _date_added = new Date();
        _date_modified = new Date();
    }
}
