package uk.co.section9.zotdroid.data.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Vector;

import uk.co.section9.zotdroid.data.zotero.Attachment;

/**
 * Created by oni on 14/07/2017.
 */

// TODO - We should add some inheritence here as many functions are the same across tables

public class Attachments extends BaseData {

    public static final String TAG = "zotdroid.data.Attachments";

    protected static final String TABLE_NAME = "attachments";

    public String get_table_name(){
        return TABLE_NAME;
    }

    public void createTable(SQLiteDatabase db) {
        String CREATE_TABLE_ATTACHMENTS = "CREATE TABLE \"" +TABLE_NAME + "\" ( \"file_type\" VARCHAR, \"file_name\" TEXT," +
                "\"zotero_key\" VARCHAR PRIMARY KEY, \"parent\" VARCHAR, \"version\" VARCHAR)";
        db.execSQL(CREATE_TABLE_ATTACHMENTS);
    }

    public void deleteTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
    }

    public static ContentValues getValues (Attachment attachment) {
        ContentValues values = new ContentValues();
        values.put("zotero_key", attachment.get_zotero_key());
        values.put("file_type", attachment.get_file_type());
        values.put("file_name", attachment.get_file_name());
        values.put("parent", attachment.get_parent());
        values.put("version", attachment.get_version());
        return values;
    }

    public static Attachment getAttachmentFromValues(ContentValues values) {
        Attachment attachment = new Attachment();
        attachment.set_zotero_key((String)values.get("zotero_key"));
        attachment.set_file_type((String)values.get("file_type"));
        attachment.set_file_name((String)values.get("file_name"));
        attachment.set_parent((String)values.get("parent"));
        attachment.set_version((String)values.get("version"));
        return attachment;
    }

    public void updateAttachment(Attachment attachment, SQLiteDatabase db) {
        db.execSQL("UPDATE " + get_table_name() +
                " SET file_type=\"" + attachment.get_file_type() + "\", " +
                "file_name=\"" + attachment.get_file_name() + "\", " +
                "parent=\"" + attachment.get_parent() + "\", " +
                "version=\"" + attachment.get_version() + "\" " +
                "WHERE zotero_key=\"" + attachment.get_zotero_key() + "\";");
    }

    public Boolean attachmentExists(Attachment a, SQLiteDatabase db){
        String q =  "select count(*) from \"" + get_table_name() + "\" where zotero_key=\"" + a.get_zotero_key() + "\";";
        return exists(q,db);
    }

    public void deleteAttachment(Attachment a, SQLiteDatabase db){
        db.execSQL("DELETE FROM " + get_table_name() + " WHERE zotero_key=\"" + a.get_zotero_key() + "\";");
    }

    public void writeAttachment (Attachment attachment, SQLiteDatabase db) {
        ContentValues values = getValues(attachment);
        db.insert(get_table_name(), null, values);
    }

    /**
     * Grrab all the attachments for a particular record - useful in pagination.
     * @param db
     * @param parent_key
     * @return
     */
    public Vector<Attachment> getForRecord(SQLiteDatabase db, String parent_key) {
        Vector<Attachment> zv = new Vector<Attachment>();
        ContentValues values = new ContentValues();
        Cursor cursor = db.rawQuery("SELECT * FROM " + get_table_name() + " WHERE parent=\"" + parent_key + "\";", null);

        while (cursor.moveToNext()) {
            values.clear();
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                values.put(cursor.getColumnName(i), cursor.getString(i));
            }
            zv.add(getAttachmentFromValues(values));
        }
        cursor.close();
        return zv;
    }

}
