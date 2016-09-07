package com.fsl.cimei.rfid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class AppInfoActivity extends BaseActivity {

	private QueryTask qTask;

	private int versionCode = 0;
	private int serverVersion = 0;
	private String versionName = "";
	private TextView appName;
	private LinearLayout checkVersion;
	private LinearLayout versionDesc;
	private ToggleButton wifiCheckToggle;
	private ToggleButton msgReceiveToggle;
	private ToggleButton msgReceive2Toggle;
	private ToggleButton msgReceive3Toggle;
	private ToggleButton msgReceive4Toggle;

	// private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_app_info);
		mFormView = findViewById(R.id.app_info_form);
		mStatusView = findViewById(R.id.app_info_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		showProgress(false);
		appName = (TextView) findViewById(R.id.app_info_name);
		checkVersion = (LinearLayout) findViewById(R.id.app_info_check_version);
		versionDesc = (LinearLayout) findViewById(R.id.app_info_version_desc);
		try {
			versionCode = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionCode;
			versionName = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			log(e.toString());
		}
		appName.setText(getResources().getString(R.string.app_name) + " " + versionName);
		// + "\n" + simpleDateFormat.format(new Date())

		checkVersion.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (serverVersion > versionCode) {
					AlertDialog.Builder builder = new AlertDialog.Builder(AppInfoActivity.this);
					builder.setTitle("软件有新版本").setIcon(android.R.drawable.ic_dialog_info);
					builder.setPositiveButton("更新", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (null == qTask) {
								qTask = new QueryTask();
								qTask.execute("updateSoftware");
							}
						}
					});
					builder.setNegativeButton(getResources().getString(R.string.close), null);
					builder.show();
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(AppInfoActivity.this);
					builder.setTitle("软件已是最新版本").setIcon(android.R.drawable.ic_dialog_info);
					builder.setNegativeButton(getResources().getString(R.string.close), null);
					builder.show();
				}
			}
		});

		versionDesc.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setAction("android.intent.action.VIEW");
				Uri content_url = Uri.parse("http://10.192.130.4:8085/RFID/readme.html");
				intent.setData(content_url);
				startActivity(intent);
			}
		});

		findViewById(R.id.app_info_server_line).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(AppInfoActivity.this, MainMenuActivity.class);
				startActivity(intent);
			}
		});
		((TextView) findViewById(R.id.app_info_env)).setText(Constants.configFileName.substring(0, Constants.configFileName.indexOf(".")));
		((TextView) findViewById(R.id.app_info_user)).setText(global.getUser() == null ? "none" : global.getUser().getUserID());
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		if (wifiInfo.getIpAddress() != 0) {
			((TextView) findViewById(R.id.app_info_scan_target)).setText(CommonUtility.intToIp(wifiInfo.getIpAddress()));
		}
		((TextView) findViewById(R.id.app_info_secure_id)).setText(global.getAndroidSecureID());
		((TextView) findViewById(R.id.app_info_android_env)).setText(android.os.Build.MODEL + " || " + android.os.Build.VERSION.SDK_INT + " || "
				+ android.os.Build.VERSION.RELEASE);

		wifiCheckToggle = (ToggleButton) findViewById(R.id.app_info_wifi_check_flag);
		msgReceiveToggle = (ToggleButton) findViewById(R.id.app_info_msg_receive_flag);
		msgReceive2Toggle = (ToggleButton) findViewById(R.id.app_info_msg_receive_2_flag);
		msgReceive3Toggle = (ToggleButton) findViewById(R.id.app_info_msg_receive_3_flag);
		msgReceive4Toggle = (ToggleButton) findViewById(R.id.app_info_msg_receive_4_flag);
		wifiCheckToggle.setTextOn("检查WiFi");
		wifiCheckToggle.setTextOff("不检查WiFi");
		msgReceiveToggle.setTextOn("接收消息");
		msgReceiveToggle.setTextOff("不接收消息");
		msgReceive2Toggle.setTextOn("接收消息");
		msgReceive2Toggle.setTextOff("不接收消息");
		msgReceive3Toggle.setTextOn("接收消息");
		msgReceive3Toggle.setTextOff("不接收消息");
		msgReceive4Toggle.setTextOn("接收消息");
		msgReceive4Toggle.setTextOff("不接收消息");
		wifiCheckToggle.setChecked(WifiCheckService.wifiCheckFlag);
		msgReceiveToggle.setChecked(MsgReceiveService.msgReceiveFlag);
		msgReceive2Toggle.setChecked(MsgReceiveService1.regularSendFlag);
		msgReceive3Toggle.setChecked(MsgReceiveService3.msgReceiveFlag);
		msgReceive4Toggle.setChecked(MsgReceiveService4.msgReceiveFlag);
		wifiCheckToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				WifiCheckService.wifiCheckFlag = isChecked;
				Intent wifiCheckIntent = new Intent(getApplicationContext(), WifiCheckService.class);
				if (isChecked) {
					startService(wifiCheckIntent);
				} else {
					stopService(wifiCheckIntent);
				}
			}
		});
		msgReceiveToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				MsgReceiveService.msgReceiveFlag = isChecked;
				Intent msgReceiveIntent = new Intent(getApplicationContext(), MsgReceiveService.class);
				if (isChecked) {
					msgReceiveIntent.putExtra("deviceID", global.getAndroidSecureID());
					startService(msgReceiveIntent);
				} else {
					stopService(msgReceiveIntent);
				}
			}
		});
		msgReceive2Toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Intent msgReceive1Intent = new Intent(getApplicationContext(), MsgReceiveService1.class);
				msgReceive1Intent.putExtra("deviceID", global.getAndroidSecureID());
				if (isChecked) {
					startService(msgReceive1Intent);
				} else {
					stopService(msgReceive1Intent);
				}
				finish();
			}
		});
		msgReceive3Toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Intent msgReceive3Intent = new Intent(getApplicationContext(), MsgReceiveService3.class);
				msgReceive3Intent.putExtra("deviceID", global.getAndroidSecureID());
				MsgReceiveService3.msgReceiveFlag = isChecked;
				if (isChecked) {
					// startService(msgReceive3Intent);
				} else {
					MsgReceiveService3.serverHost = "";
					MsgReceiveService3.groupName = "";
					stopService(msgReceive3Intent);
				}
				finish();
			}
		});
		msgReceive4Toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Intent msgReceive4Intent = new Intent(getApplicationContext(), MsgReceiveService4.class);
				msgReceive4Intent.putExtra("deviceID", global.getAndroidSecureID());
				MsgReceiveService4.msgReceiveFlag = isChecked;
				if (isChecked) {
					// startService(msgReceive4Intent);
				} else {
					stopService(msgReceive4Intent);
				}
				finish();
			}
		});

		qTask = new QueryTask();
		qTask.execute("checkVersion");
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			BaseException e = null;
			try {
				if (cmdName.equals("updateSoftware")) {
					updateSoftware();
				} else if (cmdName.equals("checkVersion")) {
					checkVersion();
				}
			} catch (BaseException e1) {
				e = e1;
			}
			return e;
		}

		@Override
		protected void onPostExecute(BaseException e) {
			qTask = null;
			showProgress(false);
			if (null == e) {
				if (cmdName.equals("updateSoftware")) {
					updateSoftwareAfter();
				} else if (cmdName.equals("checkVersion")) {
					checkVersionAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(AppInfoActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(AppInfoActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void updateSoftware() throws BaseException {
			try {
				URL url = new URL("http://10.192.130.4:8085/RFID/servlet/DownloadFileServlet?deviceID=" + global.getAndroidSecureID());
				HttpURLConnection c = (HttpURLConnection) url.openConnection();
				c.setRequestMethod("GET");
				c.setDoOutput(true);
				c.connect();

				String PATH = Environment.getExternalStorageDirectory() + "/download/";
				File file = new File(PATH);
				File outputFile = new File(file, "RFID.apk");
				if (!outputFile.exists() || (outputFile.exists() && outputFile.delete())) {
					FileOutputStream fos = new FileOutputStream(outputFile);
					InputStream is = c.getInputStream();
					byte[] buffer = new byte[1024];
					int len1 = 0;
					while ((len1 = is.read(buffer)) != -1) {
						fos.write(buffer, 0, len1);
					}
					fos.close();
					is.close();
				} else {
					throw new RfidException("下载更新失败", "AppInfo", "updateSoftware", "");
				}
			} catch (Exception e) {
				throw new RfidException("下载更新失败 " + e.toString(), "AppInfo", "updateSoftware", "");
			}
		}

		private void updateSoftwareAfter() {
			File file = new File(Environment.getExternalStorageDirectory() + "/download/" + "RFID.apk");
			if (file.exists()) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		}

		private void checkVersion() throws BaseException {
			CommonTrans commonTrans = new CommonTrans();
			String link = "servlet/LoginServlet?action=getVersion&deviceID=" + global.getAndroidSecureID();
			String output = commonTrans.queryFromServer(link);
			if (CommonUtility.isValidNumber(output)) {
				serverVersion = Integer.parseInt(output);
			}
		}

		private void checkVersionAfter() {
			if (serverVersion > versionCode) {
				AlertDialog.Builder builder = new AlertDialog.Builder(AppInfoActivity.this);
				builder.setTitle("软件有新版本").setIcon(android.R.drawable.ic_dialog_info);
				builder.setPositiveButton("更新", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (null == qTask) {
							qTask = new QueryTask();
							qTask.execute("updateSoftware");
						}
					}
				});
				builder.setNegativeButton(getResources().getString(R.string.close), null);
				builder.show();
			} else {
				// AlertDialog.Builder builder = new AlertDialog.Builder(AppInfoActivity.this);
				// builder.setTitle("软件已是最新版本").setIcon(android.R.drawable.ic_dialog_info);
				// builder.setNegativeButton(getResources().getString(R.string.close), null);
				// builder.show();
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == 138 || keyCode == 132) { // F2 for type 2 and 3
			PackageManager manager = getPackageManager();
			// List<ApplicationInfo> list = manager.getInstalledApplications(PackageManager.GET_META_DATA);
			// for (ApplicationInfo ai : list) {
			// log(ai.packageName);
			// }
			Intent i = manager.getLaunchIntentForPackage("com.android.settings");
			startActivity(i);
		} else if (keyCode == 135) { // iData F3
			PackageManager manager = getPackageManager();
			Intent i = manager.getLaunchIntentForPackage("com.android.auto.iscan");
			if (null != i) {
				startActivity(i);
			}
		} else {
			// log("" + keyCode);
			// the first: F1 131 F2 132 F3 133 F4 134
			// iData: F1 137 F2 138 F3 135 Aa 136 barcode 140 141 tag 139
		}
		return super.onKeyDown(keyCode, event);
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
		if (Constants.type == 2 || Constants.type == 3) {
			((TextView) findViewById(R.id.app_info_f2)).setVisibility(View.VISIBLE);
		}
		if (Constants.type == 3) {
			((TextView) findViewById(R.id.app_info_f3)).setVisibility(View.VISIBLE);
		}
		((TextView) findViewById(R.id.app_info_server)).setText(MainMenuActivity.hasMainMenu ? "Yes" : "No");
		super.onResume();
	}
}
