package uk.co.section9.zotdroid.task;

/**
 * Created by oni on 21/07/2017.
 */

import android.os.AsyncTask;


/**
 * Generic task that executes in the background, making requests of Zotero
 * and returning string data.
 */

public abstract class ZoteroTask extends AsyncTask<String,Integer,String> {

    public abstract void startZoteroTask();

}

