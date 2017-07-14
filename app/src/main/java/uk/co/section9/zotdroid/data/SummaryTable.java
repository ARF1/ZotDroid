package uk.co.section9.zotdroid.data;

/**
 * Created by oni on 11/07/2017.
 */

public class SummaryTable extends BaseData {

    protected static final String TABLE_NAME = "summary";

    protected ZotDroidDB _db;

    public SummaryTable(ZotDroidDB db){
        this._db = db;
    }

    public static String get_table_name(){
        return TABLE_NAME;
    }
}
