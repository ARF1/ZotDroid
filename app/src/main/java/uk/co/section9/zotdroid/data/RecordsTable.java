package uk.co.section9.zotdroid.data;

import java.util.Date;
import java.util.Vector;

/**
 * Created by oni on 11/07/2017.
 */

public class RecordsTable extends BaseData {

    // TODO - Make this a tree like structure and return these?
    public static class ZoteroRecord {
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }



        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }

        public Date getDateAdded() {
            return dateAdded;
        }

        public void setDateAdded(Date dateAdded) {
            this.dateAdded = dateAdded;
        }

        // For now, we cover all the bases we need for all possible items
        // Eventually we might have separate record tables

        protected String    contentType;
        protected String    title;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        protected String    fileName;
        protected String    itemType;
        protected Date      dateAdded;
        protected String    author; // TODO - Just one for now but we will add more
        protected String    key;
        protected String    parent;
        public String toString() {
            return title + " - " + author;
        }
    }

    protected static final String TABLE_NAME = "records";
}
