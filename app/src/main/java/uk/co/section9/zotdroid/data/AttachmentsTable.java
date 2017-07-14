package uk.co.section9.zotdroid.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import uk.co.section9.zotdroid.Util;

/**
 * Created by oni on 14/07/2017.
 */

public class AttachmentsTable {

    public static final String TAG = "zotdroid.data.AttachmentsTable";

    protected static final String TABLE_NAME = "attachments";

    public static String get_table_name(){
        return TABLE_NAME;
    }

    public static void createTable(SQLiteDatabase db) {
        String CREATE_TABLE_ATTACHMENTS = "CREATE TABLE \"" +TABLE_NAME + "\" ( \"file_type\" VARCHAR, \"file_name\" TEXT," +
                "\"zotero_key\" VARCHAR PRIMARY KEY, \"parent\" VARCHAR )";
        db.execSQL(CREATE_TABLE_ATTACHMENTS);
    }

    public void deleteTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
    }

    public static ContentValues getValues (ZoteroAttachment attachment) {
        ContentValues values = new ContentValues();
        values.put("zotero_key", attachment.get_zotero_key());
        values.put("file_type", attachment.get_file_type());
        values.put("file_name", attachment.get_file_name());
        values.put("parent", attachment.get_parent());

        return values;
    }

    public static ZoteroAttachment getAttachmentFromValues(ContentValues values) {
        ZoteroAttachment attachment = new ZoteroAttachment();
        attachment.set_zotero_key((String)values.get("zotero_key"));
        attachment.set_file_type((String)values.get("file_type"));
        attachment.set_file_name((String)values.get("file_name"));
        attachment.set_parent((String)values.get("parent"));

        return attachment;
    }

    public static void writeAttachment (ZoteroAttachment attachment, SQLiteDatabase db) {
        ContentValues values = getValues(attachment);
        db.insert(get_table_name(), null, values);
    }

}
