package uk.co.section9.zotdroid.data.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Vector;

import uk.co.section9.zotdroid.data.zotero.Record;

/**
 * Created by oni on 11/07/2017.
 */

public class Records extends BaseData {

    public final String TAG = "Records";

    protected final String TABLE_NAME = "records";

    public String get_table_name(){
        return TABLE_NAME;
    }

    public void createTable(SQLiteDatabase db) {
        String CREATE_TABLE_RECORDS = "CREATE TABLE \"" +TABLE_NAME + "\" (\"date_added\" DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "\"date_modified\" DATETIME DEFAULT CURRENT_TIMESTAMP, " + "\"content_type\" VARCHAR, \"item_type\" VARCHAR, \"title\" TEXT, " +
                "\"zotero_key\" VARCHAR PRIMARY KEY, \"parent\" VARCHAR, \"version\" VARCHAR )";
        db.execSQL(CREATE_TABLE_RECORDS);
    }

    public void deleteTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
    }

    public ContentValues getValues (Record record) {
        ContentValues values = new ContentValues();
        //values.put("date_added", Util.dateToDBString(record.get_date_added()));
        //values.put("date_modified", Util.dateToDBString(record.get_date_modified()));
        values.put("date_added", record.get_date_added());
        values.put("date_modified", record.get_date_modified());
        values.put("zotero_key", record.get_zotero_key());
        values.put("content_type", record.get_content_type());
        values.put("item_type", record.get_item_type());
        values.put("title", record.get_title());
        values.put("parent",record.get_parent());
        values.put("version", record.get_version());

        return values;
    }

    public Record getRecordFromValues(ContentValues values) {
        Record record = new Record();
        //record.set_date_added( Util.dbStringToDate((String)values.get("date_added")));
        //record.set_date_modified( Util.dbStringToDate((String)values.get("date_modified")));
        record.set_date_added((String)values.get("date_added"));
        record.set_date_modified((String)values.get("date_modified"));
        record.set_content_type((String)values.get("content_type"));
        record.set_item_type((String)values.get("item_type"));
        record.set_title((String)values.get("title"));
        record.set_zotero_key((String)values.get("zotero_key"));
        record.set_parent((String)values.get("parent"));
        record.set_version((String)values.get("version"));

        return record;
    }

    public Boolean recordExists(Record r, SQLiteDatabase db) {
        String q = "select count(*) from \"" + get_table_name() + "\" where zotero_key=\"" + r.get_zotero_key() + "\";";
        return exists(q,db);
    }

    public void updateRecord(Record record, SQLiteDatabase db) {
        db.execSQL("UPDATE " + get_table_name() +
                //" SET date_added=\"" +  Util.dateToDBString(record.get_date_added()) + "\", " +
                //"date_modified=\"" +  Util.dateToDBString(record.get_date_modified()) + "\", " +
                " SET date_added=\"" +  record.get_date_added() + "\", " +
                "date_modified=\"" +  record.get_date_modified() + "\", " +
                "content_type=\"" + record.get_content_type() + "\", " +
                "item_type=\"" + record.get_item_type() + "\", " +
                "title=\"" + record.get_title() + "\", " +
                "parent=\"" + record.get_parent() + "\", " +
                "version=\"" + record.get_version() + "\" " +
                "WHERE zotero_key=\"" + record.get_zotero_key() + "\";");
    }

    /**
     * Take a record and write it to the database
     * @param record
     * @param db
     */
    public void writeRecord(Record record, SQLiteDatabase db) {
        ContentValues values = getValues(record);
        db.insert(get_table_name(), null, values);
    }

    public void deleteRecord( Record r, SQLiteDatabase db ){
        db.execSQL("DELETE FROM " + get_table_name() + " WHERE zotero_key=\"" + r.get_zotero_key() + "\";");
    }

    public boolean recordExists(String key, SQLiteDatabase db){
        return exists("select * from " + get_table_name() + " where zotero_key=\"" + key + "\";", db);
    }

    public Record getRecordByKey(String key, SQLiteDatabase db){
        if (recordExists(key,db)) {
            return getRecordFromValues(getSingle(db, key));
        }
        return null;
    }

    // Get methods

    /**
     * Get all the records from our database upto the end limit
     * @param end
     * @param db
     * @return
     */
    public Vector<Record> getRecords (int end, SQLiteDatabase db) {
        Vector<Record> records = new Vector<Record>();
        ContentValues values = new ContentValues();
        Cursor cursor = db.rawQuery("select * from \"" + get_table_name() + "\" limit " + end + ";", null);
        while (cursor.moveToNext()){
            values.clear();
            for (int i = 0; i < cursor.getColumnCount(); i++){
                values.put(cursor.getColumnName(i),cursor.getString(i));
            }
            records.add(getRecordFromValues(values));
        }

        cursor.close();
        return records;
    }

}
