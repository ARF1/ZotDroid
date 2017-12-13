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
                "\"zotero_key\" VARCHAR PRIMARY KEY, \"parent\" VARCHAR, \"version\" VARCHAR, \"storageModTime\" INT, \"storageHash\" TEXT)";
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
        values.put("storageModTime", attachment.get_storage_mod_time());
        values.put("storageHash", attachment.get_storage_hash());
        return values;
    }

    public static Attachment getAttachmentFromValues(ContentValues values) {
        Attachment attachment = new Attachment();
        attachment.set_zotero_key(values.getAsString("zotero_key"));
        attachment.set_file_type(values.getAsString("file_type"));
        attachment.set_file_name(values.getAsString("file_name"));
        attachment.set_parent(values.getAsString("parent"));
        attachment.set_version(values.getAsString("version"));
        attachment.set_storage_mod_time(values.getAsLong("storageModTime"));
        attachment.set_storage_hash(values.getAsString("storageHash"));
        return attachment;
    }

    public boolean attachmentExists(String key, SQLiteDatabase db){
        return exists(get_table_name(), key, db);
    }

    public Attachment getAttachmentByKey(String key, SQLiteDatabase db){
        if (attachmentExists(key,db)) {
            return getAttachmentFromValues(getSingle(db, key));
        }
        return null;
    }

    public void updateAttachment(Attachment attachment, SQLiteDatabase db) {
        ContentValues values = getValues(attachment);
        db.update(get_table_name(), values, "zotero_key=?", new String[] {attachment.get_zotero_key()});
    }

    public Boolean attachmentExists(Attachment a, SQLiteDatabase db){
        return exists(get_table_name(),a.get_zotero_key(),db);
    }

    public void deleteAttachment(Attachment a, SQLiteDatabase db){
        db.delete(get_table_name(), "zotero_key=?", new String[] {a.get_zotero_key()});
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
