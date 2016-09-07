package com.fsl.cimei.rfid;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidWifiException;

public class WifiConfigActivity extends BaseActivity {

	private TextView msgView;
	private QueryTask qTask;
	private WifiManager wifiManager;
	private final int tryingCount = 20;
	private final String ssid = "Freescale";
//	private WifiMessageReceiver wifiState;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifi_config);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		mFormView = findViewById(R.id.wifi_config_form);
		mStatusView = findViewById(R.id.wifi_config_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		msgView = (TextView) findViewById(R.id.wifi_config_msg);
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//		wifiState = new WifiMessageReceiver();
//		IntentFilter filterHost = new IntentFilter();
//		filterHost.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
//		registerReceiver(wifiState, filterHost);
		resolveNewIntent(getIntent());
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";
		// String resultCode = "";

		@Override
		protected BaseException doInBackground(String... params) {
			BaseException exception = null;
			cmdName = params[0];
//			try {
//				if (cmdName.equals("checkWifi")) {
//					checkWifi();
//				} else if (cmdName.equals("checkSSIDandIP")) {
//					checkSSIDandIP();
//				} else if (cmdName.equals("pingHost")) {
//					pingHost();
//				}
//			} catch (BaseException e) {
//				exception = e;
//			}
			return exception;
		}

		@Override
		protected void onPostExecute(BaseException exception) {
			qTask = null;
			showProgress(false);
			if (exception == null) {
				if (cmdName.equals("checkWifi")) {
					msgView.setText("检查中。。。");
				} else if (cmdName.equals("checkSSIDandIP")) {
					msgView.setText("尝试连接服务器中。。。");
					qTask = new QueryTask();
					qTask.execute("pingHost");
				} else if (cmdName.equals("pingHost")) {
					msgView.setText("成功 连接服务器");
				}
			} else {
				logf(exception.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					msgView.setText(exception.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					msgView.setText(exception.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void checkWifi() {
			if (wifiManager.isWifiEnabled()) {
				wifiManager.setWifiEnabled(false);
			} else {
				wifiManager.setWifiEnabled(true);
			}
		}

		private void checkSSIDandIP() throws RfidWifiException {
			int i = tryingCount;
			while (i > 0) {
				WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				if (wifiInfo != null) {
					log("DetailedState " + WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState()).toString());
					log("IP " + wifiInfo.getIpAddress());
					log("toString " + wifiInfo.toString());
					if (wifiInfo.getSSID() != null && (wifiInfo.getSSID().equals(ssid) || wifiInfo.getSSID().equals("\"" + ssid + "\""))
							&& WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState()).equals(DetailedState.OBTAINING_IPADDR) && wifiInfo.getIpAddress() != 0) {
						break;
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				i--;
			}
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			if (wifiInfo != null && wifiInfo.getSSID() != null && (wifiInfo.getSSID().equals(ssid) || wifiInfo.getSSID().equals("\"" + ssid + "\"")) && wifiInfo.getIpAddress() != 0) {
			} else {
				throw new RfidWifiException("无法连接到服务器");
			}
		}

		private void pingHost() throws RfidWifiException {
			CommonUtility.pingHost(global.getInterfaceMgrSocketConfigQuery().getHost());
		}

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
//		if (qTask == null) {
//			showProgress(true);
//			qTask = new QueryTask();
//			qTask.execute("checkWifi");
//		}
		super.onResume();
	}

//	class WifiStateReceiver extends BroadcastReceiver {
//
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			if (intent.getAction().endsWith(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
//				int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
//				switch (wifiState) {
//				case 0:
//					msgView.setText("无线网络 正在关闭");
//					break;
//				case 1:
//					msgView.setText("无线网络 已关闭");
//					wifiManager.setWifiEnabled(true);
//					break;
//				case 2:
//					msgView.setText("无线网络 正在开启");
//					break;
//				case 3:
//					msgView.setText("无线网络 已开启，尝试连接到'" + ssid + "'无线网络，最长花费" + tryingCount + "秒");
//
//					int networkId = -1;
//					List<WifiConfiguration> wifiConfigList = wifiManager.getConfiguredNetworks();
//					if (null != wifiConfigList) {
//						for (WifiConfiguration config : wifiConfigList) {
//							if (config.SSID.equals("\"" + ssid + "\"") || config.SSID.equals(ssid)) {
//								networkId = config.networkId;
//								break;
//							}
//						}
//					}
//					if (networkId != -1) {
//						wifiManager.enableNetwork(networkId, true);
//					}
//
//					if (null == qTask) {
//						qTask = new QueryTask();
//						qTask.execute("checkSSIDandIP");
//					}
//
//					break;
//				case 4:
//					msgView.setText("无线网络 未知");
//					break;
//				}
//			}
//		}
//	}

	@Override
	protected void onDestroy() {
//		if (null != wifiState) {
//			unregisterReceiver(wifiState);
//		}
		super.onDestroy();
	}
	
	@Override
	protected void setupActionBar() {
	}

	@Override
	public void onBackPressed() {
		finish();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		resolveNewIntent(intent);
//		super.onNewIntent(intent);
	}
	
	private void resolveNewIntent(Intent intent) {
		String msg = getIntent().getExtras().getString("msg");
		if (msg.equals("连接成功")) {
			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			finish();
		} else {
			msgView.setText(msg);
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == 138 || keyCode == 132) {
			PackageManager manager = getPackageManager();
			Intent i = manager.getLaunchIntentForPackage("com.android.settings");
			startActivity(i);
		} else if (keyCode == 135) {
			PackageManager manager = getPackageManager();
			Intent i = manager.getLaunchIntentForPackage("com.android.auto.iscan");
			if (null != i) {
				startActivity(i);
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
}
