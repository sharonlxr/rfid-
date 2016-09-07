package com.fsl.cimei.rfid;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Date;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.fsl.cimei.rfid.entity.MsgDBHelper;

public class MsgReceiveService1 extends Service {
	private boolean msgReceiveFlag = false;
	public static boolean regularSendFlag = false;
	private final String TAG = "MsgReceive1";
	private DatagramSocket regular = null;
	private DatagramSocket server = null;
	private Thread regularSendThread = new Thread(new RegularSendThread());
	private Thread receiveThread = new Thread(new ReceiveThread());
	public static int serverPort = 0;
	private MsgDBHelper msgdb;
	private String deviceID = "";
	private MsgHandler msgHandler = new MsgHandler();
	private int interval = 60;
	private String serverIPAddress = "10.192.130.87"; // 06v

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
		Log.e(TAG, "MsgReceiveService1.this.hashCode()=" + MsgReceiveService1.this.hashCode());
		if (intent.getExtras().containsKey("deviceID")) {
			deviceID = intent.getStringExtra("deviceID");
		}
		msgReceiveFlag = true;
		regularSendFlag = true;
		if (serverPort >= 7000) {
			msgdb = new MsgDBHelper(MsgReceiveService1.this);
			Log.e(TAG, "receiveThread.isAlive()=" + receiveThread.isAlive() + "  " + receiveThread.getState());
			Log.e(TAG, "regularSendThread.isAlive()=" + regularSendThread.isAlive() + "  " + regularSendThread.getState());
			if (receiveThread.getState().equals(Thread.State.NEW)) {
				receiveThread.start();
			} else if (receiveThread.getState().equals(Thread.State.TERMINATED)) {
				receiveThread = new Thread(new ReceiveThread());
				receiveThread.start();
			}
			if (regularSendThread.getState().equals(Thread.State.NEW)) {
				regularSendThread.start();
			} else if (regularSendThread.getState().equals(Thread.State.TERMINATED)) {
				regularSendThread = new Thread(new RegularSendThread());
				regularSendThread.start();
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		CommonUtility.logError(TAG + " onDestroy", TAG);
		regularSendFlag = false;
		msgReceiveFlag = false;
		if (null != server) {
			server.close();
		}
		regularSendThread.interrupt();
		if (null != regular) {
			regular.close();
		}
		super.onDestroy();
	}

	class RegularSendThread implements Runnable {
		@Override
		public void run() {
			try {
				if (null == regular) {
					regular = new DatagramSocket(40311);
				}
				regular.setReceiveBufferSize(512);
			} catch (SocketException e) {
				CommonUtility.logError(TAG + " send21 " + e.toString(), TAG);
				regularSendFlag = false;
				if (null != regular) {
					regular.close();
					regular = null;
				}
			}
			if (null != regular) {
				byte[] buf = new byte[512];
				InetSocketAddress socketAddress = new InetSocketAddress(serverIPAddress, serverPort); // liriver 10.192.130.159
				DatagramPacket packet2 = null;
				try {
					packet2 = new DatagramPacket(buf, buf.length, socketAddress);
				} catch (SocketException e) {
					CommonUtility.logError(TAG + " send22 " + e.toString(), TAG);
					regularSendFlag = false;
				}
				while (regularSendFlag) {
					try {
						long t = new Date().getTime() / 1000;
						String msg = "CMD/A=\"TEST\" MID/A=\"TEST\" MESSAGE/A=\"TEST\" TID/U4=" + t + " MTY/A=\"C\"";
						packet2.setData(msg.getBytes());
						CommonUtility.logError("send " + msg, TAG);
						regular.send(packet2);
					} catch (IOException e) {
						CommonUtility.logError(TAG + " send23 " + e.toString(), TAG);
					}
					try {
						Thread.sleep(1000 * interval);
					} catch (InterruptedException e) {
						CommonUtility.logError(TAG + " send24 " + e.toString(), TAG);
						regularSendFlag = false;
					}
				}
				if (null != regular) {
					regular.close();
					regular = null;
				}
			}
		}
	}

	class ReceiveThread implements Runnable {
		@Override
		public void run() {
			try {
				if (null == server) {
					server = new DatagramSocket(5000);
				}
				server.setReceiveBufferSize(512);
			} catch (SocketException e) {
				CommonUtility.logError(TAG + " receive31 " + e.toString(), TAG);
				msgReceiveFlag = false;
				if (null != server) {
					server.close();
					server = null;
				}
			}
			if (null != server) {
				byte[] buf = new byte[512];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				while (msgReceiveFlag) {
					try {
						server.receive(packet);
					} catch (IOException e) {
						// throw exception when close server
						CommonUtility.logError(TAG + " receive32 " + e.toString(), TAG);
						msgReceiveFlag = false;
					}
					if (msgReceiveFlag) {
						String command = new String(packet.getData(), 0, packet.getLength());
						CommonUtility.logError("receive " + command, TAG);
						String reply = msgHandler.handle(MsgReceiveService1.this, command, deviceID, msgdb);
						if (reply.startsWith("CMD/A=\"TEST\"")) {
							CommonUtility.logError("reply " + reply, TAG);
						} else {
							DatagramPacket dp = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
							dp.setData(reply.getBytes());
							try {
								CommonUtility.logError("reply " + reply, TAG);
								server.send(dp);
							} catch (IOException e) {
								CommonUtility.logError(TAG + " receive33 " + e.toString(), TAG);
							}
						}
					}
				}
				if (null != server) {
					server.close();
					server = null;
				}
			}
		}
	}

}
