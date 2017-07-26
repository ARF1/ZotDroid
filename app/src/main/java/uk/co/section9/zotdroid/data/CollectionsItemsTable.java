package uk.co.section9.zotdroid.data;

/**
 * Created by oni on 26/07/2017.
 */

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;


import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.util.Vector;

import uk.co.section9.zotdroid.Util;

/**
 * Created by oni on 11/07/2017.
 */

public class CollectionsItemsTable extends BaseData {

    protected static final String TABLE_NAME = "collections_items";

    protected ZotDroidDB _db;

    public CollectionsItemsTable(ZotDroidDB db){
        this._db = db;
    }

    public static void createTable(SQLiteDatabase db) {
        String CREATE_TABLE_COLLECTIONS = "CREATE TABLE \"" +TABLE_NAME + "\" ( \"collection\" VARCHAR, \"item\" VARCHAR )";
        db.execSQL(CREATE_TABLE_COLLECTIONS);
        //String CREATE_TABLE_INDEX = "CREATE INDEX colindex ON  \"" +TABLE_NAME + "\" ( \"collection\" )";
        //db.execSQL(CREATE_TABLE_INDEX);
    }

    public static void deleteTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
    }

    public static ContentValues getValues(ZoteroCollectionItem ic) {
        ContentValues values = new ContentValues();
        values.put("item", ic.get_item());
        values.put("collection", ic.get_collection());
        return values;
    }

    /**
     * Take a collection and write it to the database
     * @param ic
     */
    public static void writeCollection( ZoteroCollectionItem ic, SQLiteDatabase db) {
        ContentValues values = getValues(ic);
        db.insert(get_table_name(), null, values);
    }

    public static ZoteroCollectionItem getCollectionFromValues(ContentValues values) {
        ZoteroCollectionItem ic = new ZoteroCollectionItem();
        ic.set_collection((String)values.get("collection"));
        ic.set_item((String)values.get("item"));
        return ic;
    }

    public static String get_table_name(){
        return TABLE_NAME;
    }

}
