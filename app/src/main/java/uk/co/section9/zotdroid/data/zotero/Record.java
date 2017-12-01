package uk.co.section9.zotdroid.data.zotero;

//import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    public Vector<Author> get_authors() {
        return _authors;
    }

    public void add_author(Author author) {
        _authors.add(author);
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

    /*public Date get_date_added() { return _date_added; }

    public void set_date_added(Date date_added) { this._date_added = date_added; }

    public Date get_date_modified() { return _date_modified; }

    public void set_date_modified(Date date_added) { this._date_modified = date_added;} */

    public String get_date_added() {
        return _date_added;
    }

    public void set_date_added(String date_added) { this._date_added = date_added; }

    public String get_date_modified() {
        return _date_modified;
    }

    public void set_date_modified(String date_added) {
        this._date_modified = date_added;
    }

    public void addAttachment(Attachment attachment) { _attachments.add(attachment); }

    public void addCollection(Collection c) { _collections.add(c);}

    public Vector<String> get_temp_collections() { return _temp_collections; }

    public void addTempCollection(String s) { _temp_collections.add(s);}
    public boolean inCollection(Collection c) { return  _collections.contains(c); }

    public Vector<Attachment> get_attachments() {return _attachments;}

    public Vector<Tag> get_tags() {return _tags;}

    public boolean add_tag(Tag tag) {
        for (Tag t : _tags){
            if (t.get_name() == tag.get_name()) { return false; }
        }
        _tags.add(tag);
        return true;
    }

    public void remove_tag(Tag tag) {
        for (int i = 0; i < _tags.size(); i++) {
            if (_tags.get(i).get_record_key() == tag.get_record_key() &&
                    _tags.get(i).get_name() == tag.get_name()) {
                _tags.remove(i);
            }
        }
    }

    public Vector<Note> get_notes() {return _notes;}

    public void add_note(Note note) { _notes.add(note); }

    public void remove_note(Note note) {
        for (int i = 0; i < _notes.size(); i++) {
            if (_notes.get(i).get_record_key() == note.get_record_key() &&
                    _notes.get(i).get_zotero_key() == note.get_zotero_key()) {
                _notes.remove(i);
            }
        }
    }

    public boolean is_synced() {
        return _synced;
    }

    public void set_synced(boolean _synced) {
        this._synced = _synced;
    }

    public String get_version() {
        return _version;
    }

    public void set_version(String _version) {
        this._version = _version;
    }

    /**
     * Search the title, authors and tags for this particular record
     * @param term
     * @return
     */
    public boolean search(String term) {
        String tt = term.toLowerCase();
        for (Author author : _authors) {
            if (author._name.toLowerCase().contains(tt)) {
                return true;
            }
        }
        if (_title.toLowerCase().contains(tt)) {return true;}

        for (Tag tag : _tags) {
            if (tag._name.toLowerCase().contains(tt)) {
                return true;
            }
        }

        return false;
    }

    public void copyFrom(Record r){
        _content_type = r._content_type;
        _title = r._title;
        _item_type = r._item_type;
        _date_added = r._date_added;
        _date_modified = r._date_modified;
        _version = r._version;
        _zotero_key = r._zotero_key;
        _parent = r._parent;
        _attachments.clear();
        _attachments.addAll(r._attachments);
        _collections.clear();
        _collections.addAll(r._collections);
        _temp_collections.clear();
        _temp_collections.addAll(r._temp_collections);
        _tags.clear();
        _tags.addAll(r._tags);
        _authors.clear();
        _authors.addAll(r._authors);
        _synced = r._synced;
    }

    // TODO - this is used only for updates at the moment - so we ONLY return these
    // things we are allowing the user to change on the server from this program
    public JSONObject to_json() {
        JSONObject jobj = new JSONObject();

        try {
            jobj.put("key",_zotero_key);
            jobj.put("version", _version);
            JSONArray jtags = new JSONArray();

            for (Tag t : _tags){
                JSONObject jtag = new JSONObject();
                jtag.put("tag", t.get_name());
                jtags.put(jtag);
            }

            jobj.put("tags",jtags);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jobj;
    }

    // For now, we cover all the bases we need for all possible items
    // Eventually we might have separate record tables

    // TODO - do we really need _item_type and _content_type? Is it not implicit in the class?
    protected String    _content_type;
    protected String    _title;
    protected String    _item_type;
    // TODO - decided to change from Date to string as Date conversion takes for ages! ><
    protected String      _date_added;
    protected String      _date_modified;
    //protected Date      _date_added;
    //protected Date      _date_modified;
    protected String    _version;
    protected String    _zotero_key;
    protected String    _parent;
    protected Vector<Attachment> _attachments;
    protected Vector<Collection> _collections;
    protected Vector<String>    _temp_collections;
    protected Vector<Tag>       _tags;
    protected Vector<Note>      _notes;
    protected Vector<Author>    _authors;
    protected boolean           _synced;

    public String toString() {
        return _title + " - " + _authors.firstElement();
    }

    public Record(){
        _authors = new Vector<Author>();
        _date_added = "no date";
        _date_modified = "no date";
        _synced = false;
        _version = "0000";
        _attachments = new Vector<Attachment>();
        _collections = new Vector<Collection>();
        _temp_collections = new Vector<String>(); // TODO - temporarily holding collection keys might not be the best way
        _tags = new Vector<Tag>(); // TODO - temporarily holding collection keys might not be the best way
        _notes = new Vector<Note>(); // TODO - temporarily holding collection keys might not be the best way

    }
}