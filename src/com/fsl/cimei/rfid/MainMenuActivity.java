package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.Properties;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.freescale.api.Constants;

public class MainMenuActivity extends Activity {

	public static boolean hasMainMenu;// = true;
	private ToggleButton hasMainMenuToggle;
	private ListView mainMenuList;
	public static SparseArray<String> menuMap; // Map<Integer, String>
	public static SparseBooleanArray selectedMenuMap; // Map<Integer, Boolean> selectedMenuMap;
	private MyAdapter mAdapter;
	private ArrayList<Integer> idList;
	private ArrayList<String> nameList;
	private String className = "Main_Menu";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);
		setupActionBar();
		hasMainMenuToggle = (ToggleButton) findViewById(R.id.main_menu_flag);
		hasMainMenuToggle.setTextOn("开启");
		hasMainMenuToggle.setTextOff("关闭");
		hasMainMenuToggle.setChecked(hasMainMenu);
		hasMainMenuToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				hasMainMenu = isChecked;
				if (hasMainMenu) {
					mainMenuList.setVisibility(View.VISIBLE);
				} else {
					mainMenuList.setVisibility(View.INVISIBLE);
				}
			}
		});
		mainMenuList = (ListView) findViewById(R.id.main_menu_list);
		if (selectedMenuMap == null || selectedMenuMap.size() == 0) {
			selectedMenuMap = new SparseBooleanArray();
			for (int i = 0; i < menuMap.size(); i++) {
				int key = menuMap.keyAt(i);
				selectedMenuMap.put(key, false);
			}
		}
		formMenuList();
		if (hasMainMenu) {
			mainMenuList.setVisibility(View.VISIBLE);
		} else {
			mainMenuList.setVisibility(View.INVISIBLE);
		}
	}

	private void formMenuList() {
		idList = new ArrayList<Integer>();
		nameList = new ArrayList<String>();
		if (menuMap.size() != 0) {
			for (int i = 0; i < menuMap.size(); i++) {
				int key = menuMap.keyAt(i);
				if (!Constants.alarmUnsetMenu && key == 23) {
				} else {
					idList.add(key);
					nameList.add(menuMap.get(key));
				}
			}
		}
		mAdapter = new MyAdapter(this);
		mainMenuList.setAdapter(mAdapter);
		mainMenuList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ViewHolder holder = (ViewHolder) view.getTag();
				holder.cb.toggle();
				selectedMenuMap.put(idList.get(position), holder.cb.isChecked());
			}
		});
	}

	private void storeSelectedMenu() {
		Properties p = new Properties();
		if (menuMap != null && menuMap.size() != 0 && selectedMenuMap != null && !(selectedMenuMap.size() == 0)) { // .isEmpty()
			if (hasMainMenu) {
				p.put(className, "T");
			} else {
				p.put(className, "F");
			}
			for (int i = 0; i < selectedMenuMap.size(); i++) {
				int key = selectedMenuMap.keyAt(i);
				if (menuMap.indexOfKey(key) >= 0) {
					p.put("" + key, selectedMenuMap.get(key) ? "T" : "F");
				}
			}
			CommonUtility.storePropertiesFile(p, className + ".conf");
		}
	}

	@Override
	protected void onPause() {
		// if (hasMainMenu) {
		storeSelectedMenu();
		// }
		super.onPause();
	}

	public class MyAdapter extends BaseAdapter {
		private LayoutInflater inflater = null;

		public MyAdapter(Context context) {
			inflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return nameList.size();
		}

		@Override
		public Object getItem(int position) {
			return nameList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				// 获得ViewHolder对象
				holder = new ViewHolder();
				// 导入布局并赋值给convertview
				convertView = inflater.inflate(R.layout.rack_mgmt_carrier_list_item, null);
				holder.tv = (TextView) convertView.findViewById(R.id.rack_mgmt_carrier_title);
				holder.cb = (CheckBox) convertView.findViewById(R.id.rack_mgmt_carrier_cb);
				holder.cb.setVisibility(View.VISIBLE);
				// 为view设置标签
				convertView.setTag(holder);
			} else {
				// 取出holder
				holder = (ViewHolder) convertView.getTag();
			}

			holder.tv.setText(nameList.get(position));
			// holder.cb.setChecked(selectedMenuMap.containsKey(idList.get(position)) ? selectedMenuMap.get(idList.get(position)) : false);
			holder.cb.setChecked(selectedMenuMap.indexOfKey(idList.get(position)) < 0 ? false : selectedMenuMap.get(idList.get(position)));
			return convertView;
		}

	}

	class ViewHolder {
		TextView tv;
		CheckBox cb;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected void setupActionBar() {
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
