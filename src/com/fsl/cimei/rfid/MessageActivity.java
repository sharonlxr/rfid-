package com.fsl.cimei.rfid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.freescale.api.Constants;

public class MessageActivity extends Activity {

	protected GlobalVariable global = null;
	// private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private ListView msgListView;
	private MsgAdapter msgAdapter;
	private final String MESSAGE = "MESSAGE";
	private final String MACHINE = "MACHINE";
	private final String TIME = "TIME";
	private final String TYPE = "TYPE";
	private TextView msgCount;
	private Button clearButton;
	private PowerManager.WakeLock mWakeLock = null;
	// private KeyguardLock mKeyguardLock;
	private String TAG = "MessageActivity";
	private int errorColor;
	private int stepColor;
	private int msgColor;

	// private HostMsgReceiver hostMsgReceiver;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		global = (GlobalVariable) getApplication();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		if (Constants.type == 1 || Constants.type == 3) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, TAG);
		}
		// KeyguardManager keyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
		// mKeyguardLock = keyguardManager.newKeyguardLock("TAG");

		setContentView(R.layout.activity_message);
		errorColor = getResources().getColor(R.color.bg_red);
		stepColor = getResources().getColor(R.color.bg_blue);
		msgColor = getResources().getColor(R.color.bg_black);
		// hostMsgReceiver = new HostMsgReceiver();
		// IntentFilter filterHost = new IntentFilter();
		// filterHost.addAction(Constants.FILTER_STRING_HOST_2);
		// registerReceiver(hostMsgReceiver, filterHost);
		msgListView = (ListView) findViewById(R.id.message_list_view);
		// msgAdapter = new SimpleAdapter(MessageActivity.this, global.getMsgListItem(), R.layout.host_message_list_item, new String[] { MESSAGE, MACHINE, TIME },
		// new int[] { R.id.message_content, R.id.message_mach, R.id.message_time });
		msgAdapter = new MsgAdapter(MessageActivity.this);
		msgListView.setAdapter(msgAdapter);
		msgCount = (TextView) findViewById(R.id.message_count);
		msgCount.setText("" + global.getMsgListItem().size());
		clearButton = (Button) findViewById(R.id.message_clear_button);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				global.getMsgListItem().clear();
				msgAdapter.notifyDataSetChanged();
				msgCount.setText("" + global.getMsgListItem().size());
			}
		});
		// Log.e("MessageAct", "onCreate");
		// resolveNewIntent(getIntent());
	}

	@Override
	protected void onResume() {
		// Log.e("MessageAct", "onResume");
		global.setFirstNewMsg(false);
		global.setShown(true);
		super.onResume();
	}

	@Override
	protected void onPause() {
		// Log.e("MessageAct", "onPause");
		global.setShown(false);
		super.onPause();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// Log.e("MessageAct", "onNewIntent");
		setIntent(intent);
		resolveNewIntent(intent);
	}

	private void resolveNewIntent(Intent intent) {
		// if (null == intent.getExtras()) {
		// return;
		// }
		// String msg = intent.getExtras().getString("msg", "");
		// String mach = intent.getExtras().getString("mach", "");
		// String time = intent.getExtras().getString("time", "");
		// String type = intent.getExtras().getString("type", "");
		// if (!msg.isEmpty()) {
		// HashMap<String, String> map = new HashMap<String, String>();
		// map.put(MESSAGE, msg);
		// map.put(MACHINE, mach);
		// map.put(TIME, time);
		// map.put(TYPE, type);
		// global.getMsgListItem().add(0, map);
		// msgAdapter.notifyDataSetChanged();
		// msgCount.setText("" + global.getMsgListItem().size());
		// if (Constants.type == 1 || Constants.type == 3) {
		// acquireWakeLock(1000);
		// }
		// }
		msgAdapter.notifyDataSetChanged();
		msgCount.setText("" + global.getMsgListItem().size());
		if (Constants.type == 1 || Constants.type == 3) {
			acquireWakeLock(1000);
		}
	}

	public void acquireWakeLock(long milltime) {
		mWakeLock.acquire(milltime);
		// mKeyguardLock.disableKeyguard();
	}

	public void releaseWakeLock() {
		if (mWakeLock != null && mWakeLock.isHeld()) {
			mWakeLock.release();
			mWakeLock = null;
		}
		// mKeyguardLock.reenableKeyguard();
	}

	@Override
	protected void onDestroy() {
		// Log.e("MessageAct", "onDestroy");
		// unregisterReceiver(hostMsgReceiver);
		if (Constants.type == 1 || Constants.type == 3) {
			releaseWakeLock();
		}
		super.onDestroy();
	}

	class MsgAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private ViewHolder holder = null;

		public MsgAdapter(Context context) {
			this.mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return global.getMsgListItem().size();
		}

		@Override
		public Object getItem(int arg0) {
			return global.getMsgListItem().get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.host_message_list_item, null);
				holder.content = (TextView) convertView.findViewById(R.id.message_content);
				holder.mach = (TextView) convertView.findViewById(R.id.message_mach);
				holder.time = (TextView) convertView.findViewById(R.id.message_time);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			// MESSAGE, MACHINE, TIME
			holder.content.setText(global.getMsgListItem().get(position).get(MESSAGE));
			if (global.getMsgListItem().get(position).get(TYPE).equals(Constants.TYPE_MSG)) {
				holder.content.setTextColor(msgColor);
			} else if (global.getMsgListItem().get(position).get(TYPE).equals(Constants.TYPE_ERROR)
					|| global.getMsgListItem().get(position).get(TYPE).equals(Constants.TYPE_END)
					|| global.getMsgListItem().get(position).get(TYPE).equals(Constants.TYPE_MISSING)) {
				holder.content.setTextColor(errorColor);
			} else if (global.getMsgListItem().get(position).get(TYPE).equals(Constants.TYPE_STEP)) {
				holder.content.setTextColor(stepColor);
			}
			holder.mach.setText(global.getMsgListItem().get(position).get(MACHINE));
			holder.time.setText(global.getMsgListItem().get(position).get(TIME));
			return convertView;
		}
	}

	class ViewHolder {
		public TextView content;
		public TextView mach;
		public TextView time;
	}

	// class HostMsgReceiver extends BroadcastReceiver {
	//
	// @Override
	// public void onReceive(Context context, Intent intent) {
	// final String msg = intent.getStringExtra("msg");
	// final String mach = intent.getStringExtra("mach");
	// final String time = intent.getStringExtra("time");
	// final String type = intent.getStringExtra("type");
	// Log.e("RFID", "Message HostMsgReceiver");
	// String MESSAGE = "MESSAGE";
	// String MACHINE = "MACHINE";
	// String TIME = "TIME";
	// String TYPE = "TYPE";
	// if (!msg.isEmpty()) {
	// HashMap<String, String> map = new HashMap<String, String>();
	// map.put(MESSAGE, msg);
	// map.put(MACHINE, mach);
	// map.put(TIME, time);
	// map.put(TYPE, type);
	// global.getMsgListItem().add(0, map);
	// }
	// }
	// }
}
