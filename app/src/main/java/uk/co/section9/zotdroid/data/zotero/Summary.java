package uk.co.section9.zotdroid.data.zotero;

/**
 * Created by oni on 27/07/2017.
 */

import java.util.Date;

import uk.co.section9.zotdroid.data.BaseData;

/**
 * Created by oni on 11/07/2017.
 */

public class Summary extends BaseData {
    public static final String TAG = "zotdroid.data.Summary";

    public Date get_date_synced() {
        return _date_synced;
    }

    public void set_date_synced(Date _date_synced) {
        this._date_synced = _date_synced;
    }

    public String get_last_version() {
        return _last_version;
    }

    public void set_last_version(String last_version) {
        this._last_version = last_version;
    }

    protected Date _date_synced;
    protected String _last_version;

    public Summary() {
        _date_synced = new Date();
        _last_version = "0000";
    }

}
