package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.fsl.cimei.rfid.entity.MsgDBHelper;

public class MessageTestActivity extends BaseActivity {

	private MsgDBHelper msgdb;
	private SimpleAdapter machListAdapter;
	private List<HashMap<String, String>> msgListItem = new ArrayList<HashMap<String, String>>();
	private Cursor cursor;
	private ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_message_test);
		msgdb = new MsgDBHelper(MessageTestActivity.this);
		cursor = msgdb.query(new String[] { "time", "content", "_id", "type", "mach" }, null, null, null, null, "time desc", null);
		while (cursor.moveToNext()) {
			String time = cursor.getString(0);
			String content = cursor.getString(1);
			String id = cursor.getString(2);
			String type = cursor.getString(3);
			String mach = cursor.getString(4);
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("time", time);
			map.put("content", id + "===" + type + "===" + content);
			map.put("mach", mach);
			msgListItem.add(map);
		}
		machListAdapter = new SimpleAdapter(MessageTestActivity.this, msgListItem, R.layout.host_message_list_item, new String[] { "time", "content", "mach" },
				new int[] { R.id.message_time, R.id.message_content, R.id.message_mach });
		listView = (ListView) findViewById(R.id.msg_test_list);
		listView.setAdapter(machListAdapter);
	}

}
