package uk.co.section9.zotdroid.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.util.Vector;

import uk.co.section9.zotdroid.Util;

/**
 * Created by oni on 11/07/2017.
 */

public class CollectionsTable extends BaseData {

    protected static final String TABLE_NAME = "collections";

    protected ZotDroidDB _db;

    public CollectionsTable(ZotDroidDB db){
        this._db = db;
    }

    public static void createTable(SQLiteDatabase db) {
        String CREATE_TABLE_COLLECTIONS = "CREATE TABLE \"" +TABLE_NAME + "\" (\"title\" TEXT, \"author\" TEXT, " +
                "\"zotero_key\" VARCHAR PRIMARY KEY, \"parent\" VARCHAR )";
        db.execSQL(CREATE_TABLE_COLLECTIONS);
    }

    public static void deleteTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
    }

    public static ContentValues getValues(ZoteroCollection collection) {
        ContentValues values = new ContentValues();
        values.put("zotero_key", collection.get_zotero_key());
        values.put("title", collection.get_title());
        values.put("parent",collection.get_parent());
        return values;
    }

    /**
     * Take a collection and write it to the database
     * @param collection
     */
    public static void writeCollection( ZoteroCollection collection, SQLiteDatabase db) {
        ContentValues values = getValues(collection);
        db.insert(get_table_name(), null, values);
    }

    public static ZoteroCollection getCollectionFromValues(ContentValues values) {
        ZoteroCollection collection = new ZoteroCollection();
        collection.set_title((String)values.get("title"));
        collection.set_zotero_key((String)values.get("zotero_key"));
        collection.set_parent((String)values.get("parent"));
        return collection;
    }

    public static String get_table_name(){
        return TABLE_NAME;
    }

}
