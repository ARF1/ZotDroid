package uk.co.section9.zotdroid;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by oni on 14/07/2017.
 */

public class Util {

    public static String dateToDBString(Date d){
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String dd = dateFormat.format(d);
        return dd;
    }

    public static Date dbStringToDate(String s) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            return dateFormat.parse(s);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date(1970,1,1);
    }

    public static void copyDate(Date from, Date to){
        to.setDate(from.getDate());
        to.setTime(from.getTime());
    }
}
