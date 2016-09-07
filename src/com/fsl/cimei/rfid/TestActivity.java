package com.fsl.cimei.rfid;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;

import com.fsl.cimei.rfid.entity.MsgDBHelper;

public class TestActivity extends Activity {
	private String[] machNameArr = new String[] { "test1", "test2", "test3", "test4" };
	private boolean[] machNameSelect = new boolean[machNameArr.length];
	private ListView machNameListView;
	private ListView testListView;
	private List<HashMap<String, Object>> testListItem;
	private SimpleAdapter testSimpleAdapter;
	private static final String MACH_NAME = "MACH_NAME";
	private static final String MACH_MODEL = "MACH_MODEL";
	private static final String MACH_TYPE = "MACH_TYPE";
	
	private MsgDBHelper msgdb;
	private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private SimpleCursorAdapter adapter;
	private Cursor c;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);
		setupActionBar();
		testListView = (ListView) findViewById(R.id.test_listview);
		msgdb = new MsgDBHelper(TestActivity.this);
//		testListItem = new ArrayList<HashMap<String, Object>>();
//		HashMap<String, Object> m = new HashMap<String, Object>();
//		m.put(MACH_NAME, getResources().getString(R.string.mach_name));
//		m.put(MACH_MODEL, getResources().getString(R.string.mach_model));
//		m.put(MACH_TYPE, getResources().getString(R.string.mach_type));
//		testListItem.add(m);
//		for (int i = 1; i < 31; i++) {
//			HashMap<String, Object> mi = new HashMap<String, Object>();
//			mi.put(MACH_NAME, "name:" + i);
//			mi.put(MACH_MODEL, "model:" + i);
//			mi.put(MACH_TYPE, "type:" + i);
//			testListItem.add(mi);
//		}
//		testSimpleAdapter = new SimpleAdapter(TestActivity.this, testListItem, R.layout.lot_start_mach_list_item, new String[] { MACH_NAME, MACH_MODEL, MACH_TYPE }, new int[] { R.id.item1,
//				R.id.item2, R.id.item3 });
//		testListView.setAdapter(testSimpleAdapter);
//		testListView.setOnItemLongClickListener(new OnItemLongClickListener() {
//			@Override
//			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//				if (position != 0) {
//					Toast.makeText(TestActivity.this, "" + position, Toast.LENGTH_SHORT).show();
//					testListItem.remove(position);
//					testSimpleAdapter.notifyDataSetChanged();
//				}
//				return false;
//			}
//		});
		
//		c = msgdb.query(new String[]{"time","_id","content"}, "mach=?", new String[]{"BDB-12"}, null, null, "time desc", null);  
//        String[] from = { "time", "_id", "content" };  
//        int[] to = { R.id.item1, R.id.item2, R.id.item3 };  
//        adapter = new SimpleCursorAdapter(this, R.layout.lot_start_mach_list_item, c, from, to);
//        testListView.setAdapter(adapter);
//		
//
//		((Button) findViewById(R.id.button1)).setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				AlertDialog ad = new AlertDialog.Builder(TestActivity.this).setTitle("选择机台").setMultiChoiceItems(machNameArr, machNameSelect, new OnMultiChoiceClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
//					}
//				}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						String s = "";
//						for (int i = 0; i < machNameArr.length; i++) {
//							if (machNameListView.getCheckedItemPositions().get(i)) {
//								s += i + ":" + machNameListView.getAdapter().getItem(i) + "  ";
//							} else {
//								machNameListView.getCheckedItemPositions().get(i, false);
//							}
//						}
//						Toast.makeText(TestActivity.this, s, Toast.LENGTH_LONG).show();
//						dialog.dismiss();
//					}
//				}).setNegativeButton("取消", null).create();
//				machNameListView = ad.getListView();
//				ad.show();
//			}
//		});
//
//		((Button) findViewById(R.id.button2)).setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				ContentValues values = new ContentValues();
//				values.put("content", "msg006");
//				values.put("time", simpleDateFormat.format(new Date()));
//				values.put("sender", "host");
//				values.put("type", "msg");
//				values.put("mach", "test-01");
//				msgdb.insert(values);
//			}
//		});
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			 NavUtils.navigateUpFromSameTask(this);
			 return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
