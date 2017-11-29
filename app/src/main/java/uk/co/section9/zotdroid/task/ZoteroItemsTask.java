package uk.co.section9.zotdroid.task;

/**
 * Created by oni on 21/07/2017.
 */

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

import uk.co.section9.zotdroid.Util;
import uk.co.section9.zotdroid.auth.ZoteroBroker;
import uk.co.section9.zotdroid.data.zotero.Attachment;
import uk.co.section9.zotdroid.data.zotero.Author;
import uk.co.section9.zotdroid.data.zotero.Note;
import uk.co.section9.zotdroid.data.zotero.Record;
import uk.co.section9.zotdroid.data.zotero.Tag;

/**
 * Extend our items task so that we can do some processing on the data returned
 * and then call the callback.
 */
public class ZoteroItemsTask extends ZoteroTask {

    private static final String TAG = "ZoteroItemsTask";

    ZoteroTaskCallback callback;
    int startItem = 0;
    int itemLimit = 25; // Seems to be what Zotero likes by default (despite the API)
    String _url = "";
    private boolean _reset_mode = true;

    public ZoteroItemsTask(ZoteroTaskCallback callback, int start, int limit) {
        this.callback = callback;
        this.startItem = start;
        this.itemLimit = limit;
        _reset_mode = true;
        _url = BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/items?start=" + Integer.toString(this.startItem);
    }

    public ZoteroItemsTask(ZoteroTaskCallback callback, Vector<String> keys) {
        this.callback = callback;
        _reset_mode = false;
        _url = BASE_URL + "/users/" + ZoteroBroker.USER_ID + "/items?itemKey=";

        for (String key: keys){
            _url += key + ",";
        }
    }

    /**
     * Actually execute the async task.
     * Not quite an override but it sets up the string for us when we do actually execute, so things are in sync
     * For some reason, it only works if I pass the start (and possibly limit) as URL params instead of headers
     * but the desc and dateAdded seem ok. It could be an integer thing I suspect
     */
    public void startZoteroTask(){
        if (_reset_mode) {
            execute(_url,
                    "start", Integer.toString(this.startItem),
                    "limit", Integer.toString(this.itemLimit),
                    "direction", "desc",
                    "sort", "dateAdded");
        } else {
            execute(_url,
                    "direction", "desc",
                    "sort", "dateAdded");
        }
    }

    protected Record processEntry(JSONObject jobj) {
        Record record = new Record();
        // TODO - We need to handle these exceptions better - possibly by just ignoring this record

        try {
            record.set_zotero_key(jobj.getString("key"));
        } catch (JSONException e){
            // We should always have a key. If we dont then bad things :S
        }

        try {
            record.set_title(jobj.getString("title"));
        } catch (JSONException e) {
            record.set_title("No title");
        }

        try {
            String td = jobj.getString("dateAdded");
            //record.set_date_added(Util.jsonStringToDate(td));
            record.set_date_added(td);

        } catch (JSONException e) {
            // Pass - go with today's date
        }

        try {
            String td = jobj.getString("dateModified");
            //record.set_date_modified(Util.jsonStringToDate(td));
            record.set_date_modified(td);
        } catch (JSONException e) {
            // Pass - go with today's date
        }

        try {
            JSONArray tags = jobj.getJSONArray("tags");
            for ( int i = 0; i < tags.length(); i++){
                record.add_tag(new Tag(tags.getJSONObject(i).getString("tag"), record.get_zotero_key()) );
            }
        } catch (JSONException e) {
            // pass - no tags
        }

        try {
            JSONArray notes = jobj.getJSONArray("notes");
            for ( int i = 0; i < notes.length(); i++){
                //record.add_note(new Note(notes.getJSONObject(i).getString("tag"));
            }
        } catch (JSONException e) {
            // pass - no tags
        }

        try {
            JSONArray authors = jobj.getJSONArray("creators");
            for (int i =0; i < authors.length(); i++) {
                JSONObject creator = authors.getJSONObject(i);
                // TODO - Can we ALWAYS guarantee we have the current record zotero key?
                record.add_author(new Author(creator.getString("lastName") + ", " + creator.getString("firstName"), record.get_zotero_key()));
            }
        } catch (JSONException e){
            // pass, no authors
        }

        try {
            record.set_parent(jobj.getString("parent"));
        } catch (JSONException e){
            record.set_parent("");
        }

        try {
            record.set_item_type(jobj.getString("itemType"));
        } catch (JSONException e){
            record.set_item_type("");
        }

        try {
            record.set_version(jobj.getString("version"));
        } catch (JSONException e){
            record.set_version("0000");
        }

        try {
            JSONArray collections = jobj.getJSONArray("collections");
            for (int i=0; i < collections.length(); i++) {
                try {
                    String tj = collections.getString(i);
                    record.addTempCollection(tj);
                } catch (JSONException e) {
                }
            }

        } catch (JSONException e) {
        }

        return record;
    }

