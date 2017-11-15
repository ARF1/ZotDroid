package uk.co.section9.zotdroid.data.tables;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import uk.co.section9.zotdroid.Util;
import uk.co.section9.zotdroid.data.BaseData;
import uk.co.section9.zotdroid.data.zotero.Summary;

/**
 * Created by oni on 11/07/2017.
 */

public class Summary extends BaseData {

    protected static final String TABLE_NAME = "summary";

    public String get_table_name(){
        return TABLE_NAME;
    }

    public final String TAG = "zotdroid.data.Summary";

    public void createTable(SQLiteDatabase db) {
        String CREATE_TABLE_SUMMARY = "CREATE TABLE \"" + TABLE_NAME + "\" (\"date_synced\" DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "\"last_version\" VARCHAR)";
        db.execSQL(CREATE_TABLE_SUMMARY);
    }

    public void deleteTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
    }

    public ContentValues getValues (uk.co.section9.zotdroid.data.zotero.Summary summary) {
        ContentValues values = new ContentValues();
        values.put("date_synced", Util.dateToDBString(summary.get_date_synced()));
        values.put("last_version", summary.get_last_version());
        return values;
    }

    public uk.co.section9.zotdroid.data.zotero.Summary getSummaryFromValues(ContentValues values) {
        uk.co.section9.zotdroid.data.zotero.Summary summary = new uk.co.section9.zotdroid.data.zotero.Summary();
        summary.set_date_synced( Util.dbStringToDate((String)values.get("date_synced")));
        summary.set_last_version((String)values.get("last_version"));
        return summary;
    }

    /**
     * Take a record and write it to the database
     * @param summary
     */
    public void writeSummary(uk.co.section9.zotdroid.data.zotero.Summary summary, SQLiteDatabase db) {
        clearRecords(db);
        ContentValues values = getValues(summary);
        db.insert(get_table_name(), null, values);
    }

    /**
     * Delete all the records in the DB - Good for syncing perhaps?
     */
    public void clearRecords(SQLiteDatabase db) {
        db.execSQL("DELETE FROM " + get_table_name());
    }
}
