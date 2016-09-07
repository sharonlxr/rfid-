package com.fsl.cimei.rfid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.example.nfc.util.AS3911;
import com.example.nfc.util.Consts;
import com.example.nfc.util.Tools;

public class NFCService extends Service {
	private AS3911 as3911;
	private OutputStream mOutputStream;
	private InputStream mInputStream;

	private String activity = "";
	private MyReceiver myReceive; // 广播接收者
	private ReadThread mReadThread; // 读数据线程
	private boolean run = true;

	private int cmdCode = 0;
	private boolean cmdFlag = false;
	private static final String TAG = "NFCService";

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	private int port = 13;
	private int buadrate = 115200;

	@Override
	public void onCreate() {
		super.onCreate();
		try {
			as3911 = new AS3911(port, buadrate, 0); // 设备串口号为13，波特率为115200
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (as3911 == null) {
			return;
		}
		AS3911.powerOn();// 打开电源
		mOutputStream = as3911.getOutputStream();
		mInputStream = as3911.getInputStream();

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		byte[] power = new byte[16];
		try {
			int size = mInputStream.read(power);
			Log.i(TAG, "power ");
		} catch (IOException e) {
			e.printStackTrace();
		}
		myReceive = new MyReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.fsl.cimei.service.NFCService");
		registerReceiver(myReceive, filter);
		// 注册Broadcast Receiver，用于关闭Service

		mReadThread = new ReadThread();
		mReadThread.start(); // 开启读线程
		Log.i(TAG, "start thread");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		cmdCode = intent.getIntExtra("cmd", 0);
		if (cmdCode == 0) {
			cmdFlag = true;
		} else {
			cmdFlag = false;
		}
		Log.i(TAG, "onStartCommand CMD CODE :  " + cmdCode + "");
		return 0;
	}

	/**
	 * @return void
	 * @Method Description:执行指令
	 * @Autor Jimmy
	 * @Date 2013-11-21
	 */
	private void exeCmd(int cmdcode) {
		byte[] cmdArr = null;
		switch (cmdcode) {
		case Consts.Get_version:
			cmdArr = as3911.getHardwareVersion();// 获取设备版本
			break;
		case Consts.Init_14443a:
			cmdArr = as3911.card14443aInit(); // 14443A初始化
			break;
		case Consts.GetUID_14443a:
			cmdArr = as3911.card14443aSelect(); // 14443A寻卡
			break;
		case Consts.DeInit_14443a:
			cmdArr = as3911.card14443aDeInit(); // 14443A取消初始化
			break;
		case Consts.Mifare_14443aInit:
			cmdArr = as3911.mifare14443aInit();// mifare初始化
			break;
		case Consts.Mifare_14443aAuth:
			cmdArr = as3911.mifare14443aAuth(Consts.AUTH_INFO_14443A); // mifare认证
			break;
		case Consts.Mifare_14443aRead:
			cmdArr = as3911.mifare14443aRead(Consts.READ_INFO_14443A);// mifare读卡
			break;
		case Consts.Mifare_14443aWrite:
			cmdArr = as3911.mifare14443aWrite(Consts.WRITE_INFO_14443A);// mifare写数据
			break;
		case Consts.CmdCode_Init_15693:// 15693初始化
			cmdArr = as3911.card15693Init();
			break;
		case Consts.CmdCode_DeInit_15693:// 15693取消初始化
			cmdArr = as3911.card15693DeInit();
			break;
		case Consts.CmdCode_1_SlotInventory_15693:// 1 slot inventory
			cmdArr = as3911.card15693OneSlotInventory();
			break;
		case Consts.CmdCode_GetSysInfo_15693:// 获取15693卡系统信息
			cmdArr = as3911.card15693GetSysInfo(Consts.Card15693_UID);
			break;
		case Consts.CmdCode_ReadSingleBlock_15693:// 读取单独一块
			cmdArr = as3911.card15693Read(Consts.Card15693_UID_Block);
			break;
		case Consts.CmdCode_WriteSingleBlock_15693:// 写单独一块
			cmdArr = as3911.card15693Write(Consts.Card15693_UID_Block_Data);
			break;
		default:
			break;
		}

		if (cmdArr != null) {
			try {
				mOutputStream.write(cmdArr); // 发送命令
				//Log.i(TAG, "SEN CMD SUCCESS" + cmdcode);
				//Log.i(TAG, "CMD" + Tools.Bytes2HexString(cmdArr, cmdArr.length));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onDestroy() {
		if (mReadThread != null)
			run = false; // 关闭线程
		AS3911.powerOff();
		as3911.close(port); // 关闭串口
		unregisterReceiver(myReceive); // 卸载注册
		Log.e(TAG, "service onDestroy");
		super.onDestroy();

	}

	/**
	 * 读线程 ,读取设备返回的信息，将其回传给发送请求的activity
	 * 
	 * @author Jimmy Pang
	 * 
	 */
	private class ReadThread extends Thread {
		byte[] recv_data = null;
		String cardUID = null;// 卡片UID
		Intent recvIntent = new Intent();

		@Override
		public void run() {
			super.run();
			while (run) {
				if (!cmdFlag) {
					recv_data = null;
					recvIntent.setAction("com.fsl.cimei.rfid.hfdemo.NewNFCTagActivity");
					// 获取版本信息
					if (cmdCode == Consts.Get_version) {
						// Log.i(TAG, "get version");
						exeCmd(Consts.Get_version);
						recv_data = getRecv_data();
						if (recv_data != null) {
							String version;
							try {
								version = new String(recv_data, "UTF-8");
								// Log.i(TAG, "version " + version);
								recvIntent.putExtra("result", version);
								sendBroadcast(recvIntent);
								recv_data = null;
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
						}
						cmdCode = 0;
					}
					// 初始化14443A
					if (cmdCode == Consts.Init_14443a) {
						//Log.i(TAG, "14443A init*******");
						exeCmd(Consts.Init_14443a);// 初始化
						recv_data = getRecv_data();
						if (recv_data != null) {
							cmdCode = Consts.GetUID_14443a;
							recv_data = null;
						} else {
							// cmdCode = Consts.CmdCode_DeInit_15693;
							cmdCode = Consts.CmdCode_Init_15693;
						}
					}
					// 获取UID
					if (cmdCode == Consts.GetUID_14443a) {
						//Log.i(TAG, "14443A GET UID*******");
						exeCmd(Consts.GetUID_14443a);// 寻卡
						recv_data = getRecv_data();
						if (recv_data != null) {
							cardUID = Tools.resolveUID(Tools.Bytes2HexString(recv_data, recv_data.length));
							//
							if (cardUID != null && !cardUID.equals("")) {
								//Log.i(TAG, "14443A  " + cardUID);
								recvIntent.putExtra("result", "ISO14443A " + cardUID);
								sendBroadcast(recvIntent);
							}
							// cmdCode = Consts.CmdCode_DeInit_15693;
							cmdCode = Consts.CmdCode_Init_15693;
							// cmdCode = Consts.Init_14443a;
							recv_data = null;
						} else {
							// cmdCode = Consts.CmdCode_DeInit_15693;
							cmdCode = Consts.CmdCode_Init_15693;
							// cmdCode = Consts.Init_14443a;
						}
					}
					// 14443A取消初始化

					// 15693取消初始化
					if (cmdCode == Consts.CmdCode_DeInit_15693) {
						// Log.i(TAG, "15963 DEINIT *******");
						exeCmd(Consts.CmdCode_DeInit_15693);
						recv_data = getRecv_data();
						if (recv_data != null) {
							// cmdCode = Consts.CmdCode_1_SlotInventory_15693;
							cmdCode = Consts.CmdCode_Init_15693;
							recv_data = null;
						} else {
							cmdCode = Consts.Init_14443a;
							// cmdCode = 0;
						}
					}
					// 15693初始化
					if (cmdCode == Consts.CmdCode_Init_15693) {
						//Log.i(TAG, "15963 INIT *******");
						exeCmd(Consts.CmdCode_Init_15693);
						recv_data = getRecv_data();
						if (recv_data != null) {
							cmdCode = Consts.CmdCode_1_SlotInventory_15693;
						} else {
							cmdCode = Consts.Init_14443a;
							// cmdCode = 0;
						}
					}
					// 15693读UID
					if (cmdCode == Consts.CmdCode_1_SlotInventory_15693) {
						//Log.i(TAG, "15693 inventory***");
						exeCmd(Consts.CmdCode_1_SlotInventory_15693);
						recv_data = getRecv_data();
						if (recv_data != null) {
							String card15693UID = Tools.resolve15693UID(Tools.Bytes2HexString(recv_data, recv_data.length));
							//Log.i(TAG, "15693 uid : " + card15693UID);
							recvIntent.putExtra("result", "ISO15693 " + card15693UID);
							sendBroadcast(recvIntent);
							cmdCode = Consts.Init_14443a;
						} else {
							cmdCode = Consts.Init_14443a;
						}
					}
				}
			}
		}
	}

	/**
	 * @return byte[]
	 * @Method Description:从串口中获取数据
	 * @Autor Jimmy
	 * @Date 2013-12-3
	 */
	private byte[] getRecv_data() {
		int count = 0;
		int index = 0;
		byte[] recvDataBuffer = null;
		byte[] recvData = null;
		try {
			while (count < 1) {
				count = mInputStream.available();
				// 读取数据超时
				if (index > 50) {
					return null;
				} else {
					index++;
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			count = mInputStream.available();
			recvDataBuffer = new byte[count];
			mInputStream.read(recvDataBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		/* =============测试数据============== */
		if (recvDataBuffer != null) {
			recvData = as3911.resolveResp(recvDataBuffer);
			// Log.e("recvData", Tools.Bytes2HexString(recvDataBuffer, recvDataBuffer.length));
		}
		return recvData;
	}

	/**
	 * 广播接受者
	 * 
	 * @author Jimmy Pang
	 * 
	 */
	private class MyReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String ac = intent.getStringExtra("activity");
			if (ac != null)
				Log.i("receive activity", ac);
			activity = ac; // 获取activity
			if (intent.getBooleanExtra("stopflag", false))
				stopSelf(); // 收到停止服务信号
			Log.i("stop service", intent.getBooleanExtra("stopflag", false) + "");
		}
	}
}
