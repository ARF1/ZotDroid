package uk.co.section9.zotdroid;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import uk.co.section9.zotdroid.data.zotero.Attachment;
import uk.co.section9.zotdroid.data.zotero.Collection;
import uk.co.section9.zotdroid.data.zotero.Note;
import uk.co.section9.zotdroid.data.zotero.Record;

/**
 * Created by oni on 15/11/2017.
 */

/**
 * A class designed to hold our current working memory 'snapshot' of the underlying database.
 * TODO - this might be better as a sort of *memory pool* kind of approach?
 */
public class ZotDroidMem {

    public  Vector<Record>          _records            = new Vector<Record>();
    public  Vector<Attachment>      _attachments        = new Vector<Attachment>();
    public  Map<String,Record>      _key_to_record      = new HashMap<String, Record>();
    public  Vector<Collection>      _collections        = new Vector<Collection>();
    public  Vector<Note>            _notes              = new Vector<Note>();
    public  Map<String, Collection> _key_to_collection  = new HashMap<>();

    // Used for pagination of the records. For now, we ALWAYS start at 0 and only ever expand
    public int _start_index = 0;
    public int _end_index = 0;

    /**
     * Erase all the currently held records, collections etc, in memory.
     */
    public  void nukeMemory(){
        _records.clear();
        _collections.clear();
        _attachments.clear();
        _key_to_record.clear();
        _notes.clear();
        _key_to_collection.clear();
        _end_index = 0;
    }

}
