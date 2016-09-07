package com.fsl.cimei.rfid;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.fsl.cimei.rfid.entity.MsgDBHelper;
import com.hume.DMH.DmhClient;
import com.hume.DMH.DmhClientItf;
import com.hume.DMH.DmhLostServerItf;
import com.hume.DMH.DmhReceiveItf;

public class MsgReceiveService3 extends Service {
	public static boolean msgReceiveFlag = false;
	public static final String TAG = "MsgReceive3";
	private MsgDBHelper msgdb;
	private String deviceID = "";
	public DmhClient dmhClient = null;
	public static String serverHost = ""; // liriver
	public static String groupName = "";
	private MsgHandler msgHandler = new MsgHandler();
	private Thread dmhThread = new Thread(new DmhThread());
	// private final String mailbox = "test111";
	private String message = "";
	private Context context;
	private int checkStateInterval = 6; // seconds

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
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		CommonUtility.logError("MsgReceiveService3.this.hashCode()=" + MsgReceiveService3.this.hashCode(), TAG);
		if (null != intent.getExtras() && intent.getExtras().containsKey("deviceID")) {
			deviceID = intent.getStringExtra("deviceID");
		}
		msgdb = new MsgDBHelper(MsgReceiveService3.this);
		context = MsgReceiveService3.this;
		if (!CommonUtility.isEmpty(deviceID) && !CommonUtility.isEmpty(serverHost) && !CommonUtility.isEmpty(groupName)) {
			dmhThread.start();
		} else {
			msgReceiveFlag = false;
			CommonUtility.logError("onStartCommand not start: " + "deviceID:" + deviceID + " serverHost:" + serverHost + " groupName:" + groupName, TAG);
		}
		return super.onStartCommand(intent, flags, startId);
	}

	class DmhThread implements Runnable {
		@Override
		public void run() {
			init();
			while (msgReceiveFlag) {
				try {
					Thread.sleep(1000 * checkStateInterval);
				} catch (InterruptedException e1) {
					CommonUtility.logError("dmhLostServer sleep " + e1.toString(), TAG);
				}
				if (null != dmhClient) {
					CommonUtility.logError("dmhClient.getState()==" + dmhClient.getState() + " dmhClient.getClientID()==" + dmhClient.getClientID(), TAG);
				}
				if (msgReceiveFlag && (null == dmhClient || dmhClient.getState() != 7)) {
//					try {
//						dmhClient.close(deviceID);
//					} catch (Exception e) {
//						CommonUtility.logError("dmh.close3 " + e.toString(), TAG);
//					}
//					dmhClient.disconnect();
					init();
				}
			}
		}
	}

	private void init() {
		if (!msgReceiveFlag || serverHost.isEmpty() || groupName.isEmpty()) {
			return;
		}
		if (dmhClient == null) {
			dmhClient = new DmhClient();
			CommonUtility.logError("dmhClient.hashCode()==" + dmhClient.hashCode(), TAG);
		}
		try {
			dmhClient.init(serverHost, groupName);
		} catch (Exception e) {
			CommonUtility.logError("init " + e.toString(), TAG);
		}
		dmhClient.setLostServer(new DmhLostServerItf() {
			@Override
			public void dmhLostServer(DmhClientItf dmh) {
				CommonUtility.logError("dmhLostServer", TAG);
				dmhClient = null;
//				try {
//					dmh.close(deviceID);
//				} catch (Exception e) {
//					CommonUtility.logError("dmh.close1 " + e.toString(), TAG);
//				}
//				dmh.disconnect();
			}
		});
		try {
			dmhClient.close(deviceID);
		} catch (Exception e) {
			CommonUtility.logError("dmh.close2 " + e.toString(), TAG);
		}
		try {
			dmhClient.whenMsg(deviceID, new DmhReceiveItf() {
				@Override
				public void dmhReceive(DmhClientItf dmh, String data, String destinationBox, String replyMailBox) throws Exception {
					if (!message.equals(data)) {
						message = data;
						CommonUtility.logError(data, TAG);
						msgHandler.handle(context, data, deviceID, msgdb);
					}
					dmh.whenMsgAgain();
				}
			});
		} catch (Exception e) {
			CommonUtility.logError(e.toString(), TAG);
		}
	}

	@Override
	public void onDestroy() {
		CommonUtility.logError(TAG + " onDestroy", TAG);
		msgReceiveFlag = false; 
		try {
			dmhClient.close(deviceID);
		} catch (Exception e) {
			CommonUtility.logError("dmh.close3 " + e.toString(), TAG);
		}
		dmhClient.disconnect();
		dmhThread.interrupt();
		super.onDestroy();
	}
}
