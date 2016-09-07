package com.fsl.cimei.rfid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.MsgDBHelper;

public class MsgReceiveService extends Service {
	private ServerSocket serverSocket = null;
	public static final String MSG_RECEIVE_SERVICE_ACTION = "com.fsl.cimei.rfid.MsgReceiveServiceAction";
	public final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
	private MsgDBHelper msgdb;
	public static boolean msgReceiveFlag = true;
	private final String TAG = "MsgReceive0";
	private String deviceID = "";
	public static String deleteDate = "";
	private MsgHandler msgHandler = new MsgHandler();
	private String oldMsg = "";
	// private ScheduledExecutorService scheduledThreadPool = null;

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
		if (null != intent && intent.getExtras().containsKey("deviceID")) {
			deviceID = intent.getStringExtra("deviceID");
		}
		msgdb = new MsgDBHelper(MsgReceiveService.this);
		if (null != serverSocket && !serverSocket.isClosed()) {
			CommonUtility.logError(TAG + " onStartCommand serverSocket " + serverSocket.hashCode(), TAG);
			return super.onStartCommand(intent, flags, startId);
		}
		try {
			serverSocket = new ServerSocket(Constants.port);
			CommonUtility.logError(TAG + " onStartCommand new serverSocket " + serverSocket.hashCode(), TAG);
		} catch (IOException e) {
			CommonUtility.logError(TAG + " 1 " + e.toString(), TAG);
		}
		new Thread() {
			public void run() {
				Socket socket = null;
				try {
					while (msgReceiveFlag) {
						socket = serverSocket.accept();
						CommonUtility.logError(TAG + " accept socket " + socket.hashCode(), TAG);
						new Thread(new SocketClient(socket)).start();
					}
				} catch (IOException e) {
					CommonUtility.logError(TAG + " 2 " + e.toString(), TAG);
				}
			}
		}.start();
		/*
		scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();
		scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				Intent msgReceive4Intent = new Intent(getApplicationContext(), MsgReceiveService4.class);
				msgReceive4Intent.putExtra("deviceID", deviceID);
				msgReceive4Intent.putExtra("type", "restart");
				startService(msgReceive4Intent);
			}
		}, 2, 2, TimeUnit.HOURS);*/
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		CommonUtility.logError(TAG + " onDestroy", TAG);
		msgReceiveFlag = false;
		//if (null != scheduledThreadPool) {
			//scheduledThreadPool.shutdown();
		//}
		if (null != serverSocket) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				CommonUtility.logError(TAG + " 3 " + e.toString(), TAG);
			}
		}
		super.onDestroy();
	}

	private class SocketClient implements Runnable {

		Socket s;

		SocketClient(Socket s) {
			this.s = s;
		}

		
		@Override
		public void run() {
			try {
				String clientName = s.getInetAddress().getHostName() + "||" + s.getInetAddress().toString();
				// CommonUtility.logError(TAG + " clientSocket run " + clientName, TAG);
				Log.v(TAG, clientName);
				BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
				String command = br.readLine();
				while (command != null) {
					CommonUtility.logError("receive " + command, TAG);
					if (command.startsWith("CMD/A=\"DISPLAY_MSG")) {
						if (!command.equals(oldMsg)) {
							oldMsg = command;
							String reply = msgHandler.handle(MsgReceiveService.this, command, deviceID, msgdb);
							bw.write(reply + "\n");
							bw.flush();
						} else {
							bw.write(command + "\n");
							bw.flush();
						}
					} else {
						String reply = msgHandler.handle(MsgReceiveService.this, command, deviceID, msgdb);
						bw.write(reply + "\n");
						bw.flush();
					}
					command = br.readLine();
				}
			} catch (IOException e) {
				CommonUtility.logError(TAG + " 4 " + e.toString(), TAG);
			}
		}
	}
}
