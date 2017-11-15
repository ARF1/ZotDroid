package uk.co.section9.zotdroid.data.zotero;

import java.util.Date;
import java.util.Vector;

/**
 * Created by oni on 14/07/2017.
 */

public class Record {

    public static final String TAG = "zotdroid.data.Record";

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
        _author = author;
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

    public void addAttachment(Attachment attachment) { _attachments.add(attachment); }

    public void addCollection(Collection c) { _collections.add(c);}

    public Vector<String> get_temp_collections() { return _temp_collections; }

    public void addTempCollection(String s) { _temp_collections.add(s);}
    public boolean inCollection(Collection c) { return  _collections.contains(c); }

    public Vector<Attachment> get_attachments() {return _attachments;}

    public Vector<String> get_tags() {return _tags;}

    public void add_tag(String tag) { _tags.add(tag); }

    public String get_version() {
        return _version;
    }

    public void set_version(String _version) {
        this._version = _version;
    }

    public boolean search(String term) {
        String tt = term.toLowerCase();
        if (_author.toLowerCase().contains(tt)) {return true;}
        if (_title.toLowerCase().contains(tt)) {return true;}
        return false;
    }

    // For now, we cover all the bases we need for all possible items
    // Eventually we might have separate record tables

    // TODO - do we really need _item_type and _content_type? Is it not implicit in the class?
    protected String    _content_type;
    protected String    _title;
    protected String    _item_type;
    protected Date      _date_added;
    protected Date      _date_modified;
    protected String    _version;
    protected String    _author; // TODO - Just one for now but we will add more
    protected String    _zotero_key;
    protected String    _parent;
    protected Vector<Attachment> _attachments;
    protected Vector<Collection> _collections;
    protected Vector<String>    _temp_collections;
    protected Vector<String>    _tags;

    public String toString() {
        return _title + " - " + _author;
    }

    public Record(){
        _date_added = new Date();
        _date_modified = new Date();
        _attachments = new Vector<Attachment>();
        _collections = new Vector<Collection>();
        _temp_collections = new Vector<String>(); // TODO - temporarily holding collection keys might not be the best way
        _tags = new Vector<String>(); // TODO - temporarily holding collection keys might not be the best way

    }
}