package uk.co.section9.zotdroid.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.util.Date;
import java.util.Vector;

import uk.co.section9.zotdroid.Util;

/**
 * Created by oni on 11/07/2017.
 */

public class RecordsTable extends BaseData {

    public static final String TAG = "zotdroid.data.RecordsTable";

    protected Vector<ZoteroRecord> _records = new Vector<ZoteroRecord>();

    // TODO - Make this a tree like structure and return these?
    public static class ZoteroRecord {

        public static final String TAG = "zotdroid.data.RecordsTable.ZoteroRecord";

        public String get_zotero_key() {
            return _zotero_key;
        }

        public void set_zotero_key(String key) {
            this._zotero_key = key;
        }

        public String get_author() {
            return _author;
        }

        public void set_author(String author) {
            this._author = _author;
        }

        public String get_title() {
            return _title;
        }

        public void set_title(String title) { this._title = title;}

        public String get_parent() {
            return _parent;
        }

        public void set_parent(String parent) {
            this._parent = parent;
        }

        public String get_content_type() {
            return _content_type;
        }

        public void set_content_type(String content_type) {
            this._content_type = content_type;
        }

        public String get_item_type() {
            return _item_type;
        }

        public void set_item_type(String item_type) {
            this._item_type = item_type;
        }

        public Date get_date_added() {
            return _date_added;
        }

        public void set_date_added(Date date_added) {
            this._date_added = date_added;
        }

        // For now, we cover all the bases we need for all possible items
        // Eventually we might have separate record tables

        protected String    _content_type;
        protected String    _title;
        protected String    _item_type;
        protected Date      _date_added;
        protected String    _author; // TODO - Just one for now but we will add more
        protected String    _zotero_key;
        protected String    _parent;
        public String toString() {
            return _title + " - " + _author;
        }

        public ZoteroRecord(){
            _date_added = new Date();
        }
    }

    protected static final String TABLE_NAME = "records";

    protected ZotDroidDB _db;

    /**
     * When the class is created we attempt to populate memory with the current records
     */
    public RecordsTable(ZotDroidDB db) {
        _db = db;
    }

    public static String get_table_name(){
        return TABLE_NAME;
    }

    public int getNumRecords() {
        return _records.size();
    }

    public Vector<ZoteroRecord> get_records(){
        return _records; // Copy of this?
    }


    public void createTable() {
        String CREATE_TABLE_RECORDS = "CREATE TABLE \"" +TABLE_NAME + "\" (\"date_added\" DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "\"content_type\" VARCHAR, \"item_type\" VARCHAR, \"title\" TEXT, \"author\" TEXT, " +
                "\"zotero_key\" VARCHAR PRIMARY KEY, \"parent\" VARCHAR )";
        _db.execSQL(CREATE_TABLE_RECORDS);
    }

    public void deleteTable() {
        _db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
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

        return record;
    }


    public void updateRecord(ZoteroRecord record) {

    }

    /**
     * Take a record and write it to the database
     * @param record
     */
    public void writeRecord( ZoteroRecord record) {
        ContentValues values = getValues(record);
        _db.insertRow(get_table_name(), values);
    }



    /**
     * Delete all the records in the DB - Good for syncing perhaps?
     */
    public void clearRecords() {

    }

    /**
     * Get all the records we have in the database
     */

    public void populateFromDB() {
        Vector<ZoteroRecord> rval = new Vector<ZoteroRecord>();
        int numrows = _db.getNumRows(get_table_name());

        for (int i=0; i < numrows; ++i){
            ContentValues values = null;
            values = _db.readRow(get_table_name(), i);
            ZoteroRecord record = getRecordFromValues(values);
            _records.add(record);
        }
    }

}
