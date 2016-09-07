package com.fsl.cimei.rfid;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.MsgDBHelper;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidWifiException;

public class ControlActivity extends BaseActivity {

	private QueryTask qTask = null;
	private HostMsgReceiver hostMsgReceiver;
	private WifiMessageReceiver wifiMsgReceiver;

	private DataCollection assignedMachDC;
	private ArrayList<HashMap<String, Object>> machList = new ArrayList<HashMap<String, Object>>();
	private GridView machGridView;
	private Cursor cursor = null;
	private MsgDBHelper msgdb;

	// private SimpleAdapter machListAdapter;
	private MachGridAdapter machGridAdapter;
	private View mStatusView2;

	// private List<HashMap<String, String>> msgListItem = new ArrayList<HashMap<String, String>>();
	private int errorColor;
	private int stepColor;
	private int msgColor;

	private final String classname = "Control";
//	private int versionCode = 0;
	public static long lastAssignedMachCheck = 0;
	// private long lastAutoLogoutCheck = 0;
	private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA);
	// private Button startScanButton;
	// public static boolean isScanOpen = false;
	// public static boolean isStartOpen = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		global.setAndroidSecureID(Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID));
		if (Constants.type == -1) {
			if (android.os.Build.MODEL.startsWith("Nexus")) {
				Constants.type = 1;
			} else if (android.os.Build.MODEL.equals("ww808_emmc")) {
				Constants.type = 2;
			} else if (android.os.Build.MODEL.equals("Android")) {
				Constants.type = 3;
			} else if (android.os.Build.MODEL.equals("Android Handheld Terminal")) {
				Constants.port = 35505;
				Constants.type = 4;
			} else if (android.os.Build.MODEL.equals("HandHeld-1")) {
				Constants.type = 5;
			} else {
				Constants.type = 0;
			}
		}
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) { log("Build.VERSION.SDK_INT " + Build.VERSION.SDK_INT); }
		if (MainMenuActivity.selectedMenuMap == null || MainMenuActivity.selectedMenuMap.size() == 0) { // MainMenuActivity.hasMainMenu &&
			loadMainMenuConfig();
		}
		setContentView(R.layout.activity_control);
		errorColor = getResources().getColor(R.color.bg_red);
		stepColor = getResources().getColor(R.color.bg_blue);
		msgColor = getResources().getColor(R.color.bg_black);
		mFormView = findViewById(R.id.control_form);
		mStatusView = findViewById(R.id.control_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		mStatusView2 = findViewById(R.id.control_load_status_2);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.control_tb_fragment);
		super.initTagBarcodeInput();
		alotNumberTextView.setVisibility(View.GONE);
		assignedMachDC = new DataCollection();
		msgdb = new MsgDBHelper(getApplicationContext());
