package uk.co.section9.zotdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Date;
import java.util.Vector;

/**
 * Created by oni on 11/07/2017.
 */

public class ZotDroidDB extends SQLiteOpenHelper {

    // All Static variables
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "prf";

    public static final String TAG = "zotdroid.ZotDroidDB";
    private SQLiteDatabase _db;

    private CollectionsTable        _collectionsTable       = new CollectionsTable();
    private AttachmentsTable        _attachmentsTable       = new AttachmentsTable();
    private RecordsTable            _recordsTable           = new RecordsTable();
    private SummaryTable            _summaryTable           = new SummaryTable();
    private CollectionsItemsTable   _collectionsItemsTable  = new CollectionsItemsTable();

    public ZotDroidDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this._db = getWritableDatabase(); // Should kickstart all the creation we need :S
        _check_and_create();
    }

    protected boolean checkTableExists(String tablename){
        Cursor cursor = _db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = \""+ tablename +"\"", null);
        if(cursor!=null) {
            if(cursor.getCount()>0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }

    /**
     * Check if our database exists and all the tables. If not then create
     * TODO - odd split of requirements here, with passing this db into the table classes
     */

    private void _check_and_create() {

        if (!checkTableExists(_recordsTable.get_table_name())) {
            _recordsTable.createTable(_db);
        }

        if (!checkTableExists(_collectionsTable.get_table_name())) {
            _collectionsTable.createTable(_db);
        }

        if (!checkTableExists(_attachmentsTable.get_table_name())) {
            _attachmentsTable.createTable(_db);
        }

        if (!checkTableExists(_collectionsItemsTable.get_table_name())) {
            _collectionsItemsTable.createTable(_db);
        }

        if (!checkTableExists(_summaryTable.get_table_name())) {
            _summaryTable.createTable(_db);
            ZoteroSummary s = new ZoteroSummary();
            s.set_date_synced(new Date());
            s.set_last_version("0000");
            _summaryTable.writeSummary(s,_db);
        }
    }

    /**
     * Destroy all the saved data and recreate if needed.
     */
    public void reset() {
        if (_db != null){
            _db.close();
        }

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS \"" + _summaryTable.get_table_name() + "\"");
        db.execSQL("DROP TABLE IF EXISTS \"" + _recordsTable.get_table_name() + "\"");
        db.execSQL("DROP TABLE IF EXISTS \"" + _collectionsTable.get_table_name() + "\"");
        db.execSQL("DROP TABLE IF EXISTS \"" + _attachmentsTable.get_table_name() + "\"");
        db.execSQL("DROP TABLE IF EXISTS \"" + _collectionsItemsTable.get_table_name() + "\"");

        onCreate(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create all the tables, presumably in memory
        // We double check to see if we have any database tables already]
        this._db = db;
        _check_and_create();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + _collectionsTable.get_table_name());
        db.execSQL("DROP TABLE IF EXISTS " + _recordsTable.get_table_name());
        db.execSQL("DROP TABLE IF EXISTS " + _summaryTable.get_table_name());
        db.execSQL("DROP TABLE IF EXISTS " + _attachmentsTable.get_table_name());
        db.execSQL("DROP TABLE IF EXISTS " + _collectionsItemsTable.get_table_name());
        // Create tables again
        onCreate(db);
    }

    protected void clearTable(String tablename){
        _db.execSQL("DELETE from " + tablename);
    }


    // Get the number of rows in a table
    public int getNumRows(String tablename){
        int result = 0;
        Cursor cursor = _db.rawQuery("select count(*) from \"" + tablename + "\";", null);
        if (cursor != null) {
            cursor.moveToFirst();
            result = cursor.getInt(0);
        }
        cursor.close();
        return result;
    }


    // reads the first cursor
    // TODO - might be a faster way when we are grabbing them all?
    public ContentValues readRow(String tablename, int rownumber) {
        int result = 0;
        ContentValues values = new ContentValues();
        Cursor cursor = _db.rawQuery("select * from \"" + tablename + "\";", null);
        cursor.moveToPosition(rownumber);

        for (int i = 0; i < cursor.getColumnCount(); i++){
            values.put(cursor.getColumnName(i),cursor.getString(i));
        }

        cursor.close();
        return values;
    }


    // Existence methods
    public boolean recordExists(String key) { return _recordsTable.recordExists(key,_db);}
    public boolean collectionExists(String key) { return _collectionsTable.collectionExists(key,_db); }
    public boolean attachmentExists(String key) { return _attachmentsTable.attachmentExists(key,_db); }

    // Update methods
    public void updateCollection(ZoteroCollection collection) {
        _collectionsTable.updateCollection(collection,_db);
    }
    public void updateRecord(ZoteroRecord record) {
        _recordsTable.updateRecord(record,_db);
    }

    public void updateAttachment(ZoteroAttachment attachment) {
        _attachmentsTable.updateAttachment(attachment,_db);
    }

    // Get methods

    /**
     * Get all the records from our database into memory
     * @return
     */
    public Vector<ZoteroRecord> getRecords() {
        int result = 0;
        Vector<ZoteroRecord> records = new Vector<ZoteroRecord>();
        ContentValues values = new ContentValues();
        Cursor cursor = _db.rawQuery("select * from \"" + _recordsTable.get_table_name() + "\";", null);
        while (cursor.moveToNext()){
            values.clear();
            for (int i = 0; i < cursor.getColumnCount(); i++){
                values.put(cursor.getColumnName(i),cursor.getString(i));
            }
            records.add(_recordsTable.getRecordFromValues(values));
        }

        cursor.close();
        return records;
    }

    public ZoteroCollection getCollection(String key) {
        return _collectionsTable.getCollection(key,_db);
    }

    public ZoteroSummary getSummary() { return _summaryTable.getSummaryFromValues( readRow(_summaryTable.get_table_name(),0)); }

    public ZoteroCollection getCollection(int rownum) {
        return _collectionsTable.getCollectionFromValues(readRow(_collectionsTable.get_table_name(),rownum));
    }

    public ZoteroRecord getRecord(String key) {
        return _recordsTable.getRecordFromValues( _recordsTable.getSingle(_db, key));
    }

    public ZoteroRecord getRecord(int rownumber) {
        return _recordsTable.getRecordFromValues(readRow(_recordsTable.get_table_name(),rownumber));
    }

    public ZoteroAttachment getAttachment(String key) {
        return _attachmentsTable.getAttachmentFromValues(_attachmentsTable.getSingle(_db,key));
    }

    public ZoteroAttachment getAttachment(int rownumber) {
        return _attachmentsTable.getAttachmentFromValues(readRow(_attachmentsTable.get_table_name(),rownumber));
    }

    public ZoteroCollectionItem getCollectionItem(String key) {
        return _collectionsItemsTable.getCollectionItemFromValues(_collectionsItemsTable.getSingle(_db,key));
    }

    public ZoteroCollectionItem getCollectionItem(int rownumber) {
        return _collectionsItemsTable.getCollectionItemFromValues(readRow(_collectionsItemsTable.get_table_name(), rownumber));
    }

    // Get number methods

    public int getNumRecords() { return getNumRows(_recordsTable.get_table_name()); }
    public int getNumAttachments() { return getNumRows(_attachmentsTable.get_table_name()); }
    public int getNumCollections () { return getNumRows(_collectionsTable.get_table_name()); }
    public int getNumCollectionsItems () { return getNumRows(_collectionsItemsTable.get_table_name()); }

    // Write methods

    public void writeCollection(ZoteroCollection collection){ _collectionsTable.writeCollection(collection,_db); }
    public void writeRecord(ZoteroRecord record){
        _recordsTable.writeRecord(record,_db);
    }
    public void writeAttachment(ZoteroAttachment attachment){ _attachmentsTable.writeAttachment(attachment,_db); }
    public void writeSummary(ZoteroSummary summary){ _summaryTable.writeSummary(summary,_db); }
    public void writeCollectionItem(ZoteroCollectionItem ic){ _collectionsItemsTable.writeCollection(ic,_db); }


    // Delete methods
    public void deleteAttachment(String key) {_attachmentsTable.deleteAttachment(key,_db);}

    public void deleteRecord(String key) {
        _recordsTable.deleteRecord(key,_db);
        _collectionsItemsTable.deleteByRecord(key,_db);
    }
    public void deleteCollection(String key) {
        _collectionsTable.deleteCollection(key,_db);
        _collectionsItemsTable.deleteByCollection(key,_db);
    }

    public void removeRecordFromCollections(String key){
        _collectionsItemsTable.deleteByRecord(key,_db);
    }

}
