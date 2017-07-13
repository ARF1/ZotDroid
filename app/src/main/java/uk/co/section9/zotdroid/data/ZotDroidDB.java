package uk.co.section9.zotdroid.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by oni on 11/07/2017.
 */

public class ZotDroidDB extends SQLiteOpenHelper {

    // All Static variables
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "prf";

    public ZotDroidDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    protected boolean checkTableExists(String tablename, SQLiteDatabase db){

        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = \""+ tablename +"\"", null);
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
        Check if our database exists and all the tables. If not then create
     */

    private void _check_and_create(SQLiteDatabase db) {
        if (!checkTableExists(SummaryTable.get_table_name(), db)) { db.execSQL(SummaryTable.createTable()); }
        if (!checkTableExists(RecordsTable.get_table_name(), db)) { db.execSQL(RecordsTable.createTable()); }
        if (!checkTableExists(CollectionsTable.get_table_name(), db)) { db.execSQL(CollectionsTable.createTable()); }
    }

    /**
     * Destroy all the saved data and recreate if needed.
     */
    public void reset() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS \"" + SummaryTable.get_table_name() + "\"");
        db.execSQL("DROP TABLE IF EXISTS \"" + RecordsTable.get_table_name() + "\"");
        db.execSQL("DROP TABLE IF EXISTS \"" + CollectionsTable.get_table_name() + "\"");
        onCreate(db);
    }



    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create all the tables, presumably in memory
        // We double check to see if we have any database tables already]
        _check_and_create(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + CollectionsTable.get_table_name());
        db.execSQL("DROP TABLE IF EXISTS " + RecordsTable.get_table_name());
        db.execSQL("DROP TABLE IF EXISTS " + SummaryTable.get_table_name());
        // Create tables again
        onCreate(db);
    }
}
