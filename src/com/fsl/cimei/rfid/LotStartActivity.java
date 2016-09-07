package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.Mach;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class LotStartActivity extends BaseActivity {

	private QueryTask qTask;

	private DataCollection machListDC;
	private ListView machListView;
	private List<HashMap<String, Object>> machListItem;
	private SimpleAdapter machAdapter;
	private static final String MACH_NAME = "MACH_NAME";
	private static final String MACH_MODEL = "MACH_MODEL";
	private static final String MACH_TYPE = "MACH_TYPE";
	private DataCollection ppDeveDC;
	private ListView ppDevcList;
	private List<HashMap<String, Object>> ppDevcListItem;
	private SimpleAdapter ppDevcAdapter;
	private static final String MTRL_TYPE = "MTRL_TYPE";
	private static final String DEVC_NUMBER = "DEVC_NUMBER";
	private DataCollection cppDC;
	private ListView cppListView;
	private List<HashMap<String, Object>> cppListItem;
	private SimpleAdapter cppAdapter;

	private LinearLayout selectMachLine;
	private List<HashMap<String, Object>> selectedMachListItem;
	private SimpleAdapter selectedMachAdapter;

	private Button addPPButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_start);
		mFormView = findViewById(R.id.start_form);
		mStatusView = findViewById(R.id.start_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		machListDC = new DataCollection();
		machListView = (ListView) findViewById(R.id.lot_start_assigned_mach_list);
		machListItem = new ArrayList<HashMap<String, Object>>();
		ppDeveDC = new DataCollection();
		ppDevcList = (ListView) findViewById(R.id.lot_start_pp_device_list);
		machListItem = new ArrayList<HashMap<String, Object>>();
		cppDC = new DataCollection();
		cppListView = (ListView) findViewById(R.id.lot_start_currently_loaded);
		machListItem = new ArrayList<HashMap<String, Object>>();
		cppListItem = new ArrayList<HashMap<String, Object>>();

		selectMachLine = (LinearLayout) findViewById(R.id.lot_start_ll2);
		selectMachLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(LotStartActivity.this, LotStartSelectMachActivity.class);
				startActivityForResult(i, 0);
			}
		});
		selectedMachListItem = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> m = new HashMap<String, Object>();
		m.put(MACH_NAME, getResources().getString(R.string.mach_name));
		m.put(MACH_MODEL, getResources().getString(R.string.mach_model));
		m.put(MACH_TYPE, getResources().getString(R.string.mach_type));
		selectedMachListItem.add(m);
		selectedMachAdapter = new SimpleAdapter(LotStartActivity.this, selectedMachListItem, R.layout.lot_start_mach_list_item, new String[] { MACH_NAME, MACH_MODEL,
				MACH_TYPE }, new int[] { R.id.item1, R.id.item2, R.id.item3 });
		machListView.setAdapter(selectedMachAdapter);
		machListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if (position != 0) {
					selectedMachListItem.remove(position);
					global.getSelectedMachList().remove(position);
					selectedMachAdapter.notifyDataSetChanged();

					if (null == qTask) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("changeCurrLoaded");
					}
				}
				return false;
			}
		});

		HashMap<String, Object> mTitle = new HashMap<String, Object>();
		mTitle.put("MTRL_TYPE", getString(R.string.material_type));
		mTitle.put("CONTAINER_ID", getString(R.string.container_id));
		mTitle.put("PP_LOT_NUMBER", getString(R.string.pp_lot_number));
		mTitle.put("DEVC_NUMBER", getString(R.string.device_number));
		mTitle.put("FLOOR_LIFE_EXP_DATE", getString(R.string.floor_life_expire_date));
		cppListItem.add(mTitle);
		cppAdapter = new SimpleAdapter(LotStartActivity.this, cppListItem, R.layout.lot_start_pp_list_item, new String[] { "MTRL_TYPE", "CONTAINER_ID", "PP_LOT_NUMBER",
				"DEVC_NUMBER", "FLOOR_LIFE_EXP_DATE" }, new int[] { R.id.lot_start_pp_item_matl_type, R.id.lot_start_pp_item_container_id, R.id.lot_start_pp_item_pp_lot,
				R.id.lot_start_pp_item_devc, R.id.lot_start_pp_item_floor_life_exp_date });
		cppListView.setAdapter(cppAdapter);

		addPPButton = (Button) findViewById(R.id.lot_start_add_pp);
		addPPButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(LotStartActivity.this, PiecePartLoadActivity.class);
				startActivity(i);
			}
		});

		if (null != global.getAoLot() && null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("loadPage");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.lot_start, menu);
		return true;
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";
		
		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("loadPage")) {
					loadPage();
				} else if (cmdName.equals("changeCurrLoaded")) {
					changeCurrLoaded();
				}
			} catch (BaseException e) {
				return e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(BaseException e) {
			qTask = null;
			showProgress(false);
			if (null == e) {
				if (cmdName.equals("loadPage")) {
					loadPageAfter();
				} else if (cmdName.equals("changeCurrLoaded")) {
					changeCurrLoadedAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotStartActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotStartActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void loadPage() throws BaseException {
			// machine
			String API = "getCurrentMachineContext(attributes='machName, machId, machModel, machType',lotNumber='" + global.getAoLot().getAlotNumber() + "')";
			machListDC = apiExecutorQuery.query("LotStart", "loadPage", API);
			// Piece part device
			API = "getLotAttributes(attributes='pkgKit,department',lotNumber='" + global.getAoLot().getAlotNumber() + "')";
			DataCollection pkgKitDC = apiExecutorQuery.query("LotStart", "loadPage", API);
			String pkgKit;
			if (pkgKitDC.size() > 0) {
				pkgKit = pkgKitDC.get(0).get(0);
			} else {
				throw new RfidException("Lot Number " + global.getAoLot().getAlotNumber() + " is not assigned to any package kit.", "LotStart", "loadPage", API);
			}
			API = "getBomAttributes(attributes='devcNumber, matlType',pkgKit='" + pkgKit + "', stepName='" + global.getAoLot().getCurrentStep().getStepName() + "')";
			DataCollection bomAttr = apiExecutorQuery.query("LotStart", "loadPage", API);
			if (bomAttr.size() > 0) {
				for (int i = 0; i < bomAttr.size(); i++) {
					ArrayList<String> row = new ArrayList<String>();
					String matlType = bomAttr.get(0).get(1);
					String devcNum = bomAttr.get(0).get(0);
					row.add(matlType);
					row.add(devcNum);
					ppDeveDC.add(row);
				}
			}

			// Currunt Loaded on Machine
			// MTRL_TYPE CONTAINER_ID PP_LOT_NUMBER DEVC_NUMBER
			// FLOOR_LIFE_EXP_DATE
			// load by default machines
		}

		private void loadPageAfter() {
			// MachineName MachineId MachineModel MachineType
			for (ArrayList<String> mach : machListDC) {
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(MACH_NAME, mach.get(0));
				m.put(MACH_MODEL, mach.get(2));
				m.put(MACH_TYPE, mach.get(3));
				machListItem.add(m);
			}
			machAdapter = new SimpleAdapter(LotStartActivity.this, machListItem, R.layout.lot_start_mach_list_item, new String[] { MACH_NAME, MACH_MODEL, MACH_TYPE },
					new int[] { R.id.item1, R.id.item2, R.id.item3 });
			machListView.setAdapter(machAdapter);

			// MtrlType DevcNumber
			for (ArrayList<String> ppDevc : ppDeveDC) {
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(MTRL_TYPE, ppDevc.get(0));
				m.put(DEVC_NUMBER, ppDevc.get(1));
				ppDevcListItem.add(m);
			}
			ppDevcAdapter = new SimpleAdapter(LotStartActivity.this, ppDevcListItem, R.layout.lot_start_pp_devc_list_item, new String[] { MTRL_TYPE, DEVC_NUMBER },
					new int[] { R.id.item4, R.id.item5 });
			ppDevcList.setAdapter(ppDevcAdapter);
		}

		private void changeCurrLoaded() throws BaseException {
			cppDC = new DataCollection();
			for (int i = 1; i < selectedMachListItem.size(); i++) {
				HashMap<String, Object> m = selectedMachListItem.get(i);
				// ESOCommonTrans.getMachIdByMachName(responseMsg,tempMachName,transUserId);
				String API = "getCurrentPPLoadedOnMachine(attributes='containerId,ppLotNumber,devcNumber,floorLifeExpiryDate,mtrlType', machId = '" + m.get(MACH_NAME)
						+ "')";
				DataCollection dc = apiExecutorQuery.query("LotStart", "loadPage", API);
				cppDC.addAll(dc);
			}
		}

		private void changeCurrLoadedAfter() {
			HashMap<String, Object> mTitle = new HashMap<String, Object>();
			mTitle.put("MTRL_TYPE", getString(R.string.material_type));
			mTitle.put("CONTAINER_ID", getString(R.string.container_id));
			mTitle.put("PP_LOT_NUMBER", getString(R.string.pp_lot_number));
			mTitle.put("DEVC_NUMBER", getString(R.string.device_number));
			mTitle.put("FLOOR_LIFE_EXP_DATE", getString(R.string.floor_life_expire_date));
			cppListItem.add(mTitle);
			for (ArrayList<String> cpp : cppDC) {
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put("MTRL_TYPE", cpp.get(0));
				m.put("CONTAINER_ID", cpp.get(1));
				m.put("PP_LOT_NUMBER", cpp.get(2));
				m.put("DEVC_NUMBER", cpp.get(3));
				m.put("FLOOR_LIFE_EXP_DATE", cpp.get(4));
				cppListItem.add(m);
			}
			cppAdapter.notifyDataSetChanged();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null) {
			switch (resultCode) {
			case 0:
				// after select machine
				if (null != global.getSelectedMachList()) {
					selectedMachListItem = new ArrayList<HashMap<String, Object>>();
					HashMap<String, Object> mTitle = new HashMap<String, Object>();
					mTitle.put(MACH_NAME, getResources().getString(R.string.mach_name));
					mTitle.put(MACH_MODEL, getResources().getString(R.string.mach_model));
					mTitle.put(MACH_TYPE, getResources().getString(R.string.mach_type));
					selectedMachListItem.add(mTitle);
					for (Mach mach : global.getSelectedMachList()) {
						HashMap<String, Object> m = new HashMap<String, Object>();
						m.put(MACH_NAME, mach.getMachID());
						m.put(MACH_MODEL, mach.getMachModel());
						m.put(MACH_TYPE, mach.getMachType());
						selectedMachListItem.add(m);
					}
					selectedMachAdapter.notifyDataSetChanged();
					global.setSelectedMachList(null);

					if (null == qTask) {
						qTask = new QueryTask();
						qTask.execute("changeCurrLoaded");
					}
				}
				break;
			}
		}
	}
	
	@Override
	protected void onPause() {
		if (null != qTask) {
			qTask.cancel(true);
		}
		super.onPause();
	}
}
