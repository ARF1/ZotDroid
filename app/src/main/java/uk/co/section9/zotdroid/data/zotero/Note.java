package uk.co.section9.zotdroid.data.zotero;

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

    protected String    _record_key;
    protected String    _zotero_key;
    protected String    _note;

    public String toString() { return _note; }

    public Note(String zotero_key, String record_key, String note) {
        _zotero_key = zotero_key;
        _record_key = record_key;
        _note = note;
    }
}
