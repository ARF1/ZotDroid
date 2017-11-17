package uk.co.section9.zotdroid.data.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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

        for (int i = 0; i < cursor.getColumnCount(); i++){
            values.put(cursor.getColumnName(i),cursor.getString(i));
        }
        return values;
    }

    protected boolean exists(String q, SQLiteDatabase db ){
        Cursor cursor = db.rawQuery(q, null);
        if (cursor != null) {
            return cursor.moveToFirst();
            /*if (cursor.g() > 0) {
                if (cursor.getInt(0) != 0) {
                    return true;
                }
            }*/
        }
        return false;
    }

    public String get_table_name() {
        return TABLE_NAME;
    }

    public void createTable(SQLiteDatabase db) {}

    public void deleteTable(SQLiteDatabase db) {}

}
