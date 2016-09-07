package com.fsl.cimei.rfid;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.util.Log;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.MsgDBHelper;

public class MsgHandler {
	final String TAG = "MsgHandler";

	public final String handle(final Context context, final String command, final String deviceID, final MsgDBHelper msgdb) {
		Map<String, String> resultMap = CommonUtility.parseCommand(command);
		// CMD/A=DISPLAY_MSG MID/A=\"$machId\" HOSTNAME/A=\"$hostName\" MESSAGE/A=\"$vfeiMsg\" TID/U4=[localtime 3] MTY/A=C
		// CMD/A=\"CMD_ACK\" MID/A=\"$MID\" MTY/A=\"R\" TID/U4=$TID ECD/U4=10000 ETX/A=\"$reply\"
		if (resultMap.containsKey("CMD/A") && resultMap.get("CMD/A").equals("DISPLAY_MSG")) {
			String tid = resultMap.containsKey("TID/U4") ? resultMap.get("TID/U4") : "";
			String mach = resultMap.containsKey("MID/A") ? resultMap.get("MID/A") : "";
			String alertMsg = resultMap.get("MESSAGE/A");
			// bw.write("CMD/A=DISPLAY_MSG MID/A=" + mach + " MTY/A=R TID/U4=" + tid + " MESSAGE/A=\"" + alertMsg + "\" ECD/U4=0 ETX/A=0\n");
			boolean isAssignedMach = false;
			if (Constants.msgFilter) {
				SharedPreferences data = context.getSharedPreferences("RFID-data", Context.MODE_PRIVATE);
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
				String time = MsgReceiveService.simpleDateFormat.format(new Date());
				if (!CommonUtility.isEmpty(tid)) {
					try {
						long seconds = Long.parseLong(tid);
						Date date = new Date();
						date.setTime(seconds * 1000);
						time = MsgReceiveService.simpleDateFormat.format(date);
						// delete history message
						if (MsgReceiveService.deleteDate.isEmpty() || !MsgReceiveService.deleteDate.equals(time.substring(0, 10))) {
							long seconds2 = seconds - 3 * 24 * 60 * 60;
							Date temp = new Date(seconds2 * 1000);
							String t = MsgReceiveService.simpleDateFormat.format(temp).substring(0, 10) + " 00:00:00";
							int result = msgdb.delHist(t);
							CommonUtility.logError("Delete history message [" + t + "] " + result, Constants.LOG_FILE_MSG);
							MsgReceiveService.deleteDate = time.substring(0, 10);
						}
					} catch (NumberFormatException e) {
						Log.e(TAG, tid);
					}
				}

				boolean inserted = false;
				String[] parseResult = CommonUtility.translateMsg(alertMsg);
				String translated = parseResult[0];
				String msgType = parseResult[1];
				if (alertMsg.startsWith("ERROR")) {
					msgType = Constants.TYPE_END;
				}
				if (translated.equals(alertMsg)) {
					// CommonUtility.logError(alertMsg, Constants.LOG_FILE_MSG);
				} else {
					alertMsg = translated;
				}
				SharedPreferences data = context.getSharedPreferences("RFID-data", Context.MODE_PRIVATE);
				if (null != data && msgType.equals(Constants.TYPE_END)) {
					Editor e = data.edit();
					Set<String> alarmMach = data.getStringSet("alarmMach", new HashSet<String>());
					alarmMach.add(mach);
					e.putStringSet("alarmMach", alarmMach);
					e.commit();
				}
				// long t = new Date().getTime() / 1000;
				// CommonUtility.logError("insert 1 " + t + " " + tid, Constants.LOG_FILE_MSG);
				ContentValues values = new ContentValues();
				values.put("content", alertMsg);
				values.put("time", time);
				values.put("sender", "HOST");
				values.put("type", msgType);
				values.put("mach", mach);
				inserted = msgdb.insert(values);
				// t = new Date().getTime() / 1000;
				// CommonUtility.logError("insert 2 " + t + " " + tid, Constants.LOG_FILE_MSG);
				if (inserted) {
					Intent intent = new Intent();
					intent.setAction(Constants.FILTER_STRING_HOST);
					intent.putExtra("msg", alertMsg);
					intent.putExtra("mach", mach);
					intent.putExtra("time", time);
					intent.putExtra("type", msgType);
					context.sendBroadcast(intent);
					// t = new Date().getTime() / 1000;
					// CommonUtility.logError("sendBroadcast 3 " + t + " " + tid, Constants.LOG_FILE_MSG);
					// Intent i = new Intent(context, ControlActivity.class);
					// PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
					// NotificationManager notificationMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
					// // Notification n = (new Notification.Builder(MsgReceiveService.this).setContentIntent(pendingIntent).setSmallIcon(R.drawable.ic_launcher).setContentTitle("RFID：新消息 " +
					// // mach)
					// // .setContentText(alertMsg).setWhen(System.currentTimeMillis())).setAutoCancel(true).build();
					// @SuppressWarnings("deprecation")
					// Notification n = new Notification.Builder(context).setContentIntent(pendingIntent).setSmallIcon(R.drawable.ic_launcher)
					// .setContentTitle(time.substring(5, time.length() - 3) + " RFID消息: " + mach).setContentText(alertMsg).setWhen(System.currentTimeMillis())
					// .setAutoCancel(true).getNotification();
					// // Notification n = new Notification();
					// // n.icon = R.drawable.ic_launcher;
					// // n.when = System.currentTimeMillis();
					// // n.setLatestEventInfo(getApplicationContext(), "RFID：新消息 ", alertMsg, pendingIntent);
					// notificationMgr.notify(0, n);
				} else {
					// CommonUtility.logError(time + " " + mach + " " + msgType + " " + alertMsg, Constants.LOG_FILE_ERR);
				}
			}
			return command;
		} else if (resultMap.containsKey("CMD/A") && resultMap.get("CMD/A").equals("TEST")) {
			return command;
		} else if (resultMap.containsKey("CMD/A") && resultMap.get("CMD/A").equals("TEST2")) {
			int versionCode = 0;
			try {
				versionCode = context.getPackageManager().getPackageInfo(context.getApplicationContext().getPackageName(), 0).versionCode;
			} catch (NameNotFoundException e1) {
				Log.e(TAG, e1.toString());
			}
			String tid = resultMap.containsKey("TID/U4") ? resultMap.get("TID/U4") : "";
			String mach = resultMap.containsKey("MID/A") ? resultMap.get("MID/A") : "";
			String alertMsg = resultMap.get("MESSAGE/A");
			String time = MsgReceiveService.simpleDateFormat.format(new Date());
			if (!CommonUtility.isEmpty(tid)) {
				try {
					long seconds = Long.parseLong(tid);
					Date date = new Date();
					date.setTime(seconds * 1000);
					time = MsgReceiveService.simpleDateFormat.format(date);
				} catch (Exception e) {
					Log.e(TAG, tid);
				}
			}
			Intent intent = new Intent();
			intent.setAction(Constants.FILTER_STRING_HOST);
			intent.putExtra("msg", alertMsg);
			intent.putExtra("mach", mach);
			intent.putExtra("time", time);
			intent.putExtra("type", "TEST");
			context.sendBroadcast(intent);
			return deviceID + " " + versionCode;
		} else if (resultMap.containsKey("CMD/A") && resultMap.get("CMD/A").equals("UPLOAD_FILE")) {
			int versionCode = 0;
			try {
				versionCode = context.getPackageManager().getPackageInfo(context.getApplicationContext().getPackageName(), 0).versionCode;
			} catch (NameNotFoundException e1) {
				Log.e(TAG, e1.toString());
			}
			final String ip = resultMap.get("SERVERIP");
			final String port = resultMap.get("SERVERPORT");
			final String filename = resultMap.get("FILENAME");
			// final String tid = resultMap.get("TID/U4");
			new Thread(new Runnable() {
				@Override
				public void run() {
					File file = Environment.getExternalStorageDirectory();
					String uploadFile = file.getAbsolutePath() + "/rfid-log/" + filename;
					if (new File(uploadFile).exists()) {
						String fileName = deviceID + "_" + filename;
						CommonUtility.uploadFile(ip, port, uploadFile, fileName);
					}
				}
			}).start();
			return deviceID + " " + versionCode;
//		} else if (resultMap.containsKey("CMD/A") && resultMap.get("CMD/A").equals("UPDATE_CONFIG")) {
//			String updateType = resultMap.get("UPDATETYPE");
//			String type = resultMap.get("TYPE");
//			String owner = resultMap.get("OWNER");
//			String value = resultMap.get("VALUE");
//			SQLiteDatabase configDB = SQLiteDatabase.openOrCreateDatabase(context.getFilesDir() + "/config.db", null);
//			if (updateType.equals("C")) {
//				Cursor c = configDB.query("config", new String[] { "_id", "type", "owner", "value" }, "type=? and owner=?", new String[] { type, owner }, null, null,
//						null, null);
//				if (c.moveToNext()) {
//					ContentValues values = new ContentValues();
//					values.put("value", value);
//					configDB.update("config", values, "type=? and owner=?", new String[] { type, owner });
//				} else {
//					ContentValues values = new ContentValues();
//					values.put("type", type);
//					values.put("owner", owner);
//					values.put("value", value);
//					configDB.insert("config", null, values);
//				}
//			} else if (updateType.equals("D")) {
//				configDB.delete("config", "type=? and owner=?", new String[] { type, owner });
//			}
//			configDB.close();
//			int versionCode = 0;
//			try {
//				versionCode = context.getPackageManager().getPackageInfo(context.getApplicationContext().getPackageName(), 0).versionCode;
//			} catch (NameNotFoundException e1) {
//				Log.e(TAG, e1.toString());
//			}
//			return deviceID + " " + versionCode;
		}
		return command;
	}

}