    protected Note processNote(JSONObject jobj) {
        // TODO - complete this
        try {
            return new Note(jobj.getString("key"), jobj.getString("parentItem"), jobj.getString("note"), jobj.getString("version"));
        } catch (JSONException e){
        }
        return new Note("","","","0000"); // TODO - some better error handling
    }


    protected Attachment processAttachment(JSONObject jobj) {
        Attachment attachment = new Attachment();

        try {
            attachment.set_zotero_key(jobj.getString("key"));
        } catch (JSONException e){
            // We should always have a key. If we dont then bad things :S
        }

        try {
            attachment.set_file_name(jobj.getString("filename"));
        } catch (JSONException e){
        }

        // TODO - some attachments are top level - do we show these?
        try {
            attachment.set_parent(jobj.getString("parentItem"));
        } catch (JSONException e){

        }

        try {
            attachment.set_version(jobj.getString("version"));
        } catch (JSONException e){
            attachment.set_version("0000");
        }

        try {
            attachment.set_file_type(jobj.getString("contentType"));
        } catch (JSONException e){
        }

        return attachment;
    }

    protected void onPostExecute(String rstring) {
        Vector<Record> records =  new Vector<Record>();
        Vector<Attachment> attachments =  new Vector<Attachment>();
        Vector<Note> notes =  new Vector<Note>();

        // Check we didn't get a failure on that rsync call
        if (rstring == "FAIL"){
            callback.onItemsCompletion(false, rstring, "0000");
            return;
        }

        try {
            JSONObject jObject = new JSONObject(rstring);

            // Grab the totals and the versions from the headers we found
            int total = 0;
            try {
                total = jObject.getInt("Total-Results");
            } catch (JSONException e) {
                Log.i(TAG,"No Total-Results in request.");
            }

            String version = "0000";
            try {
                version = jObject.getString("Last-Modified-Version");
            } catch (JSONException e) {
                Log.i(TAG,"No Last-Modified-Version in request.");
            }

            JSONArray jArray = jObject.getJSONArray("results");

            for (int i=0; i < jArray.length(); i++) {
                try {
                    JSONObject jobjtop = jArray.getJSONObject(i);
                    JSONObject jobj = jobjtop.getJSONObject("data");
                    if (jobj.getString("itemType").contains("attachment")){
                        attachments.add(processAttachment(jobj));
                    } else if (jobj.getString("itemType").contains("note")) {
                        notes.add(processNote(jobj));
                    } else {
                        records.add(processEntry(jobj));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.onItemCompletion(false, "", 0, total, null, null, null, "0000");
                    return;
                }
            }

            if (_reset_mode) {
                callback.onItemCompletion(true, "", startItem + jArray.length(), total, records, attachments, notes, version);
            } else {
                callback.onItemCompletion(true, "", records, attachments, notes, version);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"Error in parsing JSON Object.");
            callback.onItemsCompletion(false,"Erro in parsing JSON Object.", "0000");
        }
    }
}
