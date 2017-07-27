package uk.co.section9.zotdroid.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by oni on 14/07/2017.
 */

// TODO - We should add some inheritence here as many functions are the same across tables

public class AttachmentsTable extends BaseData {

    public static final String TAG = "zotdroid.data.AttachmentsTable";

    protected static final String TABLE_NAME = "attachments";

    public static String get_table_name(){
        return TABLE_NAME;
    }

    public static void createTable(SQLiteDatabase db) {
        String CREATE_TABLE_ATTACHMENTS = "CREATE TABLE \"" +TABLE_NAME + "\" ( \"file_type\" VARCHAR, \"file_name\" TEXT," +
                "\"zotero_key\" VARCHAR PRIMARY KEY, \"parent\" VARCHAR, \"version\" VARCHAR)";
        db.execSQL(CREATE_TABLE_ATTACHMENTS);
    }

    public static void deleteTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
    }

    public static ContentValues getValues (ZoteroAttachment attachment) {
        ContentValues values = new ContentValues();
        values.put("zotero_key", attachment.get_zotero_key());
        values.put("file_type", attachment.get_file_type());
        values.put("file_name", attachment.get_file_name());
        values.put("parent", attachment.get_parent());
        values.put("version", attachment.get_version());
        return values;
    }

    public static ZoteroAttachment getAttachmentFromValues(ContentValues values) {
        ZoteroAttachment attachment = new ZoteroAttachment();
        attachment.set_zotero_key((String)values.get("zotero_key"));
        attachment.set_file_type((String)values.get("file_type"));
        attachment.set_file_name((String)values.get("file_name"));
        attachment.set_parent((String)values.get("parent"));
        attachment.set_version((String)values.get("version"));
        return attachment;
    }

    public static String attachmentExists(String key){
        return "select count(*) from \"" + get_table_name() + "\" where zotero_key=\"" + key + "\";";
    }

    public static void writeAttachment (ZoteroAttachment attachment, SQLiteDatabase db) {
        ContentValues values = getValues(attachment);
        db.insert(get_table_name(), null, values);
    }

}
