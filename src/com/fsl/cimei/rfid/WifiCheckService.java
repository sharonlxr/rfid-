package com.fsl.cimei.rfid;

import java.text.SimpleDateFormat;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.RfidWifiException;

public class WifiCheckService extends Service {

	public static boolean wifiCheckFlag = false;
	private GlobalVariable global = null;
	private final String ssid = "Freescale";
	private WifiManager wifiManager;
	private int tryingCount = 20;
	private BroadcastReceiver wifiState;
	private final String classname = "WifiCheckService";

	@SuppressLint("SimpleDateFormat")
	private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private int timeSpan = 2 * 60 * 1000; // ms, 2 minutes

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public void onCreate() {
		Log.e(classname, "onCreate");
		global = (GlobalVariable) getApplication();
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiState = new WifiStateReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		registerReceiver(wifiState, filter);
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.e(classname, "onStartCommand");
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (wifiCheckFlag) {
					try {
						Thread.sleep(timeSpan);
					} catch (InterruptedException e1) {
						Log.e(classname, e1.toString());
					}
					try {
						Log.e(classname, "WifiCheck pingHost");
						CommonUtility.pingHost(global.getInterfaceMgrSocketConfigQuery().getHost());
						if (!MsgReceiveService.msgReceiveFlag) {
							MsgReceiveService.msgReceiveFlag = true;
							Intent msgReceive = new Intent(WifiCheckService.this, MsgReceiveService.class);
							startService(msgReceive);
						}
						// if (!MsgReceiveService2.msgReceiveFlag) {
						// MsgReceiveService2.msgReceiveFlag = true;
						// Intent msgReceive2 = new Intent(WifiCheckService.this, MsgReceiveService2.class);
						// startService(msgReceive2);
						// }
					} catch (RfidWifiException e) {
						if (wifiManager.isWifiEnabled()) {
							wifiManager.setWifiEnabled(false);
						} else {
							wifiManager.setWifiEnabled(true);
						}
					}
				}
			}
		}).start();

		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy() {
		Log.e(classname, "onDestroy");
		wifiCheckFlag = false;
		if (null != wifiState) {
			unregisterReceiver(wifiState);
		}
		super.onDestroy();
	}

	class WifiStateReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().endsWith(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
				switch (wifiState) {
				case 0:
					Log.e(classname, "无线网络 正在关闭");
					break;
				case 1:
					Log.e(classname, "无线网络 已关闭");
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e2) {
						Log.e(classname, e2.toString());
					}
					wifiManager.setWifiEnabled(true);
					break;
				case 2:
					Log.e(classname, "无线网络 正在开启");
					break;
				case 3:
					new Thread(new Runnable() {
						@Override
						public void run() {
							WifiInfo wifiInfo = wifiManager.getConnectionInfo();
							if (wifiInfo != null && wifiInfo.getSSID() != null && (wifiInfo.getSSID().equals(ssid) || wifiInfo.getSSID().equals("\"" + ssid + "\""))
									&& wifiInfo.getIpAddress() != 0) {
							} else {
								CommonUtility.logError("Open wifi", Constants.LOG_FILE_WIFI);
								Log.e(classname, "无线网络 已开启，尝试连接到'" + ssid + "'无线网络，最长花费" + tryingCount + "秒");
								Intent i = new Intent();
								i.setAction(Constants.FILTER_STRING_WIFI);
								i.putExtra("msg", "无线网络 已开启，尝试连接到'" + ssid + "'无线网络，最长花费" + tryingCount + "秒");
								sendBroadcast(i);

								int networkId = -1;
								List<WifiConfiguration> wifiConfigList = wifiManager.getConfiguredNetworks();
								if (null != wifiConfigList) {
									for (WifiConfiguration config : wifiConfigList) {
//										CommonUtility.logError(config.toString(), Constants.LOG_FILE_WIFI);
										if (config.SSID.equals("\"" + ssid + "\"") || config.SSID.equals(ssid)) {
											networkId = config.networkId;
											// break;
										}
									}
								}
								if (networkId != -1) {
									wifiManager.enableNetwork(networkId, true);
									int j = tryingCount;
									while (j > 0) {
										wifiInfo = wifiManager.getConnectionInfo();
										if (wifiInfo != null) {
											if (wifiInfo.getSSID() != null && (wifiInfo.getSSID().equals(ssid) || wifiInfo.getSSID().equals("\"" + ssid + "\""))
													&& WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState()).equals(DetailedState.OBTAINING_IPADDR)
													&& wifiInfo.getIpAddress() != 0) {
												break;
											}
										}
										try {
											Thread.sleep(1000);
										} catch (InterruptedException e1) {
											Log.e(classname, e1.toString());
										}
										j--;
									}
								} else {
									Log.e(classname, ssid + "无线网络未配置");
									Intent i2 = new Intent();
									i2.setAction(Constants.FILTER_STRING_WIFI);
									i2.putExtra("msg", ssid + "无线网络未配置");
									sendBroadcast(i2);
								}
							}
							
							// test if connected to host
							wifiInfo = wifiManager.getConnectionInfo();
							if (wifiInfo != null && wifiInfo.getSSID() != null
									&& (wifiInfo.getSSID().equals(ssid) || wifiInfo.getSSID().equals("\"" + ssid + "\"")) && wifiInfo.getIpAddress() != 0) {
								try {
									Log.e(classname, "测试 pingHost");
									CommonUtility.pingHost(global.getInterfaceMgrSocketConfigQuery().getHost());
									Log.e(classname, "连接host成功");
									Intent i6 = new Intent();
									i6.setAction(Constants.FILTER_STRING_WIFI);
									i6.putExtra("msg", "连接成功");
									sendBroadcast(i6);
									if (!MsgReceiveService.msgReceiveFlag) {
										MsgReceiveService.msgReceiveFlag = true;
										Intent msgReceive = new Intent(WifiCheckService.this, MsgReceiveService.class);
										startService(msgReceive);
									}
//									Intent intent1 = new Intent(WifiCheckService.this, MsgReceiveService2.class);
//									startService(intent1);
								} catch (RfidWifiException e) {
									Log.e(classname, e.toString()); // "20秒后仍无法连接到host"
									Intent i3 = new Intent();
									i3.setAction(Constants.FILTER_STRING_WIFI);
									i3.putExtra("msg", e.toString());
									sendBroadcast(i3);
								}
							} else {
								CommonUtility.logError("Fail", Constants.LOG_FILE_WIFI);
								Log.e(classname, "20秒后仍无法连接到" + ssid);
								Intent i4 = new Intent();
								i4.setAction(Constants.FILTER_STRING_WIFI);
								i4.putExtra("msg", "20秒后仍无法连接到" + ssid);
								sendBroadcast(i4);
							}
						}
					}).start();
					break;
				case 4:
					Log.e(classname, "无线网络 未知");
					Intent i5 = new Intent();
					i5.setAction(Constants.FILTER_STRING_WIFI);
					i5.putExtra("msg", "无线网络 未知");
					sendBroadcast(i5);
					break;
				}
			}
		}
	}

}
