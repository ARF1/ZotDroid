package uk.co.section9.zotdroid.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Date;
import java.util.Vector;

import uk.co.section9.zotdroid.Util;
import uk.co.section9.zotdroid.ZotDroidOps;

/**
 * Created by oni on 11/07/2017.
 */

public class RecordsTable extends BaseData {

    public final String TAG = "zotdroid.data.RecordsTable";

    protected final String TABLE_NAME = "records";

    public String get_table_name(){
        return TABLE_NAME;
    }

    public void createTable(SQLiteDatabase db) {
        String CREATE_TABLE_RECORDS = "CREATE TABLE \"" +TABLE_NAME + "\" (\"date_added\" DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "\"content_type\" VARCHAR, \"item_type\" VARCHAR, \"title\" TEXT, \"author\" TEXT, " +
                "\"zotero_key\" VARCHAR PRIMARY KEY, \"parent\" VARCHAR, \"version\" VARCHAR )";
        db.execSQL(CREATE_TABLE_RECORDS);
    }

    public void deleteTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
    }

    public ContentValues getValues (ZoteroRecord record) {
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

    public ZoteroRecord getRecordFromValues(ContentValues values) {
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

    public Boolean recordExists(String key, SQLiteDatabase db) {
        String q = "select count(*) from \"" + get_table_name() + "\" where zotero_key=\"" + key + "\";";
        return exists(q,db);
    }

    public void updateRecord(ZoteroRecord record, SQLiteDatabase db) {
        db.execSQL("UPDATE " + get_table_name() +
                " SET date_added=\"" + record.get_date_added() + "\", " +
                "content_type=\"" + record.get_content_type() + "\", " +
                "item_type=\"" + record.get_item_type() + "\", " +
                "title=\"" + record.get_title() + "\", " +
                "author=\"" + record.get_author() + "\", " +
                "parent=\"" + record.get_parent() + "\", " +
                "version=\"" + record.get_version() + "\" " +
                "WHERE zotero_key=\"" + record.get_zotero_key() + "\";");

    }

    /**
     * Take a record and write it to the database
     * @param record
     */
    public void writeRecord( ZoteroRecord record, SQLiteDatabase db) {
        ContentValues values = getValues(record);
        db.insert(get_table_name(), null, values);
    }

    public void deleteRecord( String key, SQLiteDatabase db ){
        db.execSQL("DELETE FROM " + get_table_name() + " WHERE zotero_key=\"" + key + "\";");
    }

    /**
     * Delete all the records in the DB - Good for syncing perhaps?
     */
    public void clearRecords() {

    }

    public ZoteroRecord getRecord(String key, SQLiteDatabase db){
        return getRecordFromValues(getSingle(db,key));
    }


}
