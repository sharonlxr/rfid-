package com.fsl.cimei.rfid;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.freescale.api.DateFormatter;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class PiecePartLoadActivity extends BaseActivity {

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

	private TextView ppMatlTypeSelect;
	private LinearLayout ppMatlTypeLine;
	private AlertDialog ppMatlTypeAlertDialog = null;
	private String[] ppMatlTypeArray;
	private String ppMatlType = "";
	private String oldPpMatlType = "";

	private TextView ppDevcSelect;
	private LinearLayout ppDevcLine;
	private AlertDialog ppDevcAlertDialog = null;
	private String[] ppDevcArray;
	private String ppDevc = "";

	private LinearLayout lotsLayout;
	private CheckBox lotsSelectAllCB;
	private List<CheckBox> lotsCBList;
	private DataCollection lotsDC;
	private CheckBoxChangeListener listener;

	private LinearLayout ppSelectTriggerLine;
	private List<HashMap<String, Object>> ppOptionsListItem;
	private AlertDialog ppOptionsAlertDialog = null;
	private ListView ppSelectedListView;
	private List<HashMap<String, Object>> ppSelectedListItem;
	private SimpleAdapter ppSelectedListItemAdapter;

	private Button doneButton;
	private Button cancelButton;

	private ArrayList<String> selectedPpLotNumberList = new ArrayList<String>();
	private ArrayList<String> selectedContainerIdList = new ArrayList<String>();
	private ArrayList<String> excludeValidatelotList = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_piece_part_load);
		mFormView = findViewById(R.id.pp_load_form);
		mStatusView = findViewById(R.id.pp_load_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		stepNameSelect = (TextView) findViewById(R.id.pp_load_step_name);
		stepNameLine = (LinearLayout) findViewById(R.id.pp_load_ll1);
		machNameSelect = (TextView) findViewById(R.id.pp_load_mach_name);
		machNameLine = (LinearLayout) findViewById(R.id.pp_load_ll2);
		ppMatlTypeSelect = (TextView) findViewById(R.id.pp_load_pp_matl_type);
		ppDevcSelect = (TextView) findViewById(R.id.pp_load_pp_devc);
		lotsSelectAllCB = (CheckBox) findViewById(R.id.pp_load_lots_cb);
		lotsLayout = (LinearLayout) findViewById(R.id.pp_load_lots_layout);
		doneButton = (Button) findViewById(R.id.pp_load_done);
		cancelButton = (Button) findViewById(R.id.pp_load_cancel);
		ppSelectedListView = (ListView) findViewById(R.id.pp_load_pp_list);
		ppMatlTypeLine = (LinearLayout) findViewById(R.id.pp_load_ll3);
		ppDevcLine = (LinearLayout) findViewById(R.id.pp_load_ll4);
		ppSelectTriggerLine = (LinearLayout) findViewById(R.id.pp_load_title4);
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

		ppMatlTypeLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null == qTask && null == ppMatlTypeAlertDialog) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("loadPpMatlType");
				} else {
					ppMatlTypeAlertDialog.show();
				}
			}
		});
		ppDevcLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null == qTask && !CommonUtility.isEmpty(ppMatlType) && (CommonUtility.isEmpty(oldPpMatlType) || !ppMatlType.equals(oldPpMatlType))) {
					oldPpMatlType = ppMatlType;
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("loadPpDevc");
				} else {
					if (null != ppDevcAlertDialog) {
						ppDevcAlertDialog.show();
					}
				}
			}
		});

		listener = new CheckBoxChangeListener();
		lotsDC = new DataCollection();
		lotsCBList = new ArrayList<CheckBox>();
		lotsSelectAllCB.setChecked(false);
		lotsSelectAllCB.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				for (CheckBox cb : lotsCBList) {
					cb.setChecked(lotsSelectAllCB.isChecked());
				}
			}
		});

		ppSelectTriggerLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null == qTask && !CommonUtility.isEmpty(ppMatlType) && !CommonUtility.isEmpty(ppDevc)) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("getPpByMatlTypeAndDevice");
				}
			}
		});

		ppOptionsListItem = new ArrayList<HashMap<String, Object>>();
		ppSelectedListItem = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("containerId", getResources().getString(R.string.container_id));
		map.put("ppLotNumber", getResources().getString(R.string.pp_lot_number));
		map.put("devcNumber", getResources().getString(R.string.device_number));
		map.put("containerRecvDate", getResources().getString(R.string.container_recv_date));
		map.put("containerOpenDate", getResources().getString(R.string.container_open_date));
		map.put("floorLifeExpiryDate", getResources().getString(R.string.floor_life_expire_date));
		map.put("currentQty", getResources().getString(R.string.current_qty));
		ppSelectedListItem.add(map);
		ppSelectedListItemAdapter = new SimpleAdapter(PiecePartLoadActivity.this, ppSelectedListItem, R.layout.pp_list_item, new String[] { "containerId", "ppLotNumber",
				"devcNumber", "containerRecvDate", "containerOpenDate", "floorLifeExpiryDate", "currentQty" }, new int[] { R.id.pp_item_container_id,
				R.id.pp_item_pp_lot, R.id.pp_item_devc, R.id.pp_item_container_recv_date, R.id.pp_item_container_open_date, R.id.pp_item_floor_life_exp_date,
				R.id.pp_item_current_qty });
		ppSelectedListView.setAdapter(ppSelectedListItemAdapter);
		ppSelectedListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if (position != 0) {
					ppSelectedListItem.remove(position);
					ppSelectedListItemAdapter.notifyDataSetChanged();
				}
				return false;
			}
		});

		doneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				for (int i = 0; i < lotsCBList.size(); i++) {
					CheckBox cb = lotsCBList.get(i);
					if (cb.isChecked()) {
						excludeValidatelotList.add(lotsDC.get(i).get(0));
					}
				}
				if (qTask == null) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("loadPPToMach");
				}
			}
		});

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
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
				} else if (cmdName.equals("loadPpMatlType")) {
					loadPpMatlType();
				} else if (cmdName.equals("loadPpDevc")) {
					loadPpDevc();
				} else if (cmdName.equals("loadLotsByMach")) {
					loadLotsByMach();
				} else if (cmdName.equals("getPpByMatlTypeAndDevice")) {
					getPpByMatlTypeAndDevice();
				} else if (cmdName.equals("loadPPToMach")) {
					loadPPToMach();
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
				} else if (cmdName.equals("loadPpMatlType")) {
					loadPpMatlTypeAfter();
				} else if (cmdName.equals("loadPpDevc")) {
					loadPpDevcAfter();
				} else if (cmdName.equals("loadLotsByMach")) {
					loadLotsByMachAfter();
				} else if (cmdName.equals("getPpByMatlTypeAndDevice")) {
					getPpByMatlTypeAndDeviceAfter();
				} else if (cmdName.equals("loadPPToMach")) {
					loadPPToMachAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(PiecePartLoadActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(PiecePartLoadActivity.this, e.getErrorMsg());
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
			DataCollection dataCollection = apiExecutorQuery.query("PiecePartLoad", "loadStepName", API);
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
			stepNameAlertDialog = new AlertDialog.Builder(PiecePartLoadActivity.this).setTitle("选择Step").setItems(stepNameArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					stepName = "" + stepNameArray[which];
					stepNameSelect.setText(stepName);
				}
			}).setNegativeButton(getResources().getString(R.string.button_cancel), null).create();
			stepNameAlertDialog.show();
		}

		private void loadMachName() throws BaseException {
			String API;
			// if (CommonUtility.isEmpty(global.getUserOperationList())) {
			// API = "getMESParmValues(attributes='parmValue',parmOwnerType='OPER',parmName='otherValidOper',parmOwner='" + global.getUser().getOperation() + "')";
			// DataCollection department = apiExecutor.query(API);
			// if (!CommonUtility.isEmpty(apiExecutor.getMessage())) {
			// errorMsg = apiExecutor.getMessage();
			// } else {
			// List<String> departmentResult = new ArrayList<String>();
			// for (int i = 0; i < department.size(); i++) {
			// departmentResult.add("'" + department.get(i).get(0).replace(",", "','") + "'");
			// }
			// departmentResult.add("'" + global.getUser().getOperation() + "'");
			// global.setUserOperationList(departmentResult.toString());
			// }
			// }

			CommonTrans commonTrans = new CommonTrans();
			commonTrans.checkUserInfo(apiExecutorQuery, global);
			API = "getMachineAttributes(attributes='machId', operationList = " + global.getUserOperationList() + ",activeMachinesOnly='Y', stepName='" + stepName + "')";
			DataCollection dataCollection = apiExecutorQuery.query("PiecePartLoad", "loadMachName", API);
			if (dataCollection.size() > 0) {
				machNameArray = new String[dataCollection.size()];
				for (int i = 0; i < dataCollection.size(); i++) {
					machNameArray[i] = dataCollection.get(i).get(0);
				}
			}
		}

		private void loadMachNameAfter() {
			machNameAlertDialog = new AlertDialog.Builder(PiecePartLoadActivity.this).setTitle("选择机台").setItems(machNameArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					machName = "" + machNameArray[which];
					machNameSelect.setText(machName);

					if (null == qTask && !CommonUtility.isEmpty(machName) && (CommonUtility.isEmpty(oldMachName) || !machName.equals(oldMachName))) {
						oldMachName = machName;
						lotsLayout.removeAllViews();
						lotsCBList.clear();
						lotsSelectAllCB.setChecked(false);
						qTask = new QueryTask();
						qTask.execute("loadLotsByMach");
					}
				}
			}).setNegativeButton(getResources().getString(R.string.button_cancel), null).create();
			machNameAlertDialog.show();
		}

		private void loadPpMatlType() throws BaseException {
			String API = "getPPMtrlTypeMachTypeCombs(attributes='ppMtrlType',ppMtrlType='%',machType='%')";
			DataCollection dataCollection = apiExecutorQuery.query("PiecePartLoad", "loadPpMatlType", API);
			if (dataCollection.size() > 0) {
				ppMatlTypeArray = new String[dataCollection.size()];
				for (int i = 0; i < dataCollection.size(); i++) {
					ppMatlTypeArray[i] = dataCollection.get(i).get(0);
				}
			}
		}

		private void loadPpMatlTypeAfter() {
			ppMatlTypeAlertDialog = new AlertDialog.Builder(PiecePartLoadActivity.this).setTitle("选择 Piece Part material type")
					.setItems(ppMatlTypeArray, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							ppMatlType = "" + ppMatlTypeArray[which];
							ppMatlTypeSelect.setText(ppMatlType);
						}
					}).setNegativeButton(getResources().getString(R.string.button_cancel), null).create();
			ppMatlTypeAlertDialog.show();
		}

		private void loadPpDevc() throws BaseException {
			String API = "getDeviceAttributes(attributes = 'devcNumber', matlType = '" + ppMatlType + "')";
			DataCollection dataCollection = apiExecutorQuery.query("PiecePartLoad", "loadPpDevc", API);
			if (dataCollection.size() > 0) {
				ppDevcArray = new String[dataCollection.size()];
				for (int i = 0; i < dataCollection.size(); i++) {
					ppDevcArray[i] = dataCollection.get(i).get(0);
				}
			}
		}

		private void loadPpDevcAfter() {
			ppDevcAlertDialog = new AlertDialog.Builder(PiecePartLoadActivity.this).setTitle("选择 Piece Part Device number")
					.setItems(ppDevcArray, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							ppDevc = "" + ppDevcArray[which];
							ppDevcSelect.setText(ppDevc);
						}
					}).setNegativeButton(getResources().getString(R.string.button_cancel), null).create();
			ppDevcAlertDialog.show();
		}

		private void loadLotsByMach() throws BaseException {
			String API = "getCurrentMachineContext(attributes='lotNumber,devcNumber',machId = '" + machName + "')";
			lotsDC = apiExecutorQuery.query("PiecePartLoad", "loadLotsByMach", API);
		}

		@SuppressLint("InflateParams")
		private void loadLotsByMachAfter() {
			for (ArrayList<String> lot : lotsDC) {
				LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.pp_load_lots_list_item, null);
				CheckBox cb = (CheckBox) l.findViewById(R.id.pp_load_lot_item_cb);
				cb.setOnClickListener(listener);
				lotsCBList.add(cb);
				TextView lotNumberTV = (TextView) l.findViewById(R.id.pp_load_lot_item_lot_number);
				lotNumberTV.setText(lot.get(0));
				TextView devcNumberTV = (TextView) l.findViewById(R.id.pp_load_lot_item_devc_number);
				devcNumberTV.setText(lot.get(1));
				lotsLayout.addView(l);
			}
		}

		private void getPpByMatlTypeAndDevice() throws BaseException {
			boolean onlyActivePPLot = false;
			String API = "getMESParmValues(attributes='parmValue',parmOwnerType='APPL',parmOwner='MaterialMgr',parmName='enableOnlyActivePPLot')";
			DataCollection dc = apiExecutorQuery.query("PiecePartLoad", "getPpByMatlTypeAndDevice", API);
			if (!CommonUtility.isEmpty(dc) && dc.size() > 0) {
				String val = dc.get(0).get(0);
				if (val.equals("1")) {
					onlyActivePPLot = true;
				}
			}
			API = "getSysdate()";
			dc = apiExecutorQuery.query("PiecePartLoad", "getPpByMatlTypeAndDevice", API);
			String sysDate = "";
			if (!CommonUtility.isEmpty(dc) && dc.size() > 0) {
				sysDate = dc.get(0).get(0);
			}
			API = "getPPContainerAttributes(attributes='containerId,ppLotNumber,devcNumber,containerRecvDate,containerOpenTime,floorLifeExpiryDate,currentQty',mtrlType = '"
					+ ppMatlType + "',devcNumber='" + ppDevc + "',status='AC')";
			dc = apiExecutorQuery.query("PiecePartLoad", "getPpByMatlTypeAndDevice", API);
			for (int i = 0; i < dc.size(); i++) {
				boolean add = false;
				if (onlyActivePPLot) {
					String floorLifeExpiryDate = dc.get(i).get(5);
					try {
						Date date1 = DateFormatter.getSimpleDateToDate(floorLifeExpiryDate);
						Date date2 = DateFormatter.getSimpleDateToDate(sysDate);
						if (date1.compareTo(date2) > 0) {
							add = true;
						}
					} catch (ParseException e) {
						throw new RfidException(e.toString(), "PiecePartLoad", "getPpByMatlTypeAndDevice", API);
					}
				} else {
					add = true;
				}
				if (add) {
					String containerId = dc.get(i).get(0);
					String ppLotNumber = dc.get(i).get(1);
					String devcNumber = dc.get(i).get(2);
					String containerRecvDate = dc.get(i).get(3);
					String containerOpenDate = dc.get(i).get(4);
					String floorLifeExpiryDate = dc.get(i).get(5);
					String currentQty = dc.get(i).get(6);
					HashMap<String, Object> map = new HashMap<String, Object>();
					map.put("containerId", containerId);
					map.put("ppLotNumber", ppLotNumber);
					map.put("devcNumber", devcNumber);
					map.put("containerRecvDate", containerRecvDate);
					map.put("containerOpenDate", containerOpenDate);
					map.put("floorLifeExpiryDate", floorLifeExpiryDate);
					map.put("currentQty", currentQty);
					ppOptionsListItem.add(map);
				}
			}
		}

		private void getPpByMatlTypeAndDeviceAfter() {
			SimpleAdapter detailListItemAdapter = new SimpleAdapter(PiecePartLoadActivity.this, ppOptionsListItem, R.layout.pp_list_item, new String[] { "containerId",
					"ppLotNumber", "devcNumber", "containerRecvDate", "containerOpenDate", "floorLifeExpiryDate", "currentQty" }, new int[] { R.id.pp_item_container_id,
					R.id.pp_item_pp_lot, R.id.pp_item_devc, R.id.pp_item_container_recv_date, R.id.pp_item_container_open_date, R.id.pp_item_floor_life_exp_date,
					R.id.pp_item_current_qty });
			final ListView detailView = new ListView(PiecePartLoadActivity.this);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			lp.setMargins(6, 6, 6, 6);
			detailView.setLayoutParams(lp);
			detailView.setPadding(6, 6, 6, 6);
			detailView.setAdapter(detailListItemAdapter);
			detailView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					ppSelectedListItem.add(ppOptionsListItem.get(position));
					ppSelectedListItemAdapter.notifyDataSetChanged();
				}
			});
			ppOptionsAlertDialog = new AlertDialog.Builder(PiecePartLoadActivity.this).setTitle("选择 Container/Piece Part").setView(detailView)
					.setNegativeButton(getResources().getString(R.string.button_cancel), null).create();
			ppOptionsAlertDialog.show();
		}

		private void loadPPToMach() throws BaseException {
			for (int i = 1; i < ppSelectedListItem.size(); i++) {
				HashMap<String, Object> map = ppSelectedListItem.get(i);
				selectedContainerIdList.add((String) map.get("containerId"));
				selectedPpLotNumberList.add((String) map.get("ppLotNumber"));
			}
			long listCount = ppSelectedListItem.size() - 1;
			String operationList = global.getUserOperationList();
			// String machType = "";
			String machDept = "";
			String API = "getMachineAttributes(attributes='machName,machId,machType,department', operationList =[" + operationList
					+ "], activeMachinesOnly='Y', machId = '" + machName + "')";
			DataCollection dc = apiExecutorQuery.query("PiecePartLoad", "loadPPToMach", API);
			if (dc.size() > 0) {
				// machType = dc.get(0).get(2);
				machDept = dc.get(0).get(3);
			}
			boolean temp1 = checkPPContainerMaxNum();
			if (temp1) {
				Object[] temp2 = checkLimitPPContainerMaxNum(machName, listCount);
				if (temp1 && (Boolean) temp2[0]) {
					throw new RfidException((String) temp2[1], "PiecePartLoad", "loadPPToMach", API);
				}
			}
			Object[] temp3 = checkMaxLoadedPiecePartLot(machName, machDept, selectedPpLotNumberList);
			if ((Boolean) temp3[0]) {
				throw new RfidException((String) temp3[1], "PiecePartLoad", "loadPPToMach", API);
			}
			String excludeAolotList = "";
			for (String aolot : excludeValidatelotList) {
				if (!aolot.trim().equals("")) {
					excludeAolotList = excludeAolotList.equals("") ? "'" + aolot + "'" : excludeAolotList + ",'" + aolot + "'";
				}
			}
			for (int i = 0; i < selectedContainerIdList.size(); i++) {
				API = "loadPiecepartContainerOnMachine(transUserId='" + global.getUser().getUserID() + "',containerId='" + selectedContainerIdList.get(i) + "',machId='"
						+ machName + "'";
				if (!excludeAolotList.trim().equals("")) {
					API = API + ", excludeValidatelotList=[" + excludeAolotList + "]";
				}
				API = API + ")";
				apiExecutorUpdate.transact("PiecePartLoad", "loadPPToMach", API);
			}
		}

		private boolean checkPPContainerMaxNum() throws BaseException {
			boolean require = false;
			String API = "getMESParmValues(attributes ='parmValue',parmOwnerType='APPL',parmOwner='MaterialMgr',parmName='checkPPContainerMaxNum')";
			DataCollection dc = apiExecutorQuery.query("PiecePartLoad", "checkPPContainerMaxNum", API);
			if (dc.size() > 0) {
				String val = dc.get(0).get(0);
				if (val.equals("1")) {
					require = true;
				}
			}
			return require;
		}

		private Object[] checkLimitPPContainerMaxNum(String machId, long listCount) throws BaseException {
			boolean flag = false;
			String errorMsg = "";
			int existedCount = 0;
			String API = "getCurrentPPLoadedOnMachine(attributes = 'devcNumber, containerId', machId = '" + machId + "')";
			DataCollection dc = apiExecutorQuery.query("PiecePartLoad", "checkLimitPPContainerMaxNum", API);
			for (int i = 0; i < dc.size(); i++) {
				String containerId = dc.get(i).get(1);
				if (!CommonUtility.isEmpty(containerId) && !containerId.equals("None")) {
					existedCount += 1;
				}
			}
			API = "getMESParmValues(attributes ='parmValue',parmOwnerType='MACH',parmOwner='" + machId + "',parmName='validatePPContainerMaxNum')";
			dc = apiExecutorQuery.query("PiecePartLoad", "checkLimitPPContainerMaxNum", API);
			if (dc.size() > 0) {
				String val = dc.get(0).get(0);
				long longVal = Long.valueOf(val);
				if (longVal != 0 && longVal < listCount + existedCount - 1) {
					if (existedCount > 0) {
						errorMsg = "Can not input container more than " + val + ", Current has load " + existedCount + " container";
					} else {
						errorMsg = "Can not input container more than " + val;
					}
					flag = true;
				} else if (longVal != 0 && longVal > listCount + existedCount - 1) {
					flag = false;
				} else {
					errorMsg = "PPContainer list number check equal to 0";
					flag = true;
				}
			} else {
				flag = false;
			}
			return new Object[] { flag, errorMsg };
		}

		private Object[] checkMaxLoadedPiecePartLot(String machId, String machDept, ArrayList<String> selectedPpLotNumberList) throws BaseException {
			String errorMsg = "";
			boolean flag = false;
			String API = "getMESParmValues(attributes ='parmValue',parmOwnerType='DEPT',parmOwner='" + machDept + "', parmName='maxLoadedPiecePartLot')";
			DataCollection dc = apiExecutorQuery.query("PiecePartLoad", "checkMaxLoadedPiecePartLot", API);
			if (dc.size() > 0) {
				String maxLoadedPiecePartLot = dc.get(0).get(0);
				long maxLoadedPiecePartLotLongVal = Long.valueOf(maxLoadedPiecePartLot);
				API = "getCurrentPPLoadedOnMachine(attributes = 'ppLotNumber', machId = '" + machId + "')";
				dc = apiExecutorQuery.query("PiecePartLoad", "checkMaxLoadedPiecePartLot", API);
				int existedCount = 0;
				String listLot = "";
				if (dc.size() > 0) {
					for (int i = 0; i < dc.size(); i++) {
						String tempPpLotNumber = dc.get(i).get(0);
						if (!tempPpLotNumber.equals("None") && !CommonUtility.isEmpty(tempPpLotNumber)) {
							existedCount += 1;
							listLot = listLot.equals("") ? tempPpLotNumber : listLot + "," + tempPpLotNumber;
						}
					}
				}
				int selectedCount = 0;
				boolean found = false;
				for (int i = 0; i < selectedPpLotNumberList.size(); i++) {
					if (!CommonUtility.isEmpty(listLot)) {
						String[] ppLotNumbers = listLot.split(",");
						for (int j = 0; j < ppLotNumbers.length; j++) {
							if (ppLotNumbers[j].equals(selectedPpLotNumberList.get(i))) {
								found = true;
								break;
							} else {
								found = false;
							}
						}
					}
					if (!found) {
						listLot = listLot.equals("") ? selectedPpLotNumberList.get(i) : listLot + "," + selectedPpLotNumberList.get(i);
						selectedCount += 1;
					}
				}
				if (maxLoadedPiecePartLotLongVal > 0) {
					if (maxLoadedPiecePartLotLongVal < selectedCount + existedCount) {
						if (existedCount > 0) {
							errorMsg = "Mach Dept " + machDept + " is configured for maxLoadedPiecePartLot. You can not select more than " + maxLoadedPiecePartLot
									+ " piece part lot number, Current has loaded " + existedCount;
						} else {
							errorMsg = "Mach Dept " + machDept + " is configured for maxLoadedPiecePartLot. You can not select more than " + maxLoadedPiecePartLot
									+ " piece part lot number";
						}
						flag = true;
					}
				}
			}
			return new Object[] { flag, errorMsg };
		}

		private void loadPPToMachAfter() {
			showError(PiecePartLoadActivity.this, "添加成功");
		}
	}

	private class CheckBoxChangeListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			CheckBox c = (CheckBox) v;
			if (!c.isChecked()) {
				lotsSelectAllCB.setChecked(false);
			} else {
				boolean allChecked = true;
				for (CheckBox cb : lotsCBList) {
					if (!cb.isChecked()) {
						allChecked = false;
						break;
					}
				}
				lotsSelectAllCB.setChecked(allChecked);
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
