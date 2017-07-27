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

        if (!checkTableExists(SummaryTable.get_table_name())) {
            SummaryTable.createTable(_db);
        }

        if (!checkTableExists(RecordsTable.get_table_name())) {
            RecordsTable.createTable(_db);
        }

        if (!checkTableExists(CollectionsTable.get_table_name())) {
            CollectionsTable.createTable(_db);
        }

        if (!checkTableExists(AttachmentsTable.get_table_name())) {
            AttachmentsTable.createTable(_db);
        }

        if (!checkTableExists(CollectionsItemsTable.get_table_name())) {
            CollectionsItemsTable.createTable(_db);
        }

        // There should always be one and only one summary record

        if (!checkTableExists(SummaryTable.get_table_name())) {
            SummaryTable.createTable(_db);
        }
        clearTable(SummaryTable.get_table_name());
        ZoteroSummary s = new ZoteroSummary();
        s.set_date_synced(new Date());
        s.set_last_version_collections("0000");
        s.set_last_version_items("0000");
        writeSummary(s);
    }

    /**
     * Destroy all the saved data and recreate if needed.
     */
    public void reset() {
        if (_db != null){
            _db.close();
        }

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS \"" + SummaryTable.get_table_name() + "\"");
        db.execSQL("DROP TABLE IF EXISTS \"" + RecordsTable.get_table_name() + "\"");
        db.execSQL("DROP TABLE IF EXISTS \"" + CollectionsTable.get_table_name() + "\"");
        db.execSQL("DROP TABLE IF EXISTS \"" + AttachmentsTable.get_table_name() + "\"");
        db.execSQL("DROP TABLE IF EXISTS \"" + CollectionsItemsTable.get_table_name() + "\"");

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
        db.execSQL("DROP TABLE IF EXISTS " + CollectionsTable.get_table_name());
        db.execSQL("DROP TABLE IF EXISTS " + RecordsTable.get_table_name());
        db.execSQL("DROP TABLE IF EXISTS " + SummaryTable.get_table_name());
        db.execSQL("DROP TABLE IF EXISTS " + AttachmentsTable.get_table_name());
        db.execSQL("DROP TABLE IF EXISTS " + CollectionsItemsTable.get_table_name());
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

    /**
     * Get all the records from our database into memory
     * @return
     */
    public Vector<ZoteroRecord> getRecords() {
        int result = 0;
        Vector<ZoteroRecord> records = new Vector<ZoteroRecord>();
        ContentValues values = new ContentValues();
        Cursor cursor = _db.rawQuery("select * from \"" + RecordsTable.get_table_name() + "\";", null);
        while (cursor.moveToNext()){
            values.clear();
            for (int i = 0; i < cursor.getColumnCount(); i++){
                values.put(cursor.getColumnName(i),cursor.getString(i));
            }
            records.add(RecordsTable.getRecordFromValues(values));

        }

        cursor.close();
        return records;
    }

    public void writeRecord(ZoteroRecord record){
        RecordsTable.writeRecord(record,_db);
    }


    private boolean exists(String q){
        Cursor cursor = _db.rawQuery(q, null);
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.getInt(0) != 0){
                return true;
            }
        }
        return false;
    }

    public boolean recordExists(String key) { return exists(RecordsTable.recordExists(key)); }
    public boolean collectionExists(String key) { return exists(CollectionsTable.collectionExists(key)); }
    public boolean attachmentExists(String key) { return exists(AttachmentsTable.attachmentExists(key)); }


    public ZoteroCollection getCollection(String key) {
        return CollectionsTable.getCollectionFromValues( CollectionsTable.getSingle(_db,key));
    }

    public ZoteroRecord getRecord(String key) {
        return RecordsTable.getRecordFromValues( RecordsTable.getSingle(_db, key));
    }

    public ZoteroAttachment getAttachment(String key) {
        return AttachmentsTable.getAttachmentFromValues(AttachmentsTable.getSingle(_db,key));
    }

    public void writeCollection(ZoteroCollection collection){ CollectionsTable.writeCollection(collection,_db); }

    public void writeAttachment(ZoteroAttachment attachment){ AttachmentsTable.writeAttachment(attachment,_db); }

    public ZoteroSummary getSummary() { return SummaryTable.getRecordFromValues( readRow(SummaryTable.get_table_name(),0)); }

    public void writeSummary(ZoteroSummary summary){ clearTable(SummaryTable.get_table_name()); SummaryTable.writeSummary(summary,_db); }

    public void writeCollectionItem(ZoteroCollectionItem ic){ CollectionsItemsTable.writeCollection(ic,_db); }
}
