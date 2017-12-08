package uk.co.section9.zotdroid.data.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Vector;

import uk.co.section9.zotdroid.data.zotero.Record;
import uk.co.section9.zotdroid.data.zotero.Note;

/**
 * Created by oni on 28/11/2017.
 */

public class Notes extends BaseData {

    public final String TAG = "Notes";

    protected final String TABLE_NAME = "Notes";

    public String get_table_name() {
        return TABLE_NAME;
    }

    public void createTable(SQLiteDatabase db) {
        String CREATE_TABLE_RECORDS = "CREATE TABLE \"" + TABLE_NAME + "\" (\"record_key\" VARCHAR, \"note\" TEXT, \"version\" VARCHAR, \"zotero_key\" VARCHAR)";
        db.execSQL(CREATE_TABLE_RECORDS);
    }

    public void deleteTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
    }

    public ContentValues getValues(Note note) {
        ContentValues values = new ContentValues();
        values.put("record_key", note.get_record_key());
        values.put("note", note.get_note());
        values.put("zotero_key", note.get_zotero_key());
        values.put("version", note.get_version());
        return values;
    }

    public Vector<Note> getNotesForRecord(Record r, SQLiteDatabase db){
        ContentValues values = new ContentValues();
        Vector<Note> notes = new Vector<>();
        Cursor cursor = db.rawQuery("select * from \"" + get_table_name() + "\" where record_key=\"" + r.get_zotero_key() + "\";", null);
        while (cursor.moveToNext()){
            values.clear();
            for (int i = 0; i < cursor.getColumnCount(); i++){
                values.put(cursor.getColumnName(i),cursor.getString(i));
            }

            Note a = getNoteFromValues(values);
            notes.add(a);
        }
        cursor.close();
        return notes;
    }

    public Note getNoteFromValues(ContentValues values) {
        Note note = new Note((String) values.get("zotero_key"),
                (String) values.get("record_key"),
                (String) values.get("note"),
                (String) values.get("version")
                );
        return note;
    }

    public Boolean noteExists(Note n, SQLiteDatabase db) {
        return exists(get_table_name(),n.get_zotero_key(),db);
    }

    public Boolean noteExists(String key, SQLiteDatabase db) {
        return exists(get_table_name(), key, db);
    }

    public Note getNoteByKey(String key, SQLiteDatabase db){
        if (noteExists(key, db)) {
            return getNoteFromValues(getSingle(db, key));
        }
        return null;
    }


    public void updateNote(Note note, SQLiteDatabase db) {
        db.execSQL("UPDATE " + get_table_name() +
                " SET record_key =\"" +  note.get_record_key() + "\", " +
                "note =\"" + note.get_note() + "\", " +
                "version =\"" + note.get_version() + "\" " +
                "WHERE zotero_key=\"" + note.get_zotero_key() + "\";");
    }

    /**
     * Take an author and write it to the database
     *
     * @param note
     * @param db
     */
    public void writeNote(Note note, SQLiteDatabase db) {
        ContentValues values = getValues(note);
        db.insert(get_table_name(), null, values);
    }

    public void deletenote(Note note, SQLiteDatabase db) {
        db.execSQL("DELETE FROM " + get_table_name() + " WHERE zotero_key=\"" + note.get_zotero_key() + "\";");
    }

    public void deleteByRecord(Record r, SQLiteDatabase db){
        db.execSQL("DELETE FROM " + get_table_name() + " where record_key=\"" + r.get_zotero_key() + "\"");
    }

}
