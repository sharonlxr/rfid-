package com.freescale.api;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
*
* @author RF312C
*/
public class DateFormatter {
	private final static DateFormat sqlDateFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a");
    private final static DateFormat sqlSimpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy");
    private final static DateFormat webDateFormat = new SimpleDateFormat("MM/dd/yyyy");
    private final static String sqlDateFormat1 = "mm-dd-yyyy";
    private final static String sqlDateFormat2 = "DD-Mon-YYYY hh:mi:ss pm";
    private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    //added  by Becky for MPN print in lot merge
    private final static DateFormat apiPrintDateFormat = new SimpleDateFormat("MMddyy");

    public static String getApiPrintDateFormat() {
        return apiPrintDateFormat.format(new Date());
    }

    public static String getServerCurrentDate() {
        return sqlDateFormat.format(new Date());
    }

    public static String getloggedOnTime() {
        return simpleDateFormat.format(new Date());
    }
//    public static String getCurrentDate() {
//        return null;
//    }
//
//    public static String formatDate(Date date) {
//        if (date == null) {
//            return null;
//        }
//        return dateFormat.format(date);
//    }

    public static String getSqlDateFormat1() {
        return sqlDateFormat1;
    }

    public static String getSqlDateFormat2() {
        return sqlDateFormat2;
    }

    public static Date getWebDateToDate(String value) throws ParseException {
        return (Date) webDateFormat.parse(value);
    }

    public static String getWebDateToString(Date date) {
        if (date == null) {
            return null;
        }
        return webDateFormat.format(date);
    }

    public static Date getSQLDateToDate(String value) throws ParseException {
        return (Date) sqlDateFormat.parse(value);
    }

    public static String getSQLDateToString(Date date) {
        if (date == null) {
            return null;
        }
        return sqlDateFormat.format(date);
    }

    public static Date getSQLSimpleDateToDate(String value) throws ParseException {
        return (Date) sqlSimpleDateFormat.parse(value);
    }

    public static String getSQLSimpleDateToString(Date date) {
        if (date == null) {
            return null;
        }
        return sqlSimpleDateFormat.format(date);
    }

    public static Date getSimpleDateToDate(String value) throws ParseException {
        return (Date) simpleDateFormat.parse(value);
    }
}
