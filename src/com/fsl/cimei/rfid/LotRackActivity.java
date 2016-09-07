package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class LotRackActivity extends BaseActivity {

	private QueryTask qTask = null;
	private List<Map<String, String>> lotInfoListItem = new ArrayList<Map<String, String>>();
	private SimpleAdapter listItemAdapter;
	private ListView lotListView;
	private final String classname = "LotRackMgmt";
	private final String INDEX = "INDEX";
	private final String LOT = "LOT";
	private final String RACK = "RACK";
	private final String STEP = "STEP";
	private final String STEP_AGE = "STEP_AGE";
	private final String CARRIER_COUNT = "CARRIER_COUNT";
	private final String STRIP_NUMBER = "STRIP_NUMBER";
	private final String serverLink = "LotRackServlet";
	private String lot = "";
	private String rack = "";
	private String slot = "";
	private String bdNumber = "";
	private final int pageSize = 20;
	private int requestPage = 1;
	private int pageTotal = -1; // the total page count of query result
	private int total = -1; // the count of query result
	private int index = 1;
	private final String toLoadMoreStr = "点击加载更多";
	private List<Map<String, String>> carrierNameList = new ArrayList<Map<String, String>>();
	private final String CARRIER_NAME = "CARRIER_NAME";
	private final String LOCATION = "LOCATION";
	private Map<String, Set<String>> pckgStepMap = new HashMap<String, Set<String>>();
	private TextView pckgSelect;
	private LinearLayout pckgLine;
	private AlertDialog pckgAlertDialog = null;
	private String[] pckgArray;
	private String pckg = "";
	private String oldPckg = "";
	private TextView stepSelect;
	private LinearLayout stepLine;
	private AlertDialog stepAlertDialog = null;
	private String[] stepArray;
	private String step = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_rack_mgmt);
		setupActionBar();
		mFormView = findViewById(R.id.lot_rack_form);
		mStatusView = findViewById(R.id.lot_rack_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.lot_rack_tb_fragment);
		super.initTagBarcodeInput();
		lotListView = (ListView) findViewById(R.id.lot_rack_list);
		listItemAdapter = new SimpleAdapter(LotRackActivity.this, lotInfoListItem, R.layout.lot_rack_list_item, new String[] { INDEX, LOT, RACK, STEP, STEP_AGE, CARRIER_COUNT,
				STRIP_NUMBER }, new int[] {  R.id.lot_rack_item_index, R.id.lot_rack_item_lot, R.id.lot_rack_item_rack, R.id.lot_rack_item_step, R.id.lot_rack_item_step_age,
				R.id.lot_rack_item_carrier_count, R.id.lot_rack_item_strip_number });
		lotListView.setAdapter(listItemAdapter);
		lotListView.setOnItemClickListener(new OnItemClickListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				HashMap<String, String> item = (HashMap<String, String>) parent.getItemAtPosition(position);
				if (item.get(LOT).equals(toLoadMoreStr)) {
					requestPage++;
					lotInfoListItem.remove(lotInfoListItem.size() - 1);
					qTask = new QueryTask();
					qTask.execute("load");
				} else {
					String alotNumber = item.get(LOT);
					qTask = new QueryTask();
					qTask.execute("getCarriers", alotNumber);
				}
			}
		});
		
		pckgSelect = (TextView) findViewById(R.id.lot_rack_pckg);
		pckgLine = (LinearLayout) findViewById(R.id.lot_rack_pckg_line);
		pckgLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null == qTask && null == pckgAlertDialog) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("getPckg");
				} else {
					pckgAlertDialog.show();
				}
			}
		});
		stepSelect = (TextView) findViewById(R.id.lot_rack_step);
		stepLine = (LinearLayout) findViewById(R.id.lot_rack_step_line);
		stepLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!CommonUtility.isEmpty(pckg) && (CommonUtility.isEmpty(oldPckg) || !pckg.equals(oldPckg))) {
					oldPckg = pckg;
					getStepByPckg();
				} else {
					if (null != stepAlertDialog) {
						stepAlertDialog.show();
					}
				}
			}
		});
		
	}

	public class QueryTask extends AsyncTask<String, Void, BaseException> {
		DataCollection queryResult = null;
		String cmdName = "";
		String alotNumber = "";
		
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("loadByLot")) {
					rack = "";
					slot = "";
					bdNumber = "";
					pckg = "";
					step = "";
					clearPageInfo();
					lot = params[1];
					lotInfoListItem.clear();
					loadByLot();
				} else if (cmdName.equals("loadByRack")) {
					lot = "";
					rack = params[1];
					slot = params[2];
					lotInfoListItem.clear();
					load();
				} else if (cmdName.equals("loadByBD")) {
					lot = "";
					lotInfoListItem.clear();
					load();
				} else if (cmdName.equals("load")) {
					load();
				} else if (cmdName.equals("getCarriers")) {
					alotNumber = params[1];
					getCarriers();
				} else if (cmdName.equals("getPckg")) {
					getPckg();
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
				}
			} catch (BaseException e) {
				return e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(final BaseException e) {
			qTask = null;
			showProgress(false);
			tagBarcodeInput.requestFocus();
			if (null == e) {
				if (cmdName.equals("loadByLot")) {
					alotNumberTextView.setText(lot);
					loadAfter();
				} else if (cmdName.equals("loadByRack") || cmdName.equals("loadByBD") || cmdName.equals("load")) {
					alotNumberTextView.setText(bdNumber + ";" + rack + ":" + slot + " 共" + total + "条记录");
					loadAfter();
				} else if (cmdName.equals("getCarriers")) {
					getCarriersAfter();
				} else if (cmdName.equals("getPckg")) {
					getPckgAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotRackActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotRackActivity.this, e.getErrorMsg());
				}
			}
		}

		private void getCarriersAfter() {
			SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotRackActivity.this, carrierNameList, R.layout.lot_inquiry_list_item, new String[] {
					CARRIER_NAME, LOCATION }, new int[] { R.id.itemTitle, R.id.itemText });
			final ListView detailView = new ListView(LotRackActivity.this);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			lp.setMargins(6, 6, 6, 6);
			detailView.setLayoutParams(lp);
			detailView.setPadding(6, 6, 6, 6);
			detailView.setAdapter(detailListItemAdapter);
			AlertDialog.Builder builder = new AlertDialog.Builder(LotRackActivity.this);
			builder.setTitle(alotNumber).setIcon(android.R.drawable.ic_dialog_info).setView(detailView);
			builder.setPositiveButton(getResources().getString(R.string.close), null);
			builder.show();
		}
		
		private void getPckg() throws BaseException {
			String api = "getRackAssignments(attributes='assignmentOwner',assignmentOwnerType='PCKG_STEP')";
			queryResult = apiExecutorQuery.query(classname, "getPckg", api);
			for (ArrayList<String> result : queryResult) {
				String operation = result.get(0);
				if (operation.contains("_")) {
					String[] temp = operation.split("_");
					String pckg = temp[0];
					String step = temp[1];
					if (pckgStepMap.containsKey(pckg)) {
						pckgStepMap.get(pckg).add(step);
					} else {
						Set<String> stepSet = new HashSet<String>();
						stepSet.add(step);
						pckgStepMap.put(pckg, stepSet);
					}
				}
			}
			pckgArray = new String[pckgStepMap.size()];
			int i = 0;
			for (String temp : pckgStepMap.keySet()) {
				pckgArray[i] = temp;
				i++;
			}
		}
		
		private void getPckgAfter() {
			pckgAlertDialog = new AlertDialog.Builder(LotRackActivity.this).setTitle("选择package")
					.setItems(pckgArray, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							pckg = "" + pckgArray[which];
							pckgSelect.setText(pckg);
							if (!oldPckg.equals(pckg)) {
								stepSelect.setText(getResources().getString(R.string.pls_select));
							}
						}
					}).setNegativeButton("取消", null).create();
			pckgAlertDialog.show();
		}
		
		@SuppressLint("InflateParams")
		private void loadAfter() {
			if (pageTotal > requestPage) {
				Map<String, String> toLoadMore = new HashMap<String, String>();
				toLoadMore.put(LOT, toLoadMoreStr);
				lotInfoListItem.add(toLoadMore);
			}
			listItemAdapter.notifyDataSetChanged();
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void loadByLot() throws BaseException {
			lotInfoListItem.clear();
			String api = "getRackAttributes(content='" + lot + "',attributes='rackName,rackSlot')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "loadByLot", api);
			api = "getCarrierAttributes(lotNumber='" + lot + "', attributes='carrierId,carrierName')";
			DataCollection carrierQuery = apiExecutorQuery.query(classname, "loadByLot", api);
			CommonTrans commonTrans = new CommonTrans();
			String stripNumber = commonTrans.getLatestStripNumber(apiExecutorQuery, lot);
			if (!CommonUtility.isEmpty(queryResult)) {
				for (ArrayList<String> line : queryResult) {
					String rack = line.get(0);
					String slot = line.get(1);
					String step = "";
					String step_age = "";
					Map<String, String> titleLine = new HashMap<String, String>();
					titleLine.put(LOT, lot);
					titleLine.put(RACK, rack + ":" + slot);
					titleLine.put(STEP, step);
					titleLine.put(STEP_AGE, step_age);
					if (!CommonUtility.isEmpty(carrierQuery)) {
						titleLine.put(CARRIER_COUNT, "" + carrierQuery.size());
					} else {
						titleLine.put(CARRIER_COUNT, "");
					}
					titleLine.put(STRIP_NUMBER, stripNumber);
					lotInfoListItem.add(titleLine);
				}
			}
		}

		private void getCarriers() throws BaseException {
			carrierNameList.clear();
			String api = "getCarrierAttributes(lotNumber='" + alotNumber + "',attributes='carrierId,carrierName,location')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "getCarriers", api);
			for (ArrayList<String> temp : queryResult) {
				Map<String, String> map = new HashMap<String, String>();
				map.put(CARRIER_NAME, temp.get(1));
				map.put(LOCATION, "");
				carrierNameList.add(map);
			}
		}

		// private void loadByRack() throws BaseException {
		// lotInfoListItem.clear();
		// String api = "getRackAttributes(rackName='" + rack + "',rackSlot='" + slot + "',attributes='contents')";
		// DataCollection queryResult = apiExecutorQuery.query(classname, "loadByRack", api);
		// if (!CommonUtility.isEmpty(queryResult)) {
		// lot = queryResult.get(0).get(0);
		// }
		// }

		private void load() throws BaseException {
			// bondingDiagram,bondingDiagramRevision
			// String api = "getLotAttributes(attributes='lotNumber', bondingDiagram='" + bdNumber + "')";
			// DataCollection queryResult = apiExecutorQuery.query("RackMgmt", "loadAssignedCarriers", api);
			// if (!CommonUtility.isEmpty(queryResult)) {
			// lot = queryResult.get(0).get(0);
			// }
			String query = "&bondingDiagram=" + bdNumber + "&rack=" + rack + "&slot=" + slot + "&page=" + requestPage + "&pckg=" + pckg + "&step=" + step;
			CommonTrans commonTrans = new CommonTrans();
			String output = commonTrans.queryFromServer(serverLink + "?action=getLotRack" + query);
			if (!output.startsWith("Success||")) {
				throw new RfidException(output, classname, "load", query);
			}
			output = output.substring("Success||".length());
			String[] temp = output.split("\\|\\|");
			if (temp[0].equals("0")) {
				throw new RfidException("查询结果为0", classname, "load", query);
			}
			if (requestPage == 1) {
				total = Integer.parseInt(temp[0]);
				pageTotal = total / pageSize;
				if (pageTotal * pageSize < total) {
					pageTotal++;
				}
			}
			output = temp[1];
			String[] arr = output.split("" + (char) 4);
			for (String line : arr) {
				String[] tempArr = line.split("" + (char) 3);
				String lot = tempArr[0];
				String rack = tempArr[1];
				String slot = tempArr[2];
				String step = tempArr[3];
				String step_age = tempArr[4];
				String carrierCount = tempArr[5];
				String stripNumber = tempArr[6];
				Map<String, String> titleLine = new HashMap<String, String>();
				titleLine.put(INDEX, "" + index);
				index++;
				titleLine.put(LOT, lot);
				titleLine.put(RACK, rack + ": " + slot);
				titleLine.put(STEP, step);
				titleLine.put(STEP_AGE, step_age);
				titleLine.put(CARRIER_COUNT, carrierCount);
				titleLine.put(STRIP_NUMBER, stripNumber);
				lotInfoListItem.add(titleLine);
			}
		}
	}

	@Override
	protected void onResume() {
		// if (global.getScanTarget().equals(Constants.SCAN_TARGET_lot_rack_INIT)) {
		clearPageInfo();
		if (null != global.getAoLot()) {
			if (null == qTask) {
				alotNumberTextView.setText(lot);
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("loadByLot", global.getAoLot().getAlotNumber());
			}
		}
		if (!CommonUtility.isEmpty(global.getRackName()) && !CommonUtility.isEmpty(global.getSlotName())) {
			if (null == qTask) {
				alotNumberTextView.setText(bdNumber + ";" + rack + ":" + slot);
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("loadByRack", global.getRackName(), global.getSlotName());
			}
		}
		// }
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (null != qTask) {
			qTask.cancel(true);
		}
		super.onPause();
	}

	public void setBarcodeInput(String input) {
		log("Rack setLotNumber " + input);
		clearPageInfo();
		// lot, bonding diagram, 5-digit number bonding diagram, rack
		if (input.contains(".")) { // rack
			String[] temp = input.split("\\.");
			String rackName = temp[0];
			String slotName = temp[1];
			if (qTask == null) {
				alotNumberTextView.setText(bdNumber + ";" + rack + ":" + slot);
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("loadByRack", rackName, slotName);
			}
		} else if (input.length() == 14 && input.startsWith("33T")) {
			bdNumber = input.substring(8, 13);
			if (qTask == null && (!CommonUtility.isEmpty(pckg) || (!CommonUtility.isEmpty(rack) && !CommonUtility.isEmpty(slot)))) {
				alotNumberTextView.setText(bdNumber + ";" + rack + ":" + slot);
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("loadByBD");
			}
		} else if (input.length() == 5 && CommonUtility.isValidNumber(input)) {
			if (qTask == null && (!CommonUtility.isEmpty(pckg) || (!CommonUtility.isEmpty(rack) && !CommonUtility.isEmpty(slot)))) {
				bdNumber = input;
				alotNumberTextView.setText(bdNumber + ";" + rack + ":" + slot);
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("loadByBD");
			}
		} else {
			if (qTask == null) {
				alotNumberTextView.setText(lot);
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("loadByLot", input);
			}
		}
	}

	@Override
	public void setTagId(String tagId) {
		clearPageInfo();
		super.setTagId(tagId);
	}

	public void startScanBarcode() {
		if (qTask == null) {
			tagBarcodeInput.setText("");
			qTask = new QueryTask();
			qTask.execute("scanBarcode");
		}
	}

	private void clearPageInfo() {
		requestPage = 1;
		pageTotal = -1;
		total = -1;
		index = 1;
	}

	private void getStepByPckg() {
		Set<String> stepList = pckgStepMap.get(pckg);
		stepArray = new String[stepList.size()];
		int i = 0;
		for (String temp: stepList) {
			stepArray[i] = temp;
			i++;
		}
		stepAlertDialog = new AlertDialog.Builder(LotRackActivity.this).setTitle("选择step").setItems(stepArray, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				step = "" + stepArray[which];
				stepSelect.setText(step);
			}
		}).setNegativeButton("取消", null).create();
		stepAlertDialog.show();
	}
	
}
