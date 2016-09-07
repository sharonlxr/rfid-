package com.fsl.cimei.rfid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.fsl.cimei.rfid.entity.MsgDBHelper;

public class MsgReceiveService4 extends Service {
	private Socket socket = null;
	private BufferedReader reader = null;
	private BufferedWriter writer = null;
	private MsgDBHelper msgdb;
	public static boolean msgReceiveFlag = true;
	private final String TAG = "MsgReceive4";
	private String deviceID = "";
	private MsgHandler msgHandler = new MsgHandler();
	public static String hostIP = "";// = "10.192.130.87" 06v // "10.192.154.104";
	public static int hostPort = 0; // 7002;
	private int interval = 10;
	private String oldMsg = "";
	private ScheduledExecutorService scheduledThreadPool = null;
//	private ScheduledExecutorService testThreadPool = null;

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
		String type = null;
		if (null != intent && intent.getExtras().containsKey("deviceID")) {
			deviceID = intent.getStringExtra("deviceID");
			type = intent.getStringExtra("type");
		}
		msgdb = new MsgDBHelper(getApplicationContext());
		if (msgReceiveFlag && hostPort > 7000 && !CommonUtility.isEmpty(hostIP)) {
			if (CommonUtility.isEmpty(type) && null == socket) {
				if (null != scheduledThreadPool) {
					scheduledThreadPool.shutdown();
				}
				close();
				scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();// .newScheduledThreadPool(1);
				scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
					@Override
					public void run() {
						boolean s = true;
						if (msgReceiveFlag && hostPort > 7000 && !CommonUtility.isEmpty(hostIP) && null == socket) {
							s = setupConnection();
						}
						if (s) {
							writeTest();
						}
					}
				}, 1, interval, TimeUnit.SECONDS);
			} else if (!CommonUtility.isEmpty(type) && type.equals("restart")) {
				CommonUtility.logError("restart", TAG);
				if (null != scheduledThreadPool) {
					scheduledThreadPool.shutdown();
				}
				close();
				scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();
				scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
					@Override
					public void run() {
						boolean s = true;
						if (msgReceiveFlag && hostPort > 7000 && !CommonUtility.isEmpty(hostIP) && null == socket) {
							s = setupConnection();
						}
						if (s) {
							writeTest();
						}
					}
				}, 2, interval, TimeUnit.SECONDS);
			}
		}
//		testThreadPool = Executors.newSingleThreadScheduledExecutor();
//		testThreadPool.scheduleWithFixedDelay(new Runnable() {
//			@Override
//			public void run() {
//				CommonUtility.logError(new Date().toString(), "testThreadPool");
//			}
//		}, 1, interval, TimeUnit.SECONDS);
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		CommonUtility.logError(TAG + " onDestroy", TAG);
		msgReceiveFlag = false;
		if (null != scheduledThreadPool) {
			scheduledThreadPool.shutdown();
		}
		close();
		msgdb.close();
		super.onDestroy();
	}

	private void close() {
		CommonUtility.logError(TAG + " close() ", TAG);
		oldMsg = "";
		if (null != writer) {
			try {
				writer.close();
			} catch (IOException e) {
				CommonUtility.logError(TAG + " 31 " + e.toString(), TAG);
			}
			writer = null;
		}
		if (null != reader) {
			try {
				reader.close();
			} catch (IOException e) {
				CommonUtility.logError(TAG + " 32 " + e.toString(), TAG);
			}
			reader = null;
		}
		if (null != socket) {
			try {
				socket.close();
			} catch (IOException e) {
				CommonUtility.logError(TAG + " 33 " + e.toString(), TAG);
			}
		}
		socket = null;
	}

	private boolean setupConnection() {
		boolean success = true;
		try {
			socket = new Socket(hostIP, hostPort);
		} catch (UnknownHostException e) {
			CommonUtility.logError("1:" + e.toString(), TAG);
			success = false;
		} catch (IOException e) {
			CommonUtility.logError("2:" + e.toString(), TAG);
			success = false;
		}
		if (success) {
			// try {
			// socket.setKeepAlive(true);
			// } catch (SocketException e) {
			// CommonUtility.logError("3 setKeepAlive:" + e.toString(), TAG);
			// }
			try {
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				CommonUtility.logError("0: setup socket connection successfully", TAG);
			} catch (IOException e) {
				CommonUtility.logError("3:" + e.toString(), TAG);
				success = false;
			}
		} else {
			close();
		}
		if (success) {
			new Thread(new ReaderThread()).start();
		}
		return success;
	}

	private class ReaderThread implements Runnable {
		@Override
		public void run() {
			if (null != writer && null != reader) {
				String command = null;
				try {
					command = reader.readLine();
				} catch (IOException e) {
					CommonUtility.logError("reader1:" + e.toString(), TAG);
					close();
				}
				while (command != null) {
					if (!CommonUtility.isEmpty(command)) {
						CommonUtility.logError("receive " + command, TAG);
						if (command.startsWith("CMD/A=\"TEST\"")) {
						} else {
							if (!command.equals(oldMsg)) {
								String reply = msgHandler.handle(MsgReceiveService4.this, command, deviceID, msgdb);
								if (null != writer) {
									try {
										CommonUtility.logError("reply " + reply, TAG);
										writer.write(reply + "\n");
										writer.flush();
										oldMsg = command;
									} catch (IOException e) {
										CommonUtility.logError("writer1:" + e.toString(), TAG);
										close();
									}
								}
							}
						}
					}
					if (null != reader) {
						try {
							command = reader.readLine();
						} catch (IOException e) {
							CommonUtility.logError("reader2:" + e.toString(), TAG);
							close();
						}
					} else {
						command = null;
					}
				}
			}
		}
	}

	private void writeTest() {
		if (null != writer) {
			long t = new Date().getTime() / 1000;
			try {
				// CommonUtility.logError("sendTest " + t, TAG);
				writer.write("CMD/A=\"TEST\" MID/A=\"TEST\" MESSAGE/A=\"TEST\" TID/U4=" + t + " MTY/A=\"C\"" + "\n");
				writer.flush();
			} catch (IOException e) {
				CommonUtility.logError("writer2:" + e.toString(), TAG);
				close();
			}
		}
	}
}
