package uk.co.section9.zotdroid.data;

import java.util.Date;
import java.util.Vector;

/**
 * Created by oni on 14/07/2017.
 */

public class ZoteroRecord {

    public static final String TAG = "zotdroid.data.RecordsTable.ZoteroRecord";

    public String get_zotero_key() {
        return _zotero_key;
    }

    public void set_zotero_key(String key) {
        this._zotero_key = key;
    }

    public String get_author() {
        return _author;
    }

    public void set_author(String author) {
        this._author = _author;
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

    public void set_date_added(Date date_added) {
        this._date_added = date_added;
    }

    public void addAttachment(ZoteroAttachment attachment) { _attachments.add(attachment); }

    public Vector<ZoteroAttachment> get_attachments() {return _attachments;}

    // For now, we cover all the bases we need for all possible items
    // Eventually we might have separate record tables

    protected String    _content_type;
    protected String    _title;
    protected String    _item_type;
    protected Date      _date_added;
    protected String    _author; // TODO - Just one for now but we will add more
    protected String    _zotero_key;
    protected String    _parent;
    protected Vector<ZoteroAttachment> _attachments;
    public String toString() {
        return _title + " - " + _author;
    }

    public ZoteroRecord(){
        _date_added = new Date();
        _attachments = new Vector<ZoteroAttachment>();
    }
}