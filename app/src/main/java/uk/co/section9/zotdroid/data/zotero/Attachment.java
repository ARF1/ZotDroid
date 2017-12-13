package uk.co.section9.zotdroid.data.zotero;

/**
 * Created by oni on 14/07/2017.
 */

public class Attachment {

    public static final String TAG = "zotdroid.data.Attachment";

    protected String _file_name;
    protected String _zotero_key;
    protected String _parent;
    protected String _file_type;
    protected String _version;
    protected Long _storage_mod_time;
    protected String _storage_hash;
    
    public String get_version() { return _version; }

    public void set_version(String version) { _version = version; }

    public String get_file_name() {
        return _file_name;
    }

    public void set_file_name(String _file_name) {
        this._file_name = _file_name;
    }

    public String get_storage_path() {
        return _zotero_key + '/' + _file_name;
    }

    public String get_zotero_key() {
        return _zotero_key;
    }

    public void set_zotero_key(String _zotero_key) {
        this._zotero_key = _zotero_key;
    }

    public String get_parent() {
        return _parent;
    }

    public void set_parent(String parent) {
        this._parent = parent;
    }

    public String get_file_type() {
        return _file_type;
    }

    public void set_file_type(String _file_type) {
        this._file_type = _file_type;
    }

    public Long get_storage_mod_time() {
        return _storage_mod_time;
    }
    
    public void set_storage_mod_time(Long storage_mod_time) {
        this._storage_mod_time = storage_mod_time;
    }
    
    public String get_storage_hash() {
        return _storage_hash;
    }
    
    public void set_storage_hash(String storage_hash) {
        this._storage_hash = storage_hash;
    }
    
    public void copy(Attachment att){
        _file_name = att._file_name;
        _zotero_key = att._zotero_key;
        _parent = att._parent;
        _file_type = att._file_type;
        _version = att._version;
        _storage_mod_time = att._storage_mod_time;
        _storage_hash = att._storage_hash;
    }

    public Attachment() {
    }
}