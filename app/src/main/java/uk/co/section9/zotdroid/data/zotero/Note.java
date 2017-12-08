package uk.co.section9.zotdroid.data.zotero;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by oni on 03/11/2017.
 */

public class Note {
    public static final String TAG = "zotdroid.data.Note";

    public String get_zotero_key() {
        return _zotero_key;
    }
    public void set_zotero_key(String key) {
        this._zotero_key = key;
    }

    public String get_record_key() {
        return _record_key;
    }

    public void set_record_key(String _record_key) {
        this._record_key = _record_key;
    }

    public String get_note() {
        return _note;
    }

    public void set_note(String _note) {
        this._note = _note;
    }

    public String get_version() {
        return _version;
    }

    public void set_version(String _version) {
        this._version = _version;
    }

    public boolean is_synced() { return _synced; }

    public void set_synced(boolean _synced) { this._synced = _synced;}

    protected String    _record_key;
    protected String    _zotero_key;
    protected String    _note;
    protected String    _version;
    protected boolean   _synced;

    public String toString() { return _note; }

    public Note(String zotero_key, String record_key, String note, String version) {
        _zotero_key = zotero_key;
        _record_key = record_key;
        _version = version;
        _note = note;
        _synced = true;
    }

    public JSONObject to_json() {
        JSONObject jobj = new JSONObject();
        try {
            jobj.put("key",_zotero_key);
            jobj.put("version", _version);
            jobj.put("itemType", "note");
            jobj.put("parentItem", _record_key);
            jobj.put("note",_note);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jobj;
    }

}
