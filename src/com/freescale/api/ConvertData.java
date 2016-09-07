package com.freescale.api;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;

import app.entity.DataCollection;

/**
 *
 * @author B13713
 */
public class ConvertData {

    public static final Charset GBK = Charset.forName("GBK");

    public static String decode(String str) throws UnsupportedEncodingException {
        return URLDecoder.decode(str.replace("\\x", "%"), GBK.name());
    }

    public static String encode(String str) throws UnsupportedEncodingException {
        String strEncode = URLEncoder.encode(str, GBK.name());
        return strEncode;
    }

    public static String convertDatetoPython(String dateString) {
        //dateString format:yyyy/mm/dd hh24:mi:ss" format
        String yy = String.format("%4d", Integer.valueOf(dateString.substring(0, 4)));
        String mm = String.format("%d", Integer.valueOf(dateString.substring(5, 7)));
        String dd = String.format("%d", Integer.valueOf(dateString.substring(8, 10)));
        String hh = String.format("%d", Integer.valueOf(dateString.substring(11, 13)));
        String mn = String.format("%d", Integer.valueOf(dateString.substring(14, 16)));
        String ss = String.format("%d", Integer.valueOf(dateString.substring(17, 19)));
        String pythonDate = "(" + yy + ", " + mm + ", " + dd + ", " + hh + ", " + mn + ", " + ss + ")";
        return pythonDate;
    }

    public static String replaceNoneToNull(String str) {
        if (str.trim().equals("None")) {
            str = "";
        }
        return str;
    }
     public static DataCollection sortDataCollection(DataCollection dataColection, int columnIndex, Boolean sortDescending) {
        for (int i = 1; i < dataColection.size(); i++) {
            ArrayList<String> partialLotCurrent = new ArrayList<String>();
            partialLotCurrent = dataColection.get(i);
            for (int j = i - 1; j >= 0; j--) {
                if (sortDescending) {
                    if (Integer.valueOf(partialLotCurrent.get(columnIndex)) > Integer.valueOf(dataColection.get(j).get(columnIndex))) {
                        ArrayList partialLot = new ArrayList();
                        partialLot = dataColection.get(j);
                        dataColection.set(j, partialLotCurrent);
                        dataColection.set(j + 1, partialLot);
                    }
                } else {
                    if (Integer.valueOf(partialLotCurrent.get(columnIndex)) < Integer.valueOf(dataColection.get(j).get(columnIndex))) {
                        ArrayList partialLot = new ArrayList();
                        partialLot = dataColection.get(j);
                        dataColection.set(j, partialLotCurrent);
                        dataColection.set(j + 1, partialLot);
                    }
                }
            }
        }
        return dataColection;
    }

}
