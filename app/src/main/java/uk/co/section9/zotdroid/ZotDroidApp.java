package uk.co.section9.zotdroid;

import android.app.Application;

import uk.co.section9.zotdroid.data.ZotDroidDB;

/**
 * Created by oni on 11/07/2017.
 */

public class ZotDroidApp extends Application {

    private static ZotDroidDB _db;

    @Override
    public void onCreate(){
        super.onCreate();
        _db = new ZotDroidDB(this);
    }

}
