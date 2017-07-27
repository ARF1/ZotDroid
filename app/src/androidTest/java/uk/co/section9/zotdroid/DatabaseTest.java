package uk.co.section9.zotdroid;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import uk.co.section9.zotdroid.data.RecordsTable;
import uk.co.section9.zotdroid.data.ZotDroidDB;
import uk.co.section9.zotdroid.data.ZoteroRecord;
import uk.co.section9.zotdroid.data.ZoteroSummary;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("uk.co.section9.zotdroid", appContext.getPackageName());
    }

    @Test
    public void testRecordDB() throws Exception {
        // Context of the app under test.
        // Apparently this test runs on a device - instrumentation tests essentially
        Context appContext = InstrumentationRegistry.getTargetContext();
        ZotDroidDB db = new ZotDroidDB(appContext);

        ZoteroRecord record = new ZoteroRecord();
        record.set_version("1234");
        record.set_author("author");
        record.set_content_type("pdf");
        record.set_date_added(new Date());
        record.set_parent("abcd");
        record.set_title("title");
        record.set_item_type("type");

        db.writeRecord(record);

        int numrow = db.getNumRows(RecordsTable.get_table_name());

        ZoteroRecord r2 = RecordsTable.getRecordFromValues(db.readRow(RecordsTable.get_table_name(),numrow-1));

        assertEquals(r2.get_version(),"1234");
        assertEquals(r2.get_author(),"author");
        assertEquals(r2.get_content_type(),"pdf");
        assertEquals(r2.get_parent(),"abcd");
        assertEquals(r2.get_title(),"title");
        assertEquals(r2.get_item_type(),"type");

    }

    @Test
    public void testSummaryDB() throws Exception {

        Context appContext = InstrumentationRegistry.getTargetContext();
        ZotDroidDB db = new ZotDroidDB(appContext);

        ZoteroSummary s = new ZoteroSummary();

        db.writeSummary(s);

        ZoteroSummary summary = db.getSummary();
        assertEquals(summary.get_last_version_collections(),"0000");
        assertEquals(summary.get_last_version_items(),"0000");

        s.set_last_version_items("1234");
        s.set_last_version_collections("5678");
        db.writeSummary(s);

        summary = db.getSummary();
        assertEquals(summary.get_last_version_items(),"1234");
        assertEquals(summary.get_last_version_collections(),"5678");
    }
}
