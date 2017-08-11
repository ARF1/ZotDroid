package uk.co.section9.zotdroid;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by oni on 14/07/2017.
 */

// W3C Date JSON Standard - https://www.w3.org/TR/NOTE-datetime
// https://stackoverflow.com/questions/2597083/illegal-pattern-character-t-when-parsing-a-date-string-to-java-util-date#25979660

public class Util {

    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    public static final String DB_DATE_FORMAT = "yyyy-MM-dd HH:mm:ssXXX";

    public static Date jsonStringToDate(String s) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        // This effectively removes the 'T' that the DB complains about.
        try {
            return dateFormat.parse(s);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date();
    }

    public static String dateToDBString(Date d){
        SimpleDateFormat dateFormat = new SimpleDateFormat(DB_DATE_FORMAT, Locale.getDefault());
        String dd = dateFormat.format(d);
        return dd;
    }

    public static Date dbStringToDate(String s) {
        // For some stupid Java reason we need to remove the T in our format
        SimpleDateFormat dateFormat = new SimpleDateFormat(DB_DATE_FORMAT, Locale.getDefault());
        try {
            return dateFormat.parse(s);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date();
    }

    public static void copyDate(Date from, Date to){
        to.setDate(from.getDate());
        to.setTime(from.getTime());
    }
}
