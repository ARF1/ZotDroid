package uk.co.section9.zotdroid.data.tables;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import uk.co.section9.zotdroid.data.BaseData;
import uk.co.section9.zotdroid.data.zotero.Collection;

/**
 * Created by oni on 11/07/2017.
 */

public class Collections extends BaseData {

    protected final String TABLE_NAME = "collections";

    protected final String TAG= "Collections";

    public void createTable(SQLiteDatabase db) {
        String CREATE_TABLE_COLLECTIONS = "CREATE TABLE \"" +TABLE_NAME + "\" (\"title\" TEXT, " +
                "\"zotero_key\" VARCHAR PRIMARY KEY, \"parent\" VARCHAR, \"version\" VARCHAR )";
        db.execSQL(CREATE_TABLE_COLLECTIONS);
    }

    public void deleteTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
    }

    public ContentValues getValues(Collection collection) {
        ContentValues values = new ContentValues();
        values.put("zotero_key", collection.get_zotero_key());
        values.put("title", collection.get_title());
        values.put("parent",collection.get_parent());
        values.put("version",collection.get_version());
        return values;
    }

    /**
     * Take a collection and write it to the database
     * @param collection
     */
    public void writeCollection(Collection collection, SQLiteDatabase db) {
        ContentValues values = getValues(collection);
        db.insert(get_table_name(), null, values);
    }

    public Collection getCollectionFromValues(ContentValues values) {
        Collection collection = new Collection();
        collection.set_title((String)values.get("title"));
        collection.set_zotero_key((String)values.get("zotero_key"));
        collection.set_parent((String)values.get("parent"));
        collection.set_version((String)values.get("version"));
        return collection;
    }

    public String get_table_name(){
        return TABLE_NAME;
    }

    public boolean collectionExists(Collection c, SQLiteDatabase db){
        String q =  "select count(*) from \"" + get_table_name() + "\" where zotero_key=\"" + c.get_zotero_key() + "\";";
        return exists(q,db);
    }

    public Collection getCollection(String key, SQLiteDatabase db){
        ContentValues values = getSingle(db,key);
        return getCollectionFromValues(values);
    }

    public void deleteCollection(Collection c, SQLiteDatabase db){
        db.execSQL("DELETE FROM " + get_table_name() + " WHERE zotero_key=\"" + c.get_zotero_key() + "\";");
    }

    public void updateCollection(Collection collection, SQLiteDatabase db) {
        db.execSQL("UPDATE " + get_table_name() +
                " SET title=\"" + collection.get_title() + "\", " +
                "parent=\"" + collection.get_parent() + "\", " +
                "version=\"" + collection.get_version() + "\" " +
                "WHERE zotero_key=\"" + collection.get_zotero_key() + "\";");
    }

}
