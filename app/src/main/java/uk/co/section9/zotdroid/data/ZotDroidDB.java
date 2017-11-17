package uk.co.section9.zotdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Date;
import java.util.Vector;

import uk.co.section9.zotdroid.R;
import uk.co.section9.zotdroid.data.tables.Attachments;
import uk.co.section9.zotdroid.data.tables.Authors;
import uk.co.section9.zotdroid.data.tables.CollectionsItems;
import uk.co.section9.zotdroid.data.tables.Collections;
import uk.co.section9.zotdroid.data.tables.Records;
import uk.co.section9.zotdroid.data.tables.Summary;
import uk.co.section9.zotdroid.data.zotero.Attachment;
import uk.co.section9.zotdroid.data.zotero.Author;
import uk.co.section9.zotdroid.data.zotero.Collection;
import uk.co.section9.zotdroid.data.zotero.CollectionItem;
import uk.co.section9.zotdroid.data.zotero.Record;

/**
 * Created by oni on 11/07/2017.
 */

public class ZotDroidDB extends SQLiteOpenHelper {

    // All Static variables
    private static final int DATABASE_VERSION = 1;
    private static String DATABASE_NAME = "zotdroid.sqlite";

    public static final String TAG = "zotdroid.ZotDroidDB";
    private SQLiteDatabase _db;

    private Collections         _collectionsTable       = new Collections();
    private Attachments         _attachmentsTable       = new Attachments();
    private Records             _recordsTable           = new Records();
    private Summary             _summaryTable           = new Summary();
    private CollectionsItems    _collectionsItemsTable  = new CollectionsItems();
    private Authors             _authorsTable = new Authors();

    // A small class to hold caching info
    private class SearchCache {
        public String last_search;
        public int last_num_results;
        public boolean valid = false;
    }

    private SearchCache             _cache = new SearchCache();

