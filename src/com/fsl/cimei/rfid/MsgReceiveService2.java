package com.fsl.cimei.rfid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.MsgDBHelper;

public class MsgReceiveService2 extends Service {
	// private GlobalVariable global = null;
	private Socket socket = null;
	public static final String MSG_RECEIVE_SERVICE_ACTION = "com.fsl.cimei.rfid.MsgReceiveServiceAction2";
	@SuppressLint("SimpleDateFormat")
	private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private MsgDBHelper msgdb;
	private BufferedReader reader = null;
	private BufferedWriter writer = null;
	public static boolean msgReceiveFlag = false;

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
		// global = (GlobalVariable) getApplication();
		Log.v("MsgReceiveService2", "onCreate " + this.hashCode());
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v("MsgReceiveService2", "onStartCommand " + this.hashCode());
		msgdb = new MsgDBHelper(MsgReceiveService2.this);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (msgReceiveFlag) {
						closeConnection();
						msgReceiveFlag = true;
						connect();
						String initMsg = "CMD/A=INIT ECD/U4=0";
						writer.write(initMsg + "\n"); // global.getAndroidSecureID()
						writer.flush();
						Log.v("MsgReceiveService2", initMsg);
						String buffer = reader.readLine();
						while (buffer != null && msgReceiveFlag) {
							Log.v("MsgReceiveService2", buffer.trim());
							Map<String, String> resultMap = CommonUtility.parseCommand(buffer.trim());
							if (resultMap.containsKey("CMD/A") && resultMap.get("CMD/A").equals("DISPLAY_MSG")) {
								String mach = resultMap.containsKey("MID/A") ? resultMap.get("MID/A") : "";
								String tid = resultMap.containsKey("TID/U4") ? resultMap.get("TID/U4") : "";
								String alertMsg = resultMap.get("MESSAGE/A");
								String reply = "CMD/A=DISPLAY_MSG MID/A=\"" + mach + "\" MTY/A=R TID/U4=" + tid + " ECD/U4=0 ETX/A=0";
								Log.v("MsgReceiveService2", reply);
								writer.write(reply + "\n");
								writer.flush();

//								Date date = new Date();
//								if (!CommonUtility.isEmpty(tid)) {
//									date.setTime(Long.parseLong(tid) * 1000);
//								}
//								String time = simpleDateFormat.format(date);
//								if (mach.equals("mach") || alertMsg.equals("test")) {
//								} else {
//									String[] parseResult = CommonUtility.translateMsg(alertMsg);
//									String translated = parseResult[0];
//									String msgType = parseResult[1]; 
//									if (translated.equals(alertMsg)) {
//										CommonUtility.logError(alertMsg, Constants.LOG_FILE_MSG);
//									} else {
//										alertMsg = translated;
//									}
//									ContentValues values = new ContentValues();
//									values.put("content", alertMsg);
//									values.put("time", time);
//									values.put("sender", "HOST");
//									values.put("type", msgType);
//									values.put("mach", mach);
//									msgdb.insert(values);
//								}
//
//								Intent showMsgIntent = new Intent();
//								showMsgIntent.setAction(Constants.FILTER_STRING_HOST);
//								showMsgIntent.putExtra("msg", alertMsg);
//								showMsgIntent.putExtra("mach", mach);
//								showMsgIntent.putExtra("time", time);
//								showMsgIntent.putExtra("type", msgType);
//								sendBroadcast(showMsgIntent);
//
//								Intent i = new Intent(MsgReceiveService2.this, ControlActivity.class);
//								PendingIntent pendingIntent = PendingIntent.getActivity(MsgReceiveService2.this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
//								NotificationManager notificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//								Notification n = new Notification.Builder(MsgReceiveService2.this).setContentIntent(pendingIntent).setSmallIcon(R.drawable.ic_launcher)
//										.setContentTitle(time.substring(5, time.length() - 3) + " RFID消息: " + mach).setContentText(alertMsg)
//										.setWhen(System.currentTimeMillis()).setAutoCancel(true).getNotification();
//								notificationMgr.notify(0, n);
								
								boolean isAssignedMach = false;
								if (Constants.msgFilter) {
									SharedPreferences data = MsgReceiveService2.this.getSharedPreferences("RFID-data", MODE_PRIVATE);
									if (null != data) {
										Set<String> assignedMach = data.getStringSet("assignedMach", new HashSet<String>());
										if (!assignedMach.isEmpty()) {
											for (String temp : assignedMach) {
												if (temp.equals(mach)) {
													isAssignedMach = true;
													break;
												}
											}
										}
									}
								} else {
									isAssignedMach = true;
								}
								
								if (isAssignedMach) {
									String time = simpleDateFormat.format(new Date());
									if (!CommonUtility.isEmpty(tid)) {
										try {
											long seconds = Long.parseLong(tid);
											Date date = new Date();
											date.setTime(seconds * 1000);
											time = simpleDateFormat.format(date);
										} catch (Exception e) {
											Log.v("MsgReceiveService", tid);
										}
									}

									boolean inserted = false;
									String[] parseResult = CommonUtility.translateMsg(alertMsg);
									String translated = parseResult[0];
									String msgType = parseResult[1]; 
									if (translated.equals(alertMsg)) {
										CommonUtility.logError(alertMsg, Constants.LOG_FILE_MSG);
									} else {
										alertMsg = translated;
									}
									ContentValues values = new ContentValues();
									values.put("content", alertMsg);
									values.put("time", time);
									values.put("sender", "HOST");
									values.put("type", msgType);
									values.put("mach", mach);
									inserted = msgdb.insert(values);

									if (inserted) {
										Intent intent = new Intent();
										intent.setAction(Constants.FILTER_STRING_HOST);
										intent.putExtra("msg", alertMsg);
										intent.putExtra("mach", mach);
										intent.putExtra("time", time);
										intent.putExtra("type", msgType);
										sendBroadcast(intent);
										
										Intent i = new Intent(MsgReceiveService2.this, ControlActivity.class);
										PendingIntent pendingIntent = PendingIntent.getActivity(MsgReceiveService2.this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
										NotificationManager notificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
										@SuppressWarnings("deprecation")
										Notification n = new Notification.Builder(MsgReceiveService2.this).setContentIntent(pendingIntent).setSmallIcon(R.drawable.ic_launcher)
										.setContentTitle(time.substring(5, time.length() - 3) + " RFID消息: " + mach).setContentText(alertMsg)
										.setWhen(System.currentTimeMillis()).setAutoCancel(true).getNotification();
										notificationMgr.notify(0, n);
									}
								}
							}
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								Log.v("MsgReceiveService2", e.toString());
							}
							buffer = reader.readLine();
						}
					}
				} catch (IOException e) {
					Log.v("MsgReceiveService2", e.toString());
					closeConnection();
				}
			}
		}).start();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		Log.v("MsgReceiveService2", "onDestroy");
		closeConnection();
		super.onDestroy();
	}

	private void connect() throws IOException {
		socket = new Socket("10.192.157.19", 5560);
		// socket = new Socket("10.192.155.63", 5560, InetAddress.getLocalHost(), 47776);
//		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//		socket = new Socket();
//		socket.bind(new InetSocketAddress(CommonUtility.intToIp(wifiInfo.getIpAddress()), 47776));
//		socket.connect(new InetSocketAddress("10.192.157.19", 5560));
		socket.setOOBInline(true);
		socket.setKeepAlive(true);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
	}

	private void closeConnection() {
		msgReceiveFlag = false;
		// if (null != reader) {
		// try {
		// reader.close();
		// } catch (IOException e) {
		// Log.v("MsgReceiveService2", "close reader " + e.toString());
		// }
		// reader = null;
		// }
		// if (null != writer) {
		// try {
		// writer.close();
		// } catch (IOException e) {
		// Log.v("MsgReceiveService2", "close writer " + e.toString());
		// }
		// writer = null;
		// }
		if (null != socket) {
			try {
				socket.close();
			} catch (IOException e) {
				Log.v("MsgReceiveService2", "close socket " + e.toString());
			}
			// socket = null;
		}
	}

}
