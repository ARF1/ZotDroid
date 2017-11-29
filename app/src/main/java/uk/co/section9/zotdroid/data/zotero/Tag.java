package uk.co.section9.zotdroid.data.zotero;

/**
 * Created by oni on 28/11/2017.
 */

public class Tag {
    public static final String TAG = "zotdroid.data.Tag";

    public String get_name() {
        return _name;
    }

    public void set_name(String _name) {
        this._name = _name;
    }

    public String get_record_key() {
        return _record_key;
    }

    public void set_record_key(String _record_key) {
        this._record_key = _record_key;
    }

    protected String _name;
    protected String _record_key;

    public Tag (String name, String record_key){
        _name = name;
        _record_key = record_key;
    }

}
