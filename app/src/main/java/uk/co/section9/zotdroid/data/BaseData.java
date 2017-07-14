package uk.co.section9.zotdroid.data;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by oni on 11/07/2017.
 */

public class BaseData {

    protected static final String TABLE_NAME = "";

    public static String get_table_name() {
        return TABLE_NAME;
    }

    public static void createTable(SQLiteDatabase db) {}

    public static void deleteTable(SQLiteDatabase db) {}

}
