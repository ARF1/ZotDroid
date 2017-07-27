package uk.co.section9.zotdroid.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Date;
import java.util.Vector;

import uk.co.section9.zotdroid.Util;

/**
 * Created by oni on 11/07/2017.
 */

public class RecordsTable extends BaseData {

    public static final String TAG = "zotdroid.data.RecordsTable";

    protected static final String TABLE_NAME = "records";

    public static String get_table_name(){
        return TABLE_NAME;
    }

    public static void createTable(SQLiteDatabase db) {
        String CREATE_TABLE_RECORDS = "CREATE TABLE \"" +TABLE_NAME + "\" (\"date_added\" DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "\"content_type\" VARCHAR, \"item_type\" VARCHAR, \"title\" TEXT, \"author\" TEXT, " +
                "\"zotero_key\" VARCHAR PRIMARY KEY, \"parent\" VARCHAR, \"version\" VARCHAR )";
        db.execSQL(CREATE_TABLE_RECORDS);
    }

    public static void deleteTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
    }

    public static ContentValues getValues (ZoteroRecord record) {
        ContentValues values = new ContentValues();
        values.put("date_added", Util.dateToDBString(record.get_date_added()));
        values.put("zotero_key", record.get_zotero_key());
        values.put("content_type", record.get_content_type());
        values.put("item_type", record.get_item_type());
        values.put("title", record.get_title());
        values.put("author", record.get_author());
        values.put("parent",record.get_parent());
        values.put("version", record.get_version());

        return values;
    }

    public static ZoteroRecord getRecordFromValues(ContentValues values) {
        ZoteroRecord record = new ZoteroRecord();
        record.set_date_added( Util.dbStringToDate((String)values.get("date_added")));
        record.set_content_type((String)values.get("content_type"));
        record.set_item_type((String)values.get("item_type"));
        record.set_title((String)values.get("title"));
        record.set_author((String)values.get("author"));
        record.set_zotero_key((String)values.get("zotero_key"));
        record.set_parent((String)values.get("parent"));
        record.set_version((String)values.get("version"));

        return record;
    }

    public static String recordExists(String key) {
        return "select count(*) from \"" + get_table_name() + "\" where zotero_key=\"" + key + "\";"; }

    public void updateRecord(ZoteroRecord record) {
    }

    /**
     * Take a record and write it to the database
     * @param record
     */
    public static void writeRecord( ZoteroRecord record, SQLiteDatabase db) {
        ContentValues values = getValues(record);
        db.insert(get_table_name(), null, values);
    }

    /**
     * Delete all the records in the DB - Good for syncing perhaps?
     */
    public void clearRecords() {

    }

    public static String getRecord(String key){
        return "select * from \"" + get_table_name() + "\" where zotero_key=\"" + key + "\";";
    }


}
