package uk.co.section9.zotdroid.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by oni on 11/07/2017.
 */

public class BaseData {

    protected static final String TABLE_NAME = "";

    public static ContentValues getSingle(SQLiteDatabase db, String key){

        String q = "select * from \"" + get_table_name() + "\" where zotero_key=\"" + key + "\";";
        Cursor cursor = db.rawQuery(q, null);
        cursor.moveToFirst();
        ContentValues values = new ContentValues();

        for (int i = 0; i < cursor.getColumnCount(); i++){
            values.put(cursor.getColumnName(i),cursor.getString(i));
        }
        return values;
    }

    public static String get_table_name() {
        return TABLE_NAME;
    }

    public static void createTable(SQLiteDatabase db) {}

    public static void deleteTable(SQLiteDatabase db) {}

}
