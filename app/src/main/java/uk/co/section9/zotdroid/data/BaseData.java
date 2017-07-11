package uk.co.section9.zotdroid.data;

/**
 * Created by oni on 11/07/2017.
 */

public class BaseData {

    protected static final String TABLE_NAME = "";

    public static String get_table_name() {
        return TABLE_NAME;
    }

    public static String createTable(){
        return "";
    }

    public static String deleteTable(){
        return ("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
