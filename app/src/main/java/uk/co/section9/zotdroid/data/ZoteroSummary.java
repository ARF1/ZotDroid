package uk.co.section9.zotdroid.data;

/**
 * Created by oni on 27/07/2017.
 */

import java.util.Date;

/**
 * Created by oni on 11/07/2017.
 */

public class ZoteroSummary extends BaseData {
    public static final String TAG = "zotdroid.data.ZoteroSummary";

    public Date get_date_synced() {
        return _date_synced;
    }

    public void set_date_synced(Date _date_synced) {
        this._date_synced = _date_synced;
    }

    public String get_last_version_items() {
        return _last_version_items;
    }

    public void set_last_version_items(String last_version_items) {
        this._last_version_items = last_version_items;
    }

    public String get_last_version_collections() {
        return _last_version_collections;
    }

    public void set_last_version_collections(String last_version_collections) {
        this._last_version_collections = last_version_collections;
    }

    protected Date _date_synced;
    protected String _last_version_items;
    protected String _last_version_collections;

    public ZoteroSummary() {
        _date_synced = new Date();
        _last_version_collections = "0000";
        _last_version_items = "0000";
    }

}