//		try {
//			versionCode = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionCode;
//		} catch (NameNotFoundException e) {
//			log(e.toString());
//		}
//		SharedPreferences shared = ControlActivity.this.getSharedPreferences("RFID-data", MODE_PRIVATE);
//		if (shared != null) {
//			int histVersion = shared.getInt("HISTORY_APP_VERSION", 0);
//			if (histVersion < versionCode) {
//				ControlActivity.this.deleteFile("config.db");
//				String errorMsg = CommonUtility.copyConfigFile(ControlActivity.this, "config.db");
//				if (CommonUtility.isEmpty(errorMsg)) {
//					// SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(ControlActivity.this.getFilesDir() + "/config.db", null);
//					Editor e = shared.edit();
//					e.putInt("HISTORY_APP_VERSION", versionCode);
//					e.commit();
//				}
//			}
//		}
		machGridAdapter = new MachGridAdapter(this);
		machGridView = (GridView) findViewById(R.id.control_mach_grid);
		machGridView.setAdapter(machGridAdapter);
		machGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> item = (HashMap<String, Object>) arg0.getItemAtPosition(arg2);
				final String mach = (String) item.get(Constants.ITEM_TEXT);
				if (item.containsKey(Constants.ITEM_TITLE)) {
					redirect((Integer) item.get(Constants.ITEM_TITLE));
				} else {
					cursor = msgdb.query(new String[] { "time", "content", "_id", "type" }, "mach=?", new String[] { mach }, null, null, "_id desc", null);
					// msgListItem.clear();
					// while (cursor.moveToNext()) {
					// String time = cursor.getString(0);
					// String content = cursor.getString(1);
					// String type = cursor.getString(3);
					// HashMap<String, String> map = new HashMap<String, String>();
					// map.put("time", time);
					// map.put("content", content);
					// map.put("type", type);
					// msgListItem.add(map);
					// }
					// cursor.close();
					String[] fromColumns = new String[] { "time", "content" };
					int[] toLayoutIDs = new int[] { R.id.itemTitle, R.id.itemText };
					final ListView detailView = new ListView(ControlActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					// final MsgAdapter msgAdapter = new MsgAdapter(ControlActivity.this);
					final MsgAdapter msgAdapter = new MsgAdapter(ControlActivity.this, R.layout.mach_msg_list_item, cursor, fromColumns, toLayoutIDs, 0);
					detailView.setAdapter(msgAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(ControlActivity.this);
					builder.setTitle(mach + " 历史消息").setIcon(android.R.drawable.ic_dialog_info).setView(detailView);
					// cursor = msgdb.query(new String[] { "_id", "type" }, "mach=? and type=?", new String[] { mach, Constants.TYPE_MISSING }, null, null, null, null);
					// if (cursor.moveToNext()) {
					// builder.setPositiveButton("漏扫弹夹：处理并开机", new DialogInterface.OnClickListener() {
					// @Override
					// public void onClick(DialogInterface dialog, int which) {
					// global.setScanTarget(Constants.SCAN_TARGET_REJ_MAG);
					// Intent intent = new Intent(ControlActivity.this, RejMagActivity.class);
					// startActivityForResult(intent, 5);
					// }
					// });
					// } else {
					// cursor = msgdb.query(new String[] { "_id", "type" }, "mach=? and type=?", new String[] { mach, Constants.TYPE_END }, null, null, null, null);
					// if (cursor.moveToNext()) {
					SharedPreferences data = ControlActivity.this.getSharedPreferences("RFID-data", Context.MODE_PRIVATE);
					boolean alarm = false;
					if (null != data) {
						Set<String> alarmMach = data.getStringSet("alarmMach", new HashSet<String>());
						if (alarmMach.contains(mach)) {
							alarm = true;
						}
					}
					if (alarm) {
						builder.setPositiveButton("开机", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (qTask == null) {
									qTask = new QueryTask();
									showProgress(true);
									qTask.execute("startMach", mach);
								}
							}
						});
						builder.setNeutralButton(" ", null);
					} else {
						builder.setPositiveButton(" ", null);
						builder.setNeutralButton(" ", null);
						// builder.setNeutralButton(getResources().getString(R.string.clear), new DialogInterface.OnClickListener() {
						// @Override
						// public void onClick(DialogInterface dialog, int which) {
						// msgdb.del(mach);
						// msgAdapter.notifyDataSetChanged();
						// }
						// });
					}
					// }
					builder.setNegativeButton(getResources().getString(R.string.close), null);
					builder.show();
				}
			}
		});

		hostMsgReceiver = new HostMsgReceiver();
		IntentFilter filterHost = new IntentFilter();
		filterHost.addAction(Constants.FILTER_STRING_HOST);
		registerReceiver(hostMsgReceiver, filterHost);
		wifiMsgReceiver = new WifiMessageReceiver();
		IntentFilter filterHost2 = new IntentFilter();
		filterHost2.addAction(Constants.FILTER_STRING_WIFI);
		registerReceiver(wifiMsgReceiver, filterHost2);

		Intent intent = new Intent(ControlActivity.this, MsgReceiveService.class);
		intent.putExtra("deviceID", global.getAndroidSecureID());
		startService(intent);
		// Intent intent1 = new Intent(ControlActivity.this, MsgReceiveService1.class);
		// intent1.putExtra("deviceID", global.getAndroidSecureID());
		// startService(intent1);
		// Intent intent2 = new Intent(ControlActivity.this, MsgReceiveService2.class);
		// startService(intent2);
		// Intent intent3 = new Intent(ControlActivity.this, MsgReceiveService3.class);
		// intent3.putExtra("deviceID", global.getAndroidSecureID());
		// startService(intent3);
		// Intent intent4 = new Intent(ControlActivity.this, MsgReceiveService4.class);
		// intent4.putExtra("deviceID", global.getAndroidSecureID());
		// startService(intent4);
		// Intent intent3 = new Intent(ControlActivity.this, WifiCheckService.class);
		// startService(intent3);
	}

	private void loadMainMenuConfig() {
		File file = Environment.getExternalStorageDirectory();
		File settingFile = new File(file.getAbsolutePath() + "/rfid-config/Main_Menu.conf");
		MainMenuActivity.hasMainMenu = false;
		MainMenuActivity.selectedMenuMap = new SparseBooleanArray();// new HashMap<Integer, Boolean>();
		if (settingFile.exists()) {
			Properties result = new Properties();
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(settingFile);
				result.load(fis);
			} catch (Exception e) {
				log("CommonUtility readConfig " + e.toString());
			} finally {
				if (null != fis) {
					try {
						fis.close();
					} catch (IOException e) {
						log("CommonUtility readConfig " + e.toString());
					}
				}
			}
			if (!result.isEmpty()) {
				for (Object o : result.keySet()) {
					String key = (String) o;
					if (key.equals("Main_Menu")) {
						if (result.get(o).equals("T")) {
							MainMenuActivity.hasMainMenu = true;
						}
					} else if (CommonUtility.isValidNumber(key)) {
						int id = Integer.parseInt(key);
						if (result.get(o).equals("T")) {
							MainMenuActivity.selectedMenuMap.put(id, true);
						} else {
							MainMenuActivity.selectedMenuMap.put(id, false);
						}
					}
				}
			}
		}

		if (MainMenuActivity.menuMap == null || MainMenuActivity.menuMap.size() == 0) {
			MainMenuActivity.menuMap = new SparseArray<String>();
			MainMenuActivity.menuMap.put(1, getResources().getString(R.string.action_logout));
			MainMenuActivity.menuMap.put(2, getResources().getString(R.string.title_activity_lot_inquiry));
			MainMenuActivity.menuMap.put(3, getResources().getString(R.string.title_activity_carrier_assign));
			MainMenuActivity.menuMap.put(4, getResources().getString(R.string.title_activity_check_magazine));
			MainMenuActivity.menuMap.put(5, getResources().getString(R.string.title_activity_div_lot));
			MainMenuActivity.menuMap.put(6, getResources().getString(R.string.title_activity_rej));
			MainMenuActivity.menuMap.put(7, getResources().getString(R.string.title_activity_op_shift_mach_assign));
			MainMenuActivity.menuMap.put(8, getResources().getString(R.string.title_activity_mach_rfid_function));
			MainMenuActivity.menuMap.put(9, getResources().getString(R.string.title_activity_cassette));
			MainMenuActivity.menuMap.put(10, getResources().getString(R.string.title_activity_lot_carrier_hist));
			MainMenuActivity.menuMap.put(11, getResources().getString(R.string.title_activity_lot_carrier_report));
			MainMenuActivity.menuMap.put(12, getResources().getString(R.string.title_activity_app_info));
			MainMenuActivity.menuMap.put(13, getResources().getString(R.string.title_activity_setting));
			MainMenuActivity.menuMap.put(14, getResources().getString(R.string.exit));
			MainMenuActivity.menuMap.put(15, getResources().getString(R.string.title_activity_end_lot));
			MainMenuActivity.menuMap.put(16, getResources().getString(R.string.title_activity_end_lot_on_mach));
			MainMenuActivity.menuMap.put(17, getResources().getString(R.string.title_activity_rej_mag));
			MainMenuActivity.menuMap.put(18, getResources().getString(R.string.title_activity_strip_inspection));
			MainMenuActivity.menuMap.put(19, getResources().getString(R.string.title_activity_rack_mgmt));
			MainMenuActivity.menuMap.put(20, getResources().getString(R.string.title_activity_lot_rack_mgmt));
			MainMenuActivity.menuMap.put(21, getResources().getString(R.string.title_activity_pass_window));
			MainMenuActivity.menuMap.put(22, getResources().getString(R.string.title_activity_message_test));
			MainMenuActivity.menuMap.put(23, getResources().getString(R.string.title_activity_alarm_unset));
			MainMenuActivity.menuMap.put(24, getResources().getString(R.string.title_activity_lot_pass_window));
			MainMenuActivity.menuMap.put(25, getResources().getString(R.string.title_activity_lot_carrier_tracking));
		}
		// if (MainMenuActivity.hasMainMenu) {
		// for (int i = 0; i < MainMenuActivity.selectedMenuMap.size(); i++) {
		// int id = MainMenuActivity.selectedMenuMap.keyAt(i);
		// if (MainMenuActivity.selectedMenuMap.get(id) && MainMenuActivity.menuMap.indexOfKey(id) >= 0) {
		// HashMap<String, Object> map = new HashMap<String, Object>();
		// map.put(Constants.ITEM_TEXT, MainMenuActivity.menuMap.get(id));
		// map.put(Constants.ITEM_TITLE, id);
		// machList.add(map);
		// }
		// }
		// }
		// if (machGridAdapter != null) {
		// machGridAdapter.notifyDataSetChanged();
		// }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.control, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.action_alarm_unset).setVisible(Constants.alarmUnsetMenu);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onPause() {
		if (null != qTask) {
			qTask.cancel(true);
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences data = ControlActivity.this.getSharedPreferences("RFID-data", MODE_PRIVATE);
		if (null == data || CommonUtility.isEmpty(data.getString(Constants.USER_ID, ""))) {
			global.setAoLot(null);
			Intent intent = new Intent(this, LoginActivity.class);
			startActivityForResult(intent, 0);
		} else if (global.isFirstNewMsg()) {
			Intent i = new Intent(ControlActivity.this, MessageActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			startActivity(i);
		} else {
			if (null == global.getUser()) {
				setTitle(getResources().getString(R.string.app_name));
			} else {
				setTitle(getResources().getString(R.string.app_name) + " " + global.getUser().getUserID());
			}
			global.setScanTarget("");
			global.setAoLot(null);
			global.setCarrierID("");
			global.setMachID("");
			machList.clear();
			if (null != data) {
				Set<String> assignedMach = data.getStringSet("assignedMach", new HashSet<String>());
				if (!assignedMach.isEmpty()) {
					for (String mach : assignedMach) {
						HashMap<String, Object> map = new HashMap<String, Object>();
						map.put(Constants.ITEM_TEXT, mach);
						machList.add(map);
					}
				}
			}
			formMainMenuButtons();
			machGridAdapter.notifyDataSetChanged();
			// machListAdapter = new SimpleAdapter(ControlActivity.this, machList, R.layout.control_mach_item, new String[] { Constants.ITEM_TEXT },
			// new int[] { R.id.ItemText });
			if (qTask == null) {
				// mStatusMessageView.setText(getResources().getString(R.string.loading_data));
				// showProgress2(true);
				qTask = new QueryTask();
				qTask.execute("getAssignedMach");
			}
		}
	}

	private void formMainMenuButtons() {
		if (MainMenuActivity.hasMainMenu && MainMenuActivity.menuMap != null) {
			for (int i = 0; i < MainMenuActivity.selectedMenuMap.size(); i++) {
				int id = MainMenuActivity.selectedMenuMap.keyAt(i);
				if (MainMenuActivity.selectedMenuMap.get(id) && MainMenuActivity.menuMap.indexOfKey(id) >= 0) {
					if (id == 23 && !Constants.alarmUnsetMenu) {
					} else {
						HashMap<String, Object> map = new HashMap<String, Object>();
						map.put(Constants.ITEM_TEXT, MainMenuActivity.menuMap.get(id));
						map.put(Constants.ITEM_TITLE, id);
						machList.add(map);
					}
				}
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (null != cursor) {
			cursor.close();
		}
	}

	@Override
	protected void onDestroy() {
		// log("control onDestroy");
		unregisterReceiver(hostMsgReceiver);
		unregisterReceiver(wifiMsgReceiver);
		msgdb.close();
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null) {
			switch (resultCode) {
			case 0:
				// after login
				lastAssignedMachCheck = 0;
				Toast.makeText(ControlActivity.this, "登录成功", Toast.LENGTH_LONG).show();
				break;
			case -1:
				String result = data.getStringExtra("SCAN_RESULT");
				if (!CommonUtility.isEmpty(result)) { // && result.trim().startsWith("1T")
					tagBarcodeInput.setText(result.trim() + ";");
				}
				break;
			case 5:
				// after RejMagActivity
				global.setScanTarget("");
				break;
			}
		}
	}

	/*
	 * Retrieve message from MsgReceiveService about msg and command from driver.
	 */
	class HostMsgReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String msg = intent.getStringExtra("msg");
			final String mach = intent.getStringExtra("mach");
			final String time = intent.getStringExtra("time");
			final String type = intent.getStringExtra("type");
			// log("Control HostMsgReceiver");
			// Intent i = new Intent(ControlActivity.this, MessageActivity.class);
			// //i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			// i.putExtra("msg", msg);
			// i.putExtra("mach", mach);
			// i.putExtra("time", time);
			// i.putExtra("type", type);
			// startActivity(i);
			String MESSAGE = "MESSAGE";
			String MACHINE = "MACHINE";
			String TIME = "TIME";
			String TYPE = "TYPE";
			if (!msg.isEmpty()) {
				HashMap<String, String> map = new HashMap<String, String>();
				map.put(MESSAGE, msg);
				map.put(MACHINE, mach);
				map.put(TIME, time);
				map.put(TYPE, type);
				global.getMsgListItem().add(0, map);
				Intent i = new Intent(ControlActivity.this, MessageActivity.class);
				if (global.isShown()) { // to check whether the MessageActivity is shown
					global.setFirstNewMsg(true);
					i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
					startActivity(i);
				} else if (!global.isFirstNewMsg()) { // to check if this is the first messsage received after application was not at front, in order not to start MessageActivity continuously
					global.setFirstNewMsg(true);
					i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
					startActivity(i);
				}
			}
		}
	}

	class WifiMessageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String msg = intent.getStringExtra("msg");
			/*
			 * Intent i = new Intent(ControlActivity.this, WifiConfigActivity.class); // i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); i.putExtra("msg", msg); startActivity(i); //
			 * Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			 */
			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";
		String mach = "";
		// boolean startMsgReceive1 = false;
		// boolean startMsgReceive3 = false;
		boolean startMsgReceive4 = false;
		boolean getCurrentUserMachAssignment = false;
		boolean toLogout = false;
		// boolean showWifiConfig = false;
		String lot = "";
		String carrierId = "";
		String carrierName = "";
		String resultCode = "";

		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("getAssignedMach")) {
					// CommonUtility.pingHost(global.getInterfaceMgrSocketConfigQuery().getHost());
					getAssignedMach();
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
				} else if (cmdName.equals("removeHostname")) {
					removeHostname();
				} else if (cmdName.equals("startMach")) {
					mach = params[1];
					// sendPicEvent($machId, "EVENT_ID=RFID_EVENT TYPE=ALARM_UNSET replyFormat=python machId=$machId")
					String cmd = "EVENT_ID=RFID_EVENT TYPE=ALARM_UNSET replyFormat=python machId=" + mach;
					String api = "sendPicEvent('" + mach + "','" + cmd + "')";
					logf(classname + " " + global.getUser().getUserID() + " " + api);
					apiExecutorUpdate.transact(classname, "startMach", api);
				} else if (cmdName.equals("checkBarcodeInput")) {
					CommonTrans commonTrans = new CommonTrans();
					String[] result = commonTrans.checkBarcodeInput(apiExecutorQuery, params[1], global.getUser().getDepartment());
					resultCode = result[0];
					lot = result[1];
					carrierId = result[2];
					carrierName = result[3];
				}
			} catch (RfidWifiException e) {
				// showWifiConfig = true;
				return e;
			} catch (BaseException e) {
				return e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(BaseException e) {
			qTask = null;
			showProgress(false);
			showProgress2(false);
			if (null == e) {
				if (cmdName.equals("getAssignedMach")) {
					getAssignedMachAfter();
				} else if (cmdName.equals("removeHostname")) {
					removeHostnameAfter();
				} else if (cmdName.equals("startMach")) {
					// msgdb.updateType(mach, Constants.TYPE_END);
					SharedPreferences data = ControlActivity.this.getSharedPreferences("RFID-data", Context.MODE_PRIVATE);
					if (null != data) {
						Editor editor = data.edit();
						Set<String> alarmMach = data.getStringSet("alarmMach", new HashSet<String>());
						alarmMach.remove(mach);
						editor.putStringSet("alarmMach", alarmMach);
						editor.commit();
					}
					machGridAdapter.notifyDataSetChanged();
					// Toast.makeText(ControlActivity.this, "开机成功", Toast.LENGTH_SHORT).show();
					toastMsg("开机成功");
				} else if (cmdName.equals("checkBarcodeInput")) {
					if (resultCode.isEmpty() || resultCode.equals("lot") || (resultCode.equals("carrier") && !lot.isEmpty())) {
						Intent intent = new Intent(ControlActivity.this, LotInquiryActivity.class);
						intent.putExtra("alotNumber", lot);
						startActivity(intent);
					} else if (resultCode.equals("carrier") && lot.isEmpty()) {
						setTagId(carrierId);
					} else if (resultCode.equals("multiCarrier")) {
						showError(ControlActivity.this, carrierName + " 对应多个carrier");
					}
				}
			} else {
				logf(e.toString());
				// if (showWifiConfig) {
				// WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				// if (wifiManager.isWifiEnabled()) {
				// wifiManager.setWifiEnabled(false);
				// } else {
				// wifiManager.setWifiEnabled(true);
				// }
				// } else {
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(ControlActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(ControlActivity.this, e.getErrorMsg());
				}
				// }
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
			showProgress2(false);
		}
		
//		private boolean checkLoginStatus(CommonTrans commonTrans) throws BaseException {
//			String link = "servlet/LoginServlet?action=checkLoginStatus&deviceID=" + global.getAndroidSecureID() + "&coreID=" + global.getUser().getUserID() 
//					+ "&loginTime=" + global.getUser().getLastSuccessLogin();
//			String output = commonTrans.queryFromServer(link);
//			lastAutoLogoutCheck = new Date().getTime();
//			return output.equals("logout");
//		}

		private void getAssignedMach() throws BaseException {
			// String api = "execSql('select t.mach_id,m.MACH_MODEL,m.MACH_MTYP_TYPE from MACH_OPERATOR_HISTS t, machines m where t.user_id=\\\\'"
			// + global.getUser().getUserID() + "\\\\' and user_logout_time is null and (hostname=\\\\'android-" + global.getAndroidSecureID()
			// + ".ap.freescale.net\\\\' or hostname is null) and t.MACH_ID=m.MACH_ID')";
			// assignedMachDC = apiExecutor.query(api);
			// CommonTrans commonTrans = new CommonTrans();
			// assignedMachDC = commonTrans.getAssignedMachDC(apiExecutorQuery, global);
			CommonTrans commonTrans = new CommonTrans();
//			if (Constants.autoLogout && global.getUser() != null && (lastAutoLogoutCheck == 0 || new Date().getTime() - lastAutoLogoutCheck > 1000 * 60 * 15)) { // 15 minutes
//				toLogout = checkLoginStatus(commonTrans);
//			}
			if (Constants.autoLogout && global.getUser() != null) {
				Calendar loginTime = Calendar.getInstance();
				try {
					loginTime.setTime(dateFormat.parse(global.getUser().getLastSuccessLogin()));
					Calendar logoutTime = Calendar.getInstance();
					int hour = logoutTime.get(Calendar.HOUR_OF_DAY);
					if (hour >= 0 && hour < 8) {
						logoutTime.add(Calendar.DATE, -1);
						logoutTime.set(Calendar.HOUR_OF_DAY, 20);
					} else if (hour >= 8 && hour < 20) {
						logoutTime.set(Calendar.HOUR_OF_DAY, 8);
					} else {
						logoutTime.set(Calendar.HOUR_OF_DAY, 20);
					}
					logoutTime.set(Calendar.MINUTE, 0);
					logoutTime.set(Calendar.SECOND, 0);
					log("logoutTime " + dateFormat.format(logoutTime.getTime()));
					if (loginTime.compareTo(logoutTime) < 0) {
						toLogout = true;
					}
				} catch (ParseException e) {
					log(e.toString());
				}
			}
			if ((lastAssignedMachCheck == 0 || new Date().getTime() - lastAssignedMachCheck > 1000 * 60 * 60)) { // one hour
				if (!toLogout) {
					String api = "getCurrentUserMachAssignment(attributes='machId',userId='" + global.getUser().getUserID() + "',hostName='android-"
							+ global.getAndroidSecureID() + ".ap.freescale.net')";
					assignedMachDC = apiExecutorQuery.query("Control", "getAssignedMach", api);
//					if (global.getUser().getUserID().equalsIgnoreCase("b33021")) {
//						ArrayList<String> line2 = new ArrayList<String>();
//						line2.add("BWB-133");
//						ArrayList<String> line1 = new ArrayList<String>();
//						line1.add("BWB-134");
//						ArrayList<String> line3 = new ArrayList<String>();
//						line3.add("BWB-137");
//						ArrayList<String> line4 = new ArrayList<String>();
//						line4.add("BWB-143");
//						assignedMachDC.add(line1);
//						assignedMachDC.add(line2);
//						assignedMachDC.add(line3);
//						assignedMachDC.add(line4);
//					}
					getCurrentUserMachAssignment = true;
				}
			}
			if (toLogout) {
				removeHostname();
			}
			if (MsgReceiveService4.msgReceiveFlag) {
				if (MsgReceiveService4.hostPort == 0 || MsgReceiveService4.hostIP.isEmpty()) {
					String link = "servlet/LoginServlet?action=getPort&deviceID=" + global.getAndroidSecureID();
					String output = commonTrans.queryFromServer(link);
					if (!output.equals("0") && output.contains(":")) {
						String temp[] = output.split(":");
						if (CommonUtility.isValidNumber(temp[1])) {
							MsgReceiveService4.hostIP = temp[0];
							MsgReceiveService4.hostPort = Integer.parseInt(temp[1]);
							log("MsgReceiveService4.hostIP " + MsgReceiveService4.hostIP + " MsgReceiveService4.hostPort " + MsgReceiveService4.hostPort);
							startMsgReceive4 = true;
						} else {
							MsgReceiveService4.msgReceiveFlag = false;
						}
					} else {
						MsgReceiveService4.msgReceiveFlag = false;
					}
				} else {
					startMsgReceive4 = true;
				}
			}
			// if (MsgReceiveService3.msgReceiveFlag && (MsgReceiveService3.serverHost.isEmpty() || MsgReceiveService3.groupName.isEmpty())) {
			// String link = "servlet/LoginServlet?action=getPic&deviceID=" + global.getAndroidSecureID();
			// String output = commonTrans.queryFromServer(link);
			// if (!output.equals("0") && output.contains(":")) {
			// String temp[] = output.split(":");
			// MsgReceiveService3.serverHost = temp[0];
			// MsgReceiveService3.groupName = temp[1];
			// CommonUtility.logError("MsgReceiveService3.serverHost " + MsgReceiveService3.serverHost + " groupName " + MsgReceiveService3.groupName,
			// MsgReceiveService3.TAG);
			// startMsgReceive3 = true;
			// } else {
			// MsgReceiveService3.msgReceiveFlag = false;
			// MsgReceiveService3.serverHost = "";
			// MsgReceiveService3.groupName = "";
			// }
			// }
		}

		private void getAssignedMachAfter() {
			lastAssignedMachCheck = new Date().getTime();
			if (MsgReceiveService4.msgReceiveFlag && startMsgReceive4 && !getCurrentUserMachAssignment) {
				Intent intent4 = new Intent(ControlActivity.this, MsgReceiveService4.class);
				intent4.putExtra("deviceID", global.getAndroidSecureID());
				startService(intent4);
			}
			// if (MsgReceiveService3.msgReceiveFlag && startMsgReceive3) {
			// Intent intent3 = new Intent(ControlActivity.this, MsgReceiveService3.class);
			// intent3.putExtra("deviceID", global.getAndroidSecureID());
			// startService(intent3);
			// }
			if (getCurrentUserMachAssignment) {
				Intent intent4 = new Intent(ControlActivity.this, MsgReceiveService4.class);
				intent4.putExtra("deviceID", global.getAndroidSecureID());
				intent4.putExtra("type", "restart");
				startService(intent4);
				Set<String> assignedMach = new HashSet<String>();
				machList.clear();
				if (!CommonUtility.isEmpty(assignedMachDC)) {
					for (int i = 0; i < assignedMachDC.size(); i++) {
						HashMap<String, Object> map = new HashMap<String, Object>();
						map.put(Constants.ITEM_TEXT, assignedMachDC.get(i).get(0));
						machList.add(map);
						assignedMach.add(assignedMachDC.get(i).get(0));
					}
				}
				formMainMenuButtons();
				machGridAdapter.notifyDataSetChanged();
				SharedPreferences shared = ControlActivity.this.getSharedPreferences("RFID-data", MODE_PRIVATE);
				Editor e = shared.edit();
				e.putStringSet("assignedMach", assignedMach);
				e.commit();
			}
			if (toLogout) {
				removeHostnameAfter();
				toastMsg("登录超时，请重新登录");
			}
		}

		private void removeHostname() throws BaseException {
			// deassigned his assigned mach with this tablet's hostname
			String api = "getCurrentUserMachAssignment(attributes='machId',userId='" + global.getUser().getUserID() + "',hostName='android-"
					+ global.getAndroidSecureID() + ".ap.freescale.net')";
			DataCollection assignedMachData = apiExecutorQuery.query(classname, "removeHostname", api);
			if (!CommonUtility.isEmpty(assignedMachData)) {
				List<String> apiCommandList = new ArrayList<String>();
				for (ArrayList<String> mach : assignedMachData) {
					String machID = mach.get(0);
					String transApi = "registerOperatorOnMachine(transUserId='" + global.getUser().getUserID() + "', machId='" + machID + "', stepName='', hostName='') ";
					apiCommandList.add(transApi);
				}
				CommonTrans commonTrans = new CommonTrans();
				String multiAPI = commonTrans.getMultipleAPI(global.getUser().getUserID(), apiCommandList);
				apiExecutorUpdate.transact(classname, "removeHostname", multiAPI);
			}
		}

		private void removeHostnameAfter() {
			SharedPreferences shared = ControlActivity.this.getSharedPreferences("RFID-data", MODE_PRIVATE);
			Editor e = shared.edit();
			e.remove(Constants.USER_ID);
			e.remove(Constants.FIRST_NAME);
			e.remove(Constants.LAST_NAME);
			e.remove(Constants.DEPARTMENT);
			e.remove(Constants.SERVER_CURRENT_DATE);
			e.remove("assignedMach");
			e.commit();
			global.setUser(null);
			global.setUserOperationList("");
			Intent intent = new Intent(ControlActivity.this, LoginActivity.class);
			startActivityForResult(intent, 0);
		}
	}

	protected void setupActionBar() {
	}

	@Override
	public void onBackPressed() {
	}

	public void logout() {
		super.logout();
		if (qTask == null) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("removeHostname");
		}
	}

	public void exit() {
		final EditText e = new EditText(ControlActivity.this);
		e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		AlertDialog.Builder builder = new AlertDialog.Builder(ControlActivity.this);
		builder.setTitle(getResources().getString(R.string.prompt_password)).setIcon(android.R.drawable.ic_secure).setView(e);
		builder.setPositiveButton(getResources().getString(R.string.button_done), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String input = "" + e.getText();
				if (input.equals(Constants.PASSWORD)) {
					if (Constants.type == 5) {
						Intent stopService = new Intent();
						stopService.setAction("com.fsl.cimei.rfid.NFCService");
						stopService.putExtra("stopflag", true);
						sendBroadcast(stopService);
					}
					getPackageManager().clearPackagePreferredActivities("com.fsl.cimei.rfid");
					finish();
				} else {
					toastMsg("密码不正确"); // Toast.makeText(ControlActivity.this, "密码不正确", Toast.LENGTH_SHORT).show();
				}
			}
		});
		builder.setNegativeButton(getResources().getString(R.string.button_cancel), null);
		builder.show();
	}

	public void goToSetting() {
		final EditText e = new EditText(ControlActivity.this);
		e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		AlertDialog.Builder builder = new AlertDialog.Builder(ControlActivity.this);
		builder.setTitle(getResources().getString(R.string.prompt_password)).setIcon(android.R.drawable.ic_secure).setView(e);
		builder.setPositiveButton(getResources().getString(R.string.button_done), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String input = "" + e.getText();
				if (input.equals(Constants.PASSWORD)) {
					Intent intent = new Intent();
					intent = new Intent(ControlActivity.this, SettingActivity.class);
					startActivity(intent);
				} else {
					Toast.makeText(ControlActivity.this, "密码不正确", Toast.LENGTH_SHORT).show();
				}
			}
		});
		builder.setNegativeButton(getResources().getString(R.string.button_cancel), null);
		builder.show();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	protected void showProgress2(final boolean show) {
		if (null == mStatusView2) {
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
			mStatusView2.setVisibility(View.VISIBLE);
			mStatusView2.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mStatusView2.setVisibility(show ? View.VISIBLE : View.GONE);
				}
			});
		} else {
			mStatusView2.setVisibility(show ? View.VISIBLE : View.GONE);
		}
	}

	public void setBarcodeInput(String input) { // lot number or carrier name
		if (qTask == null && !CommonUtility.isEmpty(input)) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("checkBarcodeInput", input);
		}
	}

	public void startScanBarcode() {
		if (qTask == null) {
			tagBarcodeInput.setText("");
			qTask = new QueryTask();
			qTask.execute("scanBarcode");
		}
	}

	class MsgAdapter extends SimpleCursorAdapter {

		// private LayoutInflater mInflater;
		// private MsgViewHolder holder = null;

		// public MsgAdapter(Context context) {
		// this.mInflater = LayoutInflater.from(context);
		// }

		@SuppressWarnings("unused")
		private Cursor m_cursor;
		@SuppressWarnings("unused")
		private Context m_context;
		private LayoutInflater miInflater;

		public MsgAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
			m_context = context;
			m_cursor = c;
		}

		@SuppressLint("InflateParams")
		@Override
		public void bindView(View arg0, Context arg1, Cursor cursor) {
			View convertView = null;
			if (arg0 == null) {
				convertView = miInflater.inflate(R.layout.mach_msg_list_item, null);
			} else {
				convertView = arg0;
			}
			TextView content = (TextView) convertView.findViewById(R.id.itemText);
			TextView time = (TextView) convertView.findViewById(R.id.itemTitle);
			String timeStr = cursor.getString(0);
			String contentStr = cursor.getString(1);
			String typeStr = cursor.getString(3);
			content.setText(contentStr);
			time.setText(timeStr);
			if (typeStr.equals(Constants.TYPE_MSG)) {
				content.setTextColor(msgColor);
			} else if (typeStr.equals(Constants.TYPE_ERROR) || typeStr.equals(Constants.TYPE_END) || typeStr.equals(Constants.TYPE_MISSING)) {
				content.setTextColor(errorColor);
			} else if (typeStr.equals(Constants.TYPE_STEP)) {
				content.setTextColor(stepColor);
			}
		}

		// @Override
		// public int getCount() {
		// return msgListItem.size();
		// }
		//
		// @Override
		// public Object getItem(int arg0) {
		// return msgListItem.get(arg0);
		// }
		//
		// @Override
		// public long getItemId(int arg0) {
		// return arg0;
		// }
		//
		// @SuppressLint("InflateParams")
		// @Override
		// public View getView(final int position, View convertView, ViewGroup parent) {
		// if (convertView == null) {
		// holder = new MsgViewHolder();
		// convertView = mInflater.inflate(R.layout.mach_msg_list_item, null);
		// holder.content = (TextView) convertView.findViewById(R.id.itemText);
		// holder.time = (TextView) convertView.findViewById(R.id.itemTitle);
		// convertView.setTag(holder);
		// } else {
		// holder = (MsgViewHolder) convertView.getTag();
		// }
		// // MESSAGE, MACHINE, TIME
		// holder.content.setText(msgListItem.get(position).get("content"));
		// if (msgListItem.get(position).get("type").equals(Constants.TYPE_MSG)) {
		// holder.content.setTextColor(msgColor);
		// } else if (msgListItem.get(position).get("type").equals(Constants.TYPE_ERROR) || msgListItem.get(position).get("type").equals(Constants.TYPE_END)
		// || msgListItem.get(position).get("type").equals(Constants.TYPE_MISSING)) {
		// holder.content.setTextColor(errorColor);
		// } else if (msgListItem.get(position).get("type").equals(Constants.TYPE_STEP)) {
		// holder.content.setTextColor(stepColor);
		// }
		// holder.time.setText(msgListItem.get(position).get("time"));
		// return convertView;
		// }
	}

	class MsgViewHolder {
		public TextView content;
		public TextView time;
	}

	class MachGridAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private MachGridViewHolder holder = null;

		public MachGridAdapter(Context context) {
			this.mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return machList.size();
		}

		@Override
		public Object getItem(int arg0) {
			return machList.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				holder = new MachGridViewHolder();
				convertView = mInflater.inflate(R.layout.control_mach_item, null);
				holder.mach = (TextView) convertView.findViewById(R.id.ItemText);
				convertView.setTag(holder);
			} else {
				holder = (MachGridViewHolder) convertView.getTag();
			}
			// SharedPreferences data = ControlActivity.this.getSharedPreferences("RFID-data", MODE_PRIVATE);
			// Set<String> alertMachSet;
			// if (null != data) {
			// alertMachSet = data.getStringSet("alertMach", new HashSet<String>());
			// } else {
			// alertMachSet = new HashSet<String>();
			// }
			String mach = machList.get(position).get(Constants.ITEM_TEXT).toString();
			holder.mach.setText(mach);
			if (machList.get(position).containsKey(Constants.ITEM_TITLE)) {
				convertView.setBackgroundResource(R.drawable.control_quick_access_item);
				holder.mach.setTextColor(getResources().getColor(R.color.bg_black));
			} else {
				// cursor = msgdb.query(new String[] { "_id", "type" }, "mach=? and (type=? or type=?)", new String[] { mach, Constants.TYPE_END, Constants.TYPE_MISSING },
				// null, null, null, null);
				// if (cursor.moveToNext()) {
				SharedPreferences data = ControlActivity.this.getSharedPreferences("RFID-data", Context.MODE_PRIVATE);
				boolean alarm = false;
				if (null != data) {
					Set<String> alarmMach = data.getStringSet("alarmMach", new HashSet<String>());
					if (alarmMach.contains(mach)) {
						alarm = true;
					}
				}
				if (alarm) {
					convertView.setBackgroundResource(R.drawable.control_mach_offline_item);
					holder.mach.setTextColor(getResources().getColor(R.color.bg_black));
				} else {
					convertView.setBackgroundResource(R.drawable.control_mach_normal_item);
					holder.mach.setTextColor(getResources().getColor(R.color.bg_white));
				}
			}
			return convertView;
		}
	}

	class MachGridViewHolder {
		public TextView mach;
	}

}
