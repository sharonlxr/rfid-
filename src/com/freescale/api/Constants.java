package com.freescale.api;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.annotation.SuppressLint;

public class Constants {
	public static final String PASSWORD = "4311";
	public static final String USER_ID = "USER_ID";
	public static final String FIRST_NAME = "FIRST_NAME";
	public static final String LAST_NAME = "LAST_NAME";
	public static final String DEPARTMENT = "DEPARTMENT";
	public static final String SERVER_CURRENT_DATE = "SERVER_CURRENT_DATE";
	public static final String ITEM_TITLE = "ItemTitle";
	public static final String ITEM_TEXT = "ItemText";
	public static final String ALOT_NUMBER = "ALOT_NUMBER";
	public static final String TAG_UUID = "TAG_UUID";
	public static final String CONFIG_TEST = "Testing.properties";
	public static final String CONFIG_PROD = "Production.properties";
	@SuppressLint("SimpleDateFormat")
	public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final String FILTER_STRING_HOST = "com.freescale.cimei.rfid.host.msg";
	public static final String FILTER_STRING_WIFI = "com.freescale.cimei.rfid.wifi.msg";
	public static final String SCAN_TARGET = "SCAN_TARGET";
	public static final String SCAN_TARGET_INIT = "SCAN_TARGET_INIT";
	public static final String SCAN_TARGET_ASSIGN = "SCAN_TARGET_ASSIGN";
	public static final String SCAN_TARGET_ASSIGN_INPUT = "SCAN_TARGET_ASSIGN_INPUT";
	public static final String SCAN_TARGET_ASSIGN_OUTPUT = "SCAN_TARGET_ASSIGN_OUTPUT";
	public static final String SCAN_TARGET_DEASSIGN = "SCAN_TARGET_DEASSIGN";
	public static final String SCAN_TARGET_DEASSIGN_INPUT = "SCAN_TARGET_DEASSIGN_INPUT";
	public static final String SCAN_TARGET_DEASSIGN_OUTPUT = "SCAN_TARGET_DEASSIGN_OUTPUT";
	public static final String SCAN_TARGET_RACK_IN = "SCAN_TARGET_RACK_IN";
	public static final String SCAN_TARGET_RACK_OUT = "SCAN_TARGET_RACK_OUT";
	public static final String SCAN_TARGET_RACK_INIT = "SCAN_TARGET_RACK_INIT";
	public static final String SCAN_TARGET_EMT_LOG = "SCAN_TARGET_EMT_LOG";
	public static final String SCAN_TARGET_IN_OUT_ASSIGN_IN = "SCAN_TARGET_IN_OUT_ASSIGN_IN";
	public static final String SCAN_TARGET_IN_OUT_ASSIGN_OUT = "SCAN_TARGET_IN_OUT_ASSIGN_OUT";
	public static final String SCAN_TARGET_LOGIN = "SCAN_TARGET_LOGIN";
	public static final String SCAN_TARGET_LOT_INQUIRY = "SCAN_TARGET_LOT_INQUIRY";
	public static final String SCAN_TARGET_END_LOT = "SCAN_TARGET_END_LOT";
	public static final String SCAN_TARGET_CHECK_MAGAZINE_INIT = "SCAN_TARGET_CHECK_MAGAZINE_INIT";
	public static final String SCAN_TARGET_CHECK_MAGAZINE = "SCAN_TARGET_CHECK_MAGAZINE";
	public static final String SCAN_TARGET_REJ = "SCAN_TARGET_REJ";
	public static final String SCAN_TARGET_CASSETTE = "SCAN_TARGET_CASSETTE";
	public static final String SCAN_TARGET_CASSETTE_ASSIGN = "SCAN_TARGET_CASSETTE_ASSIGN";
	public static final String SCAN_TARGET_LOT_CARRIER_TRACKING = "SCAN_TARGET_LOT_CARRIER_TRACKING";
	public static final String SCAN_TARGET_SI_INIT = "SI_SCAN_TARGET_INIT";
	public static final String SCAN_TARGET_SI_ASSIGN = "SI_SCAN_TARGET_ASSIGN";
	public static final String SCAN_TARGET_SI_DEASSIGN = "SI_SCAN_TARGET_DEASSIGN";
	public static final String SCAN_TARGET_LOT_CARRIER_HIST = "SCAN_TARGET_LOT_CARRIER_HIST";
	public static final String SCAN_TARGET_DIV_LOT_INIT = "SCAN_TARGET_DIV_LOT_INIT";
	public static final String SCAN_TARGET_DIV_LOT = "SCAN_TARGET_DIV_LOT";
	public static final String SCAN_TARGET_DIV_LOT_PROGRESS = "SCAN_TARGET_DIV_LOT_PROGRESS";
	public static final String SCAN_TARGET_LOT_RACK_INIT = "SCAN_TARGET_LOT_RACK_INIT";
	public static final String SCAN_TARGET_PASS_WINDOW_IN = "SCAN_TARGET_PASS_WINDOW_IN";
	public static final String SCAN_TARGET_PASS_WINDOW_OUT = "SCAN_TARGET_PASS_WINDOW_OUT";
	public static final String SCAN_TARGET_PASS_WINDOW_INIT = "SCAN_TARGET_PASS_WINDOW_INIT";
	public static final String SCAN_TARGET_LOT_PASS_WINDOW = "SCAN_TARGET_LOT_PASS_WINDOW";
	public static final String SCAN_TARGET_REJ_MAG = "SCAN_TARGET_REJ_MAG";
	public static final String APP_NAME="RFID";
	public static int type = -1; // 0-unknown 1-N7 2-PDA 3-iData 4-Fangtong 5-Chilijuncheng
	public static int port = 5555;
	public static boolean carrierAssignInputOutput = false;
	public static String configFileName = null;//CONFIG_TEST;
	public static boolean manualDeassign = false;
	public static boolean msgFilter = true;
	public static boolean carrierAssignLoc = false;
	public static boolean alarmUnsetMenu = false;
	public static boolean autoLogout = false;
//	public static String serverName = "http://10.192.154.104:8080/RFID/";
	public static String serverName = "http://10.192.130.4:8085/RFID/"; // 04v
	public static final String TYPE_MSG = "msg";
	public static final String TYPE_STEP = "step";
	public static final String TYPE_ERROR = "error";
	public static final String TYPE_END = "endMach";
	public static final String TYPE_MISSING = "missingMag";
	public static final String LOG_FILE_ERR = "err";
	public static final String LOG_FILE_MSG = "message";
	public static final String LOG_FILE_WIFI = "Wifi";
	public final static String EVENT_CODE = "RFID_LotMagCheck";
	
}
