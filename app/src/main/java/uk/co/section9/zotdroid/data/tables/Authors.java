package uk.co.section9.zotdroid.data.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Vector;

import uk.co.section9.zotdroid.Util;
import uk.co.section9.zotdroid.data.BaseData;
import uk.co.section9.zotdroid.data.zotero.Author;

/**
 * Created by oni on 15/11/2017.
 */

public class Authors extends BaseData {

    public final String TAG = "Authors";

    protected final String TABLE_NAME = "authors";

    public String get_table_name() {
        return TABLE_NAME;
    }

    public void createTable(SQLiteDatabase db) {
        String CREATE_TABLE_RECORDS = "CREATE TABLE \"" + TABLE_NAME + "\" (\"record_key\" VARCHAR, \"author\" VARCHAR)";
        db.execSQL(CREATE_TABLE_RECORDS);
    }

    public void deleteTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
    }

    public ContentValues getValues(Author author) {
        ContentValues values = new ContentValues();
        values.put("record_key", author.get_record_key());
        values.put("author", author.get_name());
        return values;
    }

    public Author getAuthorFromValues(ContentValues values) {
        Author author = new Author("", "");
        author.set_name((String) values.get("author"));
        author.set_record_key((String) values.get("record_key"));
        return author;
    }

    /**
     * Take an author and write it to the database
     *
     * @param author
     * @param db
     */
    public void writeAuthor(Author author, SQLiteDatabase db) {
        ContentValues values = getValues(author);
        db.insert(get_table_name(), null, values);
    }

    public void deleteAuthor(Author author, SQLiteDatabase db) {
        db.execSQL("DELETE FROM " + get_table_name() + " WHERE author=\"" + author.get_name() + "\" and record_key=\"" + author.get_record_key() + "\";");
    }

}
