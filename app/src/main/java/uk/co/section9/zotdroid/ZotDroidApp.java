package uk.co.section9.zotdroid;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import uk.co.section9.zotdroid.data.ZotDroidDB;

/**
 * Created by oni on 15/11/2017.
 */

public class ZotDroidApp extends Application {

    protected ZotDroidDB        _zotdroid_db;
    protected ZotDroidMem       _zotdroid_mem;

    @Override
    public void onCreate() {
        super.onCreate();
        // Create the memory pool
        _zotdroid_mem = new ZotDroidMem();

        // find the database
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String database_path = settings.getString("settings_db_location", "");
        if (Util.path_exists(database_path)) {
            database_path = Util.remove_trailing_slash(database_path);
            _zotdroid_db = new ZotDroidDB(this, database_path);
        } else {
            _zotdroid_db = new ZotDroidDB(this);
        }

    }

    public ZotDroidMem getMem() { return _zotdroid_mem; }

    public ZotDroidDB getDB() {
        return _zotdroid_db;
    }
}
