package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;

public class PiecePartUnloadActivity extends BaseActivity {

	private QueryTask qTask;

	private TextView stepNameSelect;
	private LinearLayout stepNameLine;
	private AlertDialog stepNameAlertDialog = null;
	private String[] stepNameArray;
	private String stepName = "";
	private String oldStepName = "";

	private TextView machNameSelect;
	private LinearLayout machNameLine;
	private AlertDialog machNameAlertDialog = null;
	private String[] machNameArray;
	private String machName = "";
	private String oldMachName = "";

	private LinearLayout currentlyLoadedLinearLayout;
	private List<HashMap<String, Object>> currentlyLoadedListItem;
	private DataCollection currentlyLoadedDC;
	private SimpleAdapter currentlyLoadedListViewAdapter;

	private LinearLayout ppToUnloadListView;
	private List<HashMap<String, Object>> ppToUnloadListItem;
	private SimpleAdapter ppToUnloadListViewAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_piece_part_unload);
		mFormView = findViewById(R.id.pp_unload_form);
		mStatusView = findViewById(R.id.pp_unload_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		stepNameSelect = (TextView) findViewById(R.id.pp_unload_step_name);
		stepNameLine = (LinearLayout) findViewById(R.id.pp_unload_ll1);
		machNameSelect = (TextView) findViewById(R.id.pp_unload_mach_name);
		machNameLine = (LinearLayout) findViewById(R.id.pp_unload_ll2);

		// currentlyLoadedLinearLayout = (LinearLayout) findViewById(R.id.pp_unload_currently_loaded);
		currentlyLoadedListItem = new ArrayList<HashMap<String, Object>>();
		currentlyLoadedDC = new DataCollection();

		// ppToUnloadListView = (LinearLayout) findViewById(R.id.pp_unload_to_unload);
		ppToUnloadListItem = new ArrayList<HashMap<String, Object>>();

		stepNameLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null == qTask && null == stepNameAlertDialog) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("loadStepName");
				} else {
					stepNameAlertDialog.show();
				}
			}
		});
		machNameLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null == qTask && !CommonUtility.isEmpty(stepName) && (CommonUtility.isEmpty(oldStepName) || !stepName.equals(oldStepName))) {
					oldStepName = stepName;
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("loadMachName");
				} else {
					if (null != machNameAlertDialog) {
						machNameAlertDialog.show();
					}
				}
			}
		});

		// HashMap<String, Object> map = new HashMap<String, Object>();
		// currentlyLoadedListItem.add(map);
		// ppToUnloadListItem.add(map);
		// currentlyLoadedListViewAdapter = new SimpleAdapter(PiecePartUnloadActivity.this, currentlyLoadedListItem, R.layout.pp_list_item_2, new String[] { "containerId", "floorLifeExpiryDate",
		// "ppLotNumber", "shelfLifeExpiryDate", "devcNumber", "bakeLifeExpiryDate", "status" }, new int[] { R.id.pp_item_2_container_id, R.id.pp_item_2_floor_life_exp_date,
		// R.id.pp_item_2_pp_lot, R.id.pp_item_2_shelf_life_exp_date, R.id.pp_item_2_devc, R.id.pp_item_2_bake_life_exp_date, R.id.pp_item_2_status });
		// ppToUnloadListViewAdapter = new SimpleAdapter(PiecePartUnloadActivity.this, ppToUnloadListItem, R.layout.pp_list_item_2, new String[] { "containerId", "floorLifeExpiryDate", "ppLotNumber",
		// "shelfLifeExpiryDate", "devcNumber", "bakeLifeExpiryDate", "status" }, new int[] { R.id.pp_item_2_container_id, R.id.pp_item_2_floor_life_exp_date, R.id.pp_item_2_pp_lot,
		// R.id.pp_item_2_shelf_life_exp_date, R.id.pp_item_2_devc, R.id.pp_item_2_bake_life_exp_date, R.id.pp_item_2_status });
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";
		
		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("loadStepName")) {
					loadStepName();
				} else if (cmdName.equals("loadMachName")) {
					loadMachName();
				} else if (cmdName.equals("getCurrLoadedPpByMachId")) {
					getCurrLoadedPpByMachId();
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
				if (cmdName.equals("loadStepName")) {
					loadStepNameAfter();
				} else if (cmdName.equals("loadMachName")) {
					loadMachNameAfter();
				} else if (cmdName.equals("getCurrLoadedPpByMachId")) {
					getCurrLoadedPpByMachIdAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(PiecePartUnloadActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(PiecePartUnloadActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void loadStepName() throws BaseException {
			String API = "getStepAttributes(attributes='stepName,displaySequence,stepClass',activeStepOnly='Y')";
			DataCollection dataCollection = apiExecutorQuery.query("PiecePartUnload", "loadStepName", API);
			List<String> stepNameList = new ArrayList<String>();
			if (dataCollection.size() > 0) {
				for (int i = 0; i < dataCollection.size(); i++) {
					String stepName = dataCollection.get(i).get(0);
					String displaySequence = dataCollection.get(i).get(1);
					String stepClass = dataCollection.get(i).get(2);
					if (displaySequence == null || displaySequence.trim().equals("") || displaySequence.equals("None")) {
						displaySequence = "0";
					}
					if (Integer.valueOf(displaySequence) >= 0 && (stepClass.equals("ASSEMBLY") || stepClass.equals("") || stepClass.equals("None"))) {
						stepNameList.add(stepName);
					}
				}
			}
			stepNameArray = new String[stepNameList.size()];
			for (int i = 0; i < stepNameList.size(); i++) {
				stepNameArray[i] = stepNameList.get(i);
			}
		}

		private void loadStepNameAfter() {
			stepNameAlertDialog = new AlertDialog.Builder(PiecePartUnloadActivity.this).setTitle("选择Step").setItems(stepNameArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					stepName = "" + stepNameArray[which];
					stepNameSelect.setText(stepName);
				}
			}).setNegativeButton(getResources().getString(R.string.button_cancel), null).create();
			stepNameAlertDialog.show();
		}

		private void loadMachName() throws BaseException {
			CommonTrans commonTrans = new CommonTrans();
			commonTrans.checkUserInfo(apiExecutorQuery, global);
			String API = "getMachineAttributes(attributes='machId', operationList = " + global.getUserOperationList() + ",activeMachinesOnly='Y', stepName='" + stepName
					+ "')";
			DataCollection dataCollection = apiExecutorQuery.query("PiecePartUnload", "loadMachName", API);
			if (dataCollection.size() > 0) {
				machNameArray = new String[dataCollection.size()];
				for (int i = 0; i < dataCollection.size(); i++) {
					machNameArray[i] = dataCollection.get(i).get(0);
				}
			}
		}

		private void loadMachNameAfter() {
			machNameAlertDialog = new AlertDialog.Builder(PiecePartUnloadActivity.this).setTitle("选择机台").setItems(machNameArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					machName = "" + machNameArray[which];
					machNameSelect.setText(machName);

					if (null == qTask && !CommonUtility.isEmpty(machName) && (CommonUtility.isEmpty(oldMachName) || !machName.equals(oldMachName))) {
						oldMachName = machName;
						currentlyLoadedLinearLayout.removeAllViews();
						currentlyLoadedListItem.clear();
						qTask = new QueryTask();
						qTask.execute("getCurrLoadedPpByMachId");
					}
				}
			}).setNegativeButton(getResources().getString(R.string.button_cancel), null).create();
			machNameAlertDialog.show();
		}

		private void getCurrLoadedPpByMachId() throws BaseException {
			String API = "getMachineAttributes(attributes='machName,machId', operationList = " + global.getUserOperationList() + ",activeMachinesOnly='Y', machId = '"
					+ machName + "')";
			DataCollection dc = apiExecutorQuery.query("PiecePartUnload", "getCurrLoadedPpByMachId", API);
			if (dc.size() > 0) {
				API = "getCurrentPPLoadedOnMachine(attributes='containerId,floorLifeExpiryDate,ppLotNumber,shelfLifeExpiryDate,devcNumber,bakeLifeExpiryDate,status', machId = '"
						+ machName + "')";
				currentlyLoadedDC = apiExecutorQuery.query("PiecePartUnload", "getCurrLoadedPpByMachId", API);
			}
		}

		@SuppressLint("InflateParams")
		private void getCurrLoadedPpByMachIdAfter() {
			LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.pp_list_item_2, null);
			((TextView) l.findViewById(R.id.pp_item_2_container_id)).setText(getResources().getString(R.string.container_id));
			((TextView) l.findViewById(R.id.pp_item_2_floor_life_exp_date)).setText(getResources().getString(R.string.floor_life_expire_date));
			((TextView) l.findViewById(R.id.pp_item_2_pp_lot)).setText(getResources().getString(R.string.pp_lot_number));
			((TextView) l.findViewById(R.id.pp_item_2_shelf_life_exp_date)).setText(getResources().getString(R.string.shelf_life_expiry_date));
			((TextView) l.findViewById(R.id.pp_item_2_devc)).setText(getResources().getString(R.string.device_number));
			((TextView) l.findViewById(R.id.pp_item_2_bake_life_exp_date)).setText(getResources().getString(R.string.bake_life_expiry_date));
			((TextView) l.findViewById(R.id.pp_item_2_status)).setText(getResources().getString(R.string.status));
			currentlyLoadedLinearLayout.addView(l);
			for (int i = 0; i < currentlyLoadedDC.size(); i++) {
				String containerId = currentlyLoadedDC.get(i).get(0);
				String floorLifeExpiryDate = currentlyLoadedDC.get(i).get(1);
				String ppLotNumber = currentlyLoadedDC.get(i).get(2);
				String shelfLifeExpiryDate = currentlyLoadedDC.get(i).get(3);
				String devcNumber = currentlyLoadedDC.get(i).get(4);
				String bakeLifeExpiryDate = currentlyLoadedDC.get(i).get(5);
				String status = currentlyLoadedDC.get(i).get(6);
				LinearLayout ll = (LinearLayout) getLayoutInflater().inflate(R.layout.pp_list_item_2, null);
				((TextView) ll.findViewById(R.id.pp_item_2_container_id)).setText(containerId);
				((TextView) ll.findViewById(R.id.pp_item_2_floor_life_exp_date)).setText(floorLifeExpiryDate);
				((TextView) ll.findViewById(R.id.pp_item_2_pp_lot)).setText(ppLotNumber);
				((TextView) ll.findViewById(R.id.pp_item_2_shelf_life_exp_date)).setText(shelfLifeExpiryDate);
				((TextView) ll.findViewById(R.id.pp_item_2_devc)).setText(devcNumber);
				((TextView) ll.findViewById(R.id.pp_item_2_bake_life_exp_date)).setText(bakeLifeExpiryDate);
				((TextView) ll.findViewById(R.id.pp_item_2_status)).setText(status);
				currentlyLoadedLinearLayout.addView(ll);
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
