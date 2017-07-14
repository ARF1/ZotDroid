package uk.co.section9.zotdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by oni on 11/07/2017.
 */

public class ZotDroidDB extends SQLiteOpenHelper {

    // All Static variables
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "prf";

    public static final String TAG = "zotdroid.ZotDroidDB";

    public RecordsTable     _records_table;
    public CollectionsTable _collections_table;
    public SummaryTable     _summary_table;

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

        _summary_table = new SummaryTable(this);
        _records_table = new RecordsTable(this);
        _collections_table = new CollectionsTable(this);

        if (!checkTableExists(SummaryTable.get_table_name())) {
            _summary_table.createTable();
        } else {
            _summary_table.populateFromDB();
        }

        if (!checkTableExists(RecordsTable.get_table_name())) {
            _records_table.createTable();
        } else {
            _records_table.populateFromDB();
        }

        if (!checkTableExists(CollectionsTable.get_table_name())) {
            _collections_table.createTable();
        } else {
            _collections_table.populateFromDB();
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
        db.execSQL("DROP TABLE IF EXISTS \"" + _summary_table.get_table_name() + "\"");
        db.execSQL("DROP TABLE IF EXISTS \"" + _records_table.get_table_name() + "\"");
        db.execSQL("DROP TABLE IF EXISTS \"" + _collections_table.get_table_name() + "\"");

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
        db.execSQL("DROP TABLE IF EXISTS " + _collections_table.get_table_name());
        db.execSQL("DROP TABLE IF EXISTS " + _records_table.get_table_name());
        db.execSQL("DROP TABLE IF EXISTS " + _summary_table.get_table_name());
        // Create tables again
        onCreate(db);
    }

    /**
     * Cheeky little pass through for the sub-table classes
     * @param statement
     */
    protected void execSQL(String statement) {
        _db.execSQL(statement);
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

    public void insertRow(String tablename, ContentValues values){
        _db.insert(tablename, null, values);
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
}
