package uk.co.section9.zotdroid.data.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.DatabaseUtils;

/**
 * Created by oni on 11/07/2017.
 */

public class BaseData {

    protected final String TABLE_NAME = "";

    public ContentValues getSingle(SQLiteDatabase db, String key){
        String q = "select * from " + this.get_table_name() + " where zotero_key=\"" + key + "\";";
        Cursor cursor = db.rawQuery(q, null);
        cursor.moveToFirst();
        ContentValues values = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(cursor, values);
        return values;
    }

    protected boolean exists(String table, String key, SQLiteDatabase db ){
        String q = "select count(*) from " + table + " where zotero_key=\"" + key + "\";";
        Cursor cursor = db.rawQuery(q, null);
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.getInt(0) != 0) {
                return true;
            }
        }
        return false;
    }

    public String get_table_name() {
        return TABLE_NAME;
    }

    public void createTable(SQLiteDatabase db) {}

    public void deleteTable(SQLiteDatabase db) {}

}
