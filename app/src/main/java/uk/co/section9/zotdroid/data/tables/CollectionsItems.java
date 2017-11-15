package uk.co.section9.zotdroid.data.tables;

/**
 * Created by oni on 26/07/2017.
 */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Vector;

import uk.co.section9.zotdroid.data.BaseData;
import uk.co.section9.zotdroid.data.zotero.Collection;
import uk.co.section9.zotdroid.data.zotero.CollectionItem;
import uk.co.section9.zotdroid.data.zotero.Record;

/**
 * Created by oni on 11/07/2017.
 */

public class CollectionsItems extends BaseData {

    protected final String TABLE_NAME = "collections_items";

    public void createTable(SQLiteDatabase db) {
        String CREATE_TABLE_COLLECTIONS = "CREATE TABLE \"" +TABLE_NAME + "\" ( \"collection\" VARCHAR, \"item\" VARCHAR )";
        db.execSQL(CREATE_TABLE_COLLECTIONS);
    }

    public void deleteTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + get_table_name());
    }

    public ContentValues getValues(CollectionItem ic) {
        ContentValues values = new ContentValues();
        values.put("item", ic.get_item());
        values.put("collection", ic.get_collection());
        return values;
    }

    /**
     * Take a collection and write it to the database
     * @param ic
     */
    public void writeCollection(CollectionItem ic, SQLiteDatabase db) {
        ContentValues values = getValues(ic);
        db.insert(get_table_name(), null, values);
    }

    public static CollectionItem getCollectionItemFromValues(ContentValues values) {
        CollectionItem ic = new CollectionItem();
        ic.set_collection((String)values.get("collection"));
        ic.set_item((String)values.get("item"));
        return ic;
    }

    public void deleteByRecord(String key, SQLiteDatabase db){
        db.execSQL("DELETE FROM " + get_table_name() + " where item=\"" + key + "\"");
    }

    public void deleteByCollection(String key, SQLiteDatabase db){
        db.execSQL("DELETE FROM " + get_table_name() + " where collection=\"" + key + "\"");
    }

    public String get_table_name(){
        return TABLE_NAME;
    }


    public Vector<CollectionItem> getItemsForCollection (String key, SQLiteDatabase db) {
        Vector<CollectionItem> citems = new Vector<>();
        ContentValues values = new ContentValues();
        Cursor cursor = db.rawQuery("select * from \"" + get_table_name() + "\" where collection=\"" + key + "\";", null);
        while (cursor.moveToNext()){
            values.clear();
            for (int i = 0; i < cursor.getColumnCount(); i++){
                values.put(cursor.getColumnName(i),cursor.getString(i));
            }
            citems.add(getCollectionItemFromValues(values));
        }

        cursor.close();
        return citems;
    }

    public Vector<CollectionItem> getCollectionItemForItem(String key, SQLiteDatabase db) {

        Vector<CollectionItem> citems = new Vector<>();
        ContentValues values = new ContentValues();
        Cursor cursor = db.rawQuery("select * from \"" + get_table_name() + "\" where item=\"" + key + "\";", null);
        while (cursor.moveToNext()){
            values.clear();
            for (int i = 0; i < cursor.getColumnCount(); i++){
                values.put(cursor.getColumnName(i),cursor.getString(i));
            }
            citems.add(getCollectionItemFromValues(values));
        }

        cursor.close();
        return citems;
    }
}
