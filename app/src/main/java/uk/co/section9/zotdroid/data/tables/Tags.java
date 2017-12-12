package uk.co.section9.zotdroid.data.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.DatabaseUtils;

import java.util.Vector;

import uk.co.section9.zotdroid.data.zotero.Tag;
import uk.co.section9.zotdroid.data.zotero.Record;

/**
 * Created by oni on 28/11/2017.
 */

public class Tags extends BaseData {

    public final String TAG = "Tags";

    protected final String TABLE_NAME = "tags";

    public String get_table_name() {
        return TABLE_NAME;
    }

    public void createTable(SQLiteDatabase db) {
        String CREATE_TABLE_RECORDS = "CREATE TABLE \"" + TABLE_NAME + "\" (\"record_key\" VARCHAR, \"name\" VARCHAR)";
        db.execSQL(CREATE_TABLE_RECORDS);
    }

    public void deleteTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
    }

    public ContentValues getValues(Tag tag) {
        ContentValues values = new ContentValues();
        values.put("record_key", tag.get_record_key());
        values.put("name", tag.get_name());
        return values;
    }

    public Vector<Tag> getTagsForRecord(Record r, SQLiteDatabase db){
        ContentValues values = new ContentValues();
        Vector<Tag> tags = new Vector<>();
        Cursor cursor = db.rawQuery("select * from \"" + get_table_name() + "\" where record_key=\"" + r.get_zotero_key() + "\";", null);
        while (cursor.moveToNext()){
            values.clear();
            DatabaseUtils.cursorRowToContentValues(cursor, values);
            Tag a = getTagFromValues(values);
            tags.add(a);
        }
        cursor.close();
        return tags;
    }

    public Tag getTagFromValues(ContentValues values) {
        Tag tag = new Tag("", "");
        tag.set_name((String) values.get("name"));
        tag.set_record_key((String) values.get("record_key"));
        return tag;
    }

    /**
     * Take an author and write it to the database
     *
     * @param tag
     * @param db
     */
    public void writeTag(Tag tag, SQLiteDatabase db) {
        ContentValues values = getValues(tag);
        db.insert(get_table_name(), null, values);
    }

    public void deleteName(Tag tag, SQLiteDatabase db) {
        db.execSQL("DELETE FROM " + get_table_name() + " WHERE name=\"" + tag.get_name() + "\" and record_key=\"" + tag.get_record_key() + "\";");
    }

    public void deleteByRecord(Record r, SQLiteDatabase db){
        db.execSQL("DELETE FROM " + get_table_name() + " where record_key=\"" + r.get_zotero_key() + "\"");
    }

}