    public ZotDroidDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this._db = getWritableDatabase(); // Should kickstart all the creation we need :S
        _check_and_create();
    }

    /**
     * Alternative constructor for if we change the database location
     * @param context
     * @param alternative_location
     */
    public ZotDroidDB(Context context, String alternative_location) {
        // Double check we have no trailing slash
        super(context, alternative_location + "/zotdroid.sqlite", null, DATABASE_VERSION);
        this._db = getWritableDatabase();
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

        if (!checkTableExists(_authorsTable.get_table_name())) {
            _authorsTable.createTable(_db);
        }

        if (!checkTableExists(_summaryTable.get_table_name())) {
            _summaryTable.createTable(_db);
            uk.co.section9.zotdroid.data.zotero.Summary s = new uk.co.section9.zotdroid.data.zotero.Summary();
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
        db.execSQL("DROP TABLE IF EXISTS \"" + _authorsTable.get_table_name() + "\"");
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
        db.execSQL("DROP TABLE IF EXISTS " + _authorsTable.get_table_name());
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

    // Get the number of Zotero Records in a particular collection (or ALL if c is null)
    public int getNumRecordsCollection(Collection c){
        int result = 0;
        Cursor cursor = _db.rawQuery("select count(*) from \"" +  _recordsTable.get_table_name() + "\";", null);
        if (c != null) {
            cursor = _db.rawQuery("select count(*) from \"" +
                    _collectionsItemsTable.get_table_name() + "\" where collection = \"" +
                    c.get_zotero_key() + "\";", null);
        }
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
    public boolean recordExists(Record r) { return _recordsTable.recordExists(r,_db);}
    public boolean collectionExists(Collection c) { return _collectionsTable.collectionExists(c,_db); }
    public boolean attachmentExists(Attachment a) { return _attachmentsTable.attachmentExists(a,_db); }

    // Update methods
    public void updateCollection(Collection collection) {
        _collectionsTable.updateCollection(collection,_db);
    }
    public void updateRecord(Record record) {
        _recordsTable.updateRecord(record,_db);
    }

    public void updateAttachment(Attachment attachment) {
        _attachmentsTable.updateAttachment(attachment,_db);
    }

    public Collection getCollection(String key) {
        return _collectionsTable.getCollection(key,_db);
    }

    public uk.co.section9.zotdroid.data.zotero.Summary getSummary() { return _summaryTable.getSummaryFromValues( readRow(_summaryTable.get_table_name(),0)); }

    public Collection getCollection(int rownum) {
        return _collectionsTable.getCollectionFromValues(readRow(_collectionsTable.get_table_name(),rownum));
    }

    public Attachment getAttachment(String key) {
        return _attachmentsTable.getAttachmentFromValues(_attachmentsTable.getSingle(_db,key));
    }

    public  Vector<Attachment> getAttachmentsForRecord(Record record) {
         return _attachmentsTable.getForRecord(_db,record.get_zotero_key());
    }

    public Attachment getAttachment(int rownumber) {
        return _attachmentsTable.getAttachmentFromValues(readRow(_attachmentsTable.get_table_name(),rownumber));
    }

    public CollectionItem getCollectionItem(String key) {
        return _collectionsItemsTable.getCollectionItemFromValues(_collectionsItemsTable.getSingle(_db,key));
    }

    public CollectionItem getCollectionItem(int rownumber) {
        return _collectionsItemsTable.getCollectionItemFromValues(readRow(_collectionsItemsTable.get_table_name(), rownumber));
    }

    // Composite Get methods

    public Record getRecord(String key) {
        if (_recordsTable.recordExists(key,_db)) {
            Record r = _recordsTable.getRecordFromValues(_recordsTable.getSingle(_db, key));
            for (Author a : _authorsTable.getAuthorsForRecord(r, _db)) {
                r.add_author(a);
            }
            return r;
        }
        return null;
    }

    // TODO - add protections around this one
    public Record getRecord(int rownumber) {
        Record r = _recordsTable.getRecordFromValues(readRow(_recordsTable.get_table_name(),rownumber));
        for(Author a : _authorsTable.getAuthorsForRecord(r,_db)){
            r.add_author(a);
        }
        return r;
    }

    public Vector<Record> getRecords(int end) {
        Vector<Record> records = _recordsTable.getRecords(end, _db);
        for(Record r : records){
            for(Author a : _authorsTable.getAuthorsForRecord(r,_db)){
                r.add_author(a);
            }
        }
        return records;
    }

    // TODO - could do this as an actual SQL query I suppose?
    public Vector<Collection> getCollectionForItem(Record record){
        Vector<CollectionItem> tci = _collectionsItemsTable.getCollectionItemForItem(record,_db);
        Vector<Collection> tc = new Vector<>();

        for (CollectionItem ci : tci) {
            tc.add(_collectionsTable.getCollection(ci.get_collection(),_db));
        }
        return tc;
    }

    // TODO - could do this as an actual SQL query I suppose?
    public Vector<Record> getItemsForCollection (Collection collection){
        Vector<CollectionItem> tci = _collectionsItemsTable.getItemsForCollection(collection.get_zotero_key(),_db);
        Vector<Record> tc = new Vector<>();

        for (CollectionItem ci : tci) {
            tc.add(_recordsTable.getRecordByKey(ci.get_item(),_db));
        }
        return tc;
    }

    public Vector<Record> searchRecords (Collection collection, String searchterm, int end){
        Vector<Record> records = new Vector<Record>();
        _cache.last_num_results = 0;

        // Annoyingly we must search the entire collection - this could be slow in certain cases
        if (collection == null){
            ContentValues values = new ContentValues();
            Cursor cursor = _db.rawQuery("select * from \"" + _recordsTable.get_table_name() + "\";", null);
            while (cursor.moveToNext()){
                values.clear();
                for (int i = 0; i < cursor.getColumnCount(); i++){
                    values.put(cursor.getColumnName(i),cursor.getString(i));
                }

                Record r = _recordsTable.getRecordFromValues(values);
                if (r.search(searchterm) || searchterm.isEmpty()) {
                    _cache.last_num_results +=1;
                    if (records.size() < end) {
                        records.add(r);
                    }
                };
            }
            cursor.close();
        } else {
            Vector<CollectionItem> tci = _collectionsItemsTable.getItemsForCollection(collection.get_zotero_key(),_db);
            for (CollectionItem ci : tci) {
                Record r = _recordsTable.getRecordByKey(ci.get_item(),_db);
                if (r.search(searchterm) || searchterm.isEmpty()) {
                    _cache.last_num_results += 1;
                    if (records.size() < end) {
                        records.add(r);
                    }
                }
            }
        }
        _cache.valid = true;
        _cache.last_search = searchterm;

        return records;
    }

    // This is slow, so we cache the results of the previous search operation for extra speed
    public int getNumRecordsSearch (Collection collection, String searchterm) {
        int tt = 0;
        if (_cache.last_search.equals(searchterm)) { return _cache.last_num_results; }

        if (collection == null){
            ContentValues values = new ContentValues();
            Cursor cursor = _db.rawQuery("select count(*) from \"" + _recordsTable.get_table_name() + "\";", null);
            while (cursor.moveToNext() ){
                values.clear();
                for (int i = 0; i < cursor.getColumnCount(); i++){
                    values.put(cursor.getColumnName(i),cursor.getString(i));
                }
                Record r = _recordsTable.getRecordFromValues(values);
                if (r.search(searchterm) || searchterm.isEmpty()) { tt+=1; };
            }

            cursor.close();
        } else {
            Vector<CollectionItem> tci = _collectionsItemsTable.getItemsForCollection(collection.get_zotero_key(),_db);

            for (CollectionItem ci : tci) {
                Record r = _recordsTable.getRecordByKey(ci.get_item(),_db);
                if (r.search(searchterm) || searchterm.isEmpty()) { tt+=1; };
            }
        }
        return tt;
    }


    // Get number methods

    public int getNumRecords() { return getNumRows(_recordsTable.get_table_name()); }
    public int getNumAttachments() { return getNumRows(_attachmentsTable.get_table_name()); }
    public int getNumCollections () { return getNumRows(_collectionsTable.get_table_name()); }
    public int getNumCollectionsItems () { return getNumRows(_collectionsItemsTable.get_table_name()); }

    // Write methods

    public void writeCollection(Collection collection){ _collectionsTable.writeCollection(collection,_db); }
    public void writeAttachment(Attachment attachment){ _attachmentsTable.writeAttachment(attachment,_db); }
    public void writeSummary(uk.co.section9.zotdroid.data.zotero.Summary summary){ _summaryTable.writeSummary(summary,_db); }
    public void writeCollectionItem(CollectionItem ic){ _collectionsItemsTable.writeCollection(ic,_db); }

    // Composite write methods

    public void writeRecord(Record record){
        _recordsTable.writeRecord(record,_db);
        for (Author a : record.get_authors()){
            _authorsTable.writeAuthor(a,_db);
        }
    }


    // Delete methods
    public void deleteAttachment(Attachment a) {_attachmentsTable.deleteAttachment(a,_db);}

    public void deleteRecord(Record r) {
        _recordsTable.deleteRecord(r,_db);
        _collectionsItemsTable.deleteByRecord(r,_db);
        _authorsTable.deleteByRecord(r,_db);
    }
    public void deleteCollection(Collection c) {
        _collectionsTable.deleteCollection(c,_db);
        _collectionsItemsTable.deleteByCollection(c,_db);
    }

    public void removeRecordFromCollections(Record r){
        _collectionsItemsTable.deleteByRecord(r,_db);
    }

    // Search methods



}
