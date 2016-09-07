package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.Carrier;
import com.fsl.cimei.rfid.entity.Step;
import com.fsl.cimei.rfid.exception.ApiException;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class PassWindowActivity extends BaseActivity {

	private QueryTask qTask = null;
	private Button buttonScanRackIn;
	private Button buttonScanRackOut;
	private TextView infoView;
	private ListView carrierListView;
	private final String classname = "PassWindow";
	private String alotNumber = "";
	private CarrierListAdapter carrierListAdapter;
	private List<Map<String, String>> carrierListItem = new ArrayList<Map<String, String>>();
	private final String CARRIER_ID = "CARRIER_ID";
	private final String CARRIER_NAME = "CARRIER_NAME";
	private final String RACK = "RACK";
	private final String SLOT = "SLOT";
	private final String CHECKED = "CHECKED";
	private final String LOCATION = "LOCATION";
	private String tagID = "";
	private String stripNumberStr = "";
	public static final String[] STEP_NAME = new String[] { "PASS_WINDOW", "PASS WINDOW" };
	private String rack = "";
	private String slot = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pass_window);
		setupActionBar();
		mFormView = findViewById(R.id.pass_window_form);
		mStatusView = findViewById(R.id.pass_window_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.pass_window_tb_fragment);
		super.initTagBarcodeInput();

		buttonScanRackIn = (Button) findViewById(R.id.button_scan_rack_in);
		buttonScanRackOut = (Button) findViewById(R.id.button_scan_rack_out);
		infoView = (TextView) findViewById(R.id.pass_window_alot_number);
		carrierListAdapter = new CarrierListAdapter(PassWindowActivity.this);
		carrierListView = (ListView) findViewById(R.id.pass_window_assigned_carriers);
		carrierListView.setAdapter(carrierListAdapter);

		buttonScanRackIn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				global.setCarrierID("");
				tagID = "";
				if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_INIT)) {
					global.setScanTarget(Constants.SCAN_TARGET_PASS_WINDOW_IN);
					buttonScanRackIn.setText(R.string.button_cancel);
					buttonScanRackIn.setEnabled(true);
					buttonScanRackOut.setText(R.string.button_scan_rack_out);
					buttonScanRackOut.setEnabled(false);
					alotNumber = "";
					infoView.setText("");
					carrierListItem.clear();
					carrierListAdapter.notifyDataSetChanged();
				} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_IN)) {
					Toast.makeText(PassWindowActivity.this, "操作取消", Toast.LENGTH_SHORT).show();
					global.setScanTarget(Constants.SCAN_TARGET_PASS_WINDOW_INIT);
					buttonScanRackIn.setText(R.string.button_scan_rack_in);
					buttonScanRackIn.setEnabled(true);
					buttonScanRackOut.setText(R.string.button_scan_rack_out);
					buttonScanRackOut.setEnabled(true);
				}
			}
		});

		buttonScanRackOut.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				global.setCarrierID("");
				tagID = "";
				if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_INIT)) {
					global.setScanTarget(Constants.SCAN_TARGET_PASS_WINDOW_OUT);
					buttonScanRackIn.setText(R.string.button_scan_rack_in);
					buttonScanRackIn.setEnabled(false);
					buttonScanRackOut.setText(R.string.button_cancel);
					buttonScanRackOut.setEnabled(true);
					rack = "";
					slot = "";
					// global.setRackName(""); global.setSlotName("");
					alotNumberTextView.setText("");
					alotNumber = "";
					infoView.setText("");
					carrierListItem.clear();
					carrierListAdapter.notifyDataSetChanged();
				} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_OUT)) {
					Toast.makeText(PassWindowActivity.this, "操作取消", Toast.LENGTH_SHORT).show();
					global.setScanTarget(Constants.SCAN_TARGET_PASS_WINDOW_INIT);
					buttonScanRackIn.setText(R.string.button_scan_rack_in);
					buttonScanRackIn.setEnabled(true);
					buttonScanRackOut.setText(R.string.button_scan_rack_out);
					buttonScanRackOut.setEnabled(true);
				}
			}
		});
	}

	public class QueryTask extends AsyncTask<String, Void, BaseException> {
		DataCollection queryResult = null;
		String cmdName = "";
		String resultCode = "";

		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("checkCarrierID")) {
					checkCarrierID();
				} else if (cmdName.equals("putItemOnRack")) {
					putItemOnRack(false);
				} else if (cmdName.equals("startLot")) {
					putItemOnRack(true);
				} else if (cmdName.equals("takeItemOffRack")) {
					takeItemOffRack(false);
				} else if (cmdName.equals("endLot")) {
					takeItemOffRack(true);
				} else if (cmdName.equals("checkBarcodeInput")) {
					String input = params[1];
					checkBarcodeInput(input);
				}
			} catch (BaseException e) {
				return e;
			}
			return null;
		}

		private void putItemOnRack(boolean start) throws BaseException {
			ArrayList<String> APICommandList = new ArrayList<String>();
			for (int i = 0; i < carrierListItem.size(); i++) {
				if (carrierListItem.get(i).get(CHECKED).equals("Y")) {
					String carrierId = carrierListItem.get(i).get(CARRIER_ID);
					String rack = carrierListItem.get(i).get(RACK);
					String slot = carrierListItem.get(i).get(SLOT);
					if (!CommonUtility.isEmpty(carrierId) && !CommonUtility.isEmpty(rack) && !CommonUtility.isEmpty(slot)) {
						APICommandList.add("putItemOnRack(transUserId='" + global.getUser().getUserID() + "',rackName='" + rack + "',rackSlot='" + slot + "',content='"
								+ carrierId + "')");
					}
				}
			}
			if (start) {
				CommonTrans commonTrans = new CommonTrans();
				DataCollection currentStepContext = commonTrans.getCurrentStepContext(apiExecutorQuery, alotNumber);
				Step currentStep = new Step();
				if (currentStepContext.size() > 0) {
					String ProcNameResult = currentStepContext.get(0).get(0).trim();
					String StepSeqResult = currentStepContext.get(0).get(1).trim();
					String stepNameSession = currentStepContext.get(0).get(2).trim();
					currentStep.setProcName(ProcNameResult);
					currentStep.setStepSeq(StepSeqResult);
					currentStep.setStepName(stepNameSession);
				} else {
					throw new RfidException("Lot [" + alotNumber + "] 不在step", classname, "startLot", "getCurrentStepContext");
				}
				commonTrans.validateStripNumber(apiExecutorUpdate, apiExecutorQuery, alotNumber, currentStep.getStepName(), currentStep.getProcName(),
						currentStep.getStepSeq(), global.getUser().getUserID(), Constants.EVENT_CODE, global.getAndroidSecureID());
				String api = "startLotAtStep(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + alotNumber + "',stepName='" + currentStep.getStepName()
						+ "')";
				APICommandList.add(api);
			}
			try {
				apiExecutorUpdate.executeMultipleAPI(global.getUser().getUserID(), APICommandList);
				if (!CommonUtility.isEmpty(apiExecutorUpdate.getMessage())) {
					throw new ApiException("提交失败 " + apiExecutorUpdate.getMessage(), classname, "putItemOnRack", APICommandList.toString());
				}
			} catch (Exception e) {
				throw new ApiException("提交失败 " + e.toString(), classname, "putItemOnRack", APICommandList.toString());
			}
		}

		private void takeItemOffRack(boolean end) throws BaseException {
			ArrayList<String> APICommandList = new ArrayList<String>();
			for (int i = 0; i < carrierListItem.size(); i++) {
				if (carrierListItem.get(i).get(CHECKED).equals("Y")) {
					String carrierId = carrierListItem.get(i).get(CARRIER_ID);
					String rack = carrierListItem.get(i).get(RACK);
					String slot = carrierListItem.get(i).get(SLOT);
					if (!CommonUtility.isEmpty(carrierId) && !CommonUtility.isEmpty(rack) && !CommonUtility.isEmpty(slot)) {
						String api = "takeItemOffRack(transUserId='" + global.getUser().getUserID() + "',rackName='" + rack + "',rackSlot='" + slot + "',content='"
								+ carrierId + "')";
						APICommandList.add(api);
					}
				}
			}
			if (end) {
				CommonTrans commonTrans = new CommonTrans();
				DataCollection currentStepContext = commonTrans.getCurrentStepContext(apiExecutorQuery, alotNumber);
				Step currentStep = new Step();
				if (currentStepContext.size() > 0) {
					String ProcNameResult = currentStepContext.get(0).get(0).trim();
					String StepSeqResult = currentStepContext.get(0).get(1).trim();
					String stepNameSession = currentStepContext.get(0).get(2).trim();
					currentStep.setProcName(ProcNameResult);
					currentStep.setStepSeq(StepSeqResult);
					currentStep.setStepName(stepNameSession);
				} else {
					throw new RfidException("Lot [" + alotNumber + "] 不在step", classname, "endLot", "getCurrentStepContext");
				}
				if (!currentStep.getStepName().equals(STEP_NAME[0]) && !currentStep.getStepName().equals(STEP_NAME[1])) {
					throw new RfidException("Lot [" + alotNumber + "] 不在pass window step", classname, "endLot", "getCurrentStepContext");
				}
				String api = "endLotAtStep(transUserId='" + global.getUser().getUserID() + "', lotNumber='" + alotNumber + "',stepName='" + currentStep.getStepName()
						+ "')";
				logf(api);
				APICommandList.add(api);
			}
			try {
				apiExecutorUpdate.executeMultipleAPI(global.getUser().getUserID(), APICommandList);
				if (!CommonUtility.isEmpty(apiExecutorUpdate.getMessage())) {
					throw new ApiException("提交失败 " + apiExecutorUpdate.getMessage(), classname, "takeItemOffRack", APICommandList.toString());
				}
			} catch (Exception e) {
				throw new ApiException("提交失败 " + e.toString(), classname, "takeItemOffRack", APICommandList.toString());
			}
		}

		private void checkBarcodeInput(String input) throws BaseException {
			CommonTrans commonTrans = new CommonTrans();
			List<Carrier> carrierList = commonTrans.checkBarcodeInput(apiExecutorQuery, input);
			if (carrierList.isEmpty()) {
				if (alotNumber.isEmpty()) {
					commonTrans.validateLotNumber(apiExecutorQuery, input);
					if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_INIT)) {
						throw new RfidException("请先选择相应操作", classname, "checkBarcodeInput", "");
					} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_IN) && (CommonUtility.isEmpty(rack) || CommonUtility.isEmpty(slot))) {
						throw new RfidException("放入物料请先扫描pass window", classname, "checkBarcodeInput", "");
					}
					alotNumber = input;
					DataCollection currentStepContext = commonTrans.getCurrentStepContext(apiExecutorQuery, alotNumber);
					if (currentStepContext.size() > 0) {
						String stepNameSession = currentStepContext.get(0).get(2).trim();
						if (!stepNameSession.equals(STEP_NAME[0]) && !stepNameSession.equals(STEP_NAME[1])) {
							throw new RfidException("Lot [" + alotNumber + "] 不在pass window step", classname, "checkBarcodeInput", "getCurrentStepContext");
						}
					} else {
						throw new RfidException("Lot [" + alotNumber + "] 不在step", classname, "checkBarcodeInput", "getCurrentStepContext");
					}
					stripNumberStr = commonTrans.getLatestStripNumber(apiExecutorQuery, alotNumber);
					loadAssignedCarriers(alotNumber);
					resultCode = "load";
				} else {
					throw new RfidException("请先取消当次操作再输入新的批号", classname, "checkBarcodeInput", "");
				}
			} else if (carrierList.size() == 1) {
				tagID = carrierList.get(0).getTagID();
				cmdName = "checkCarrierID";
				checkNewLotCarrier(carrierList.get(0));
			} else {
				if (alotNumber.isEmpty()) {
					throw new RfidException(input + " 对应多个弹夹", classname, "checkBarcodeInput", "1");
				} else {
					Carrier carrier = new Carrier();
					int total = 0;
					for (Carrier c : carrierList) {
						if (c.getLotNumber().equals(alotNumber)) {
							total++;
							carrier = c;
						}
					}
					if (total == 0) { // 混料
						throw new RfidException("混料！当前弹匣[" + input + "]不属于所找批号" + alotNumber + ".请确认后重试。", classname, "checkBarcodeInput", "2");
					} else if (total == 1) {
						tagID = carrier.getTagID();
						cmdName = "checkCarrierID";
						checkNewLotCarrier(carrier);
					} else { // 这批料有多个名字相同的弹夹？？
						throw new RfidException(input + " 对应多个弹夹", classname, "checkBarcodeInput", "3");
					}
				}
			}
		}
		
		private void checkCarrierID() throws BaseException {
			String api = "getCarrierAttributes(carrierId='" + tagID + "',attributes='status,location,receiptDate,lotNumber,carrierName,carrierType')"; // ,carrierLayer,carrierGroupId,cassetteOrMagazine,waferLotNumber
			DataCollection queryResult = apiExecutorQuery.query(classname, "checkCarrierID", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				String carrierName = queryResult.get(0).get(4);
				if (!CommonUtility.isEmpty(queryResult.get(0).get(5)) && queryResult.get(0).get(5).equals("RACK")) {
					String[] temp = carrierName.split(":");
					rack = temp[0];
					slot = temp[1];
					if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_OUT)) {
						throw new RfidException("请扫描物料标签", classname, "checkCarrierID", "");
					}
					resultCode = "rack";
				} else if (!CommonUtility.isEmpty(queryResult.get(0).get(3)) && !queryResult.get(0).get(3).equalsIgnoreCase("None")) {
					Carrier carrier = new Carrier(tagID, queryResult.get(0).get(4));
					carrier.setStatus(queryResult.get(0).get(0));
					carrier.setLocation(queryResult.get(0).get(1));
					carrier.setLotNumber(queryResult.get(0).get(3));
					carrier.setCarrierType(queryResult.get(0).get(5));
					checkNewLotCarrier(carrier);
				} else {
					throw new RfidException("此RFID标签[" + carrierName + "]未对应物料", classname, "checkCarrierID", api);
				}
			} else {
				throw new RfidException("此RFID标签不存在", classname, "checkCarrierID", api);
			}
		}

		@Override
		protected void onPostExecute(final BaseException e) {
			qTask = null;
			showProgress(false);
			tagBarcodeInput.requestFocus();
			if (null == e) {
				if ((cmdName.equals("checkCarrierID") && resultCode.equals("load")) || (cmdName.equals("checkBarcodeInput") && resultCode.equals("load"))) {
					StringBuilder builder = new StringBuilder();
					if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_IN)) {
						infoView.setText(getResources().getString(R.string.button_scan_rack_in) + "：" + alotNumber + " 共" + carrierListItem.size() + "个弹夹，条数为："
								+ stripNumberStr);
						for (Map<String, String> m : carrierListItem) {
							if (!CommonUtility.isEmpty(m.get(RACK)) || !CommonUtility.isEmpty(m.get(SLOT))) {
								builder.append("弹夹[" + m.get(CARRIER_NAME) + "]已被放入passwindow[" + m.get(RACK) + ":" + m.get(SLOT) + "]; ");
							}
						}
					} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_OUT)) {
						infoView.setText(getResources().getString(R.string.button_scan_rack_out) + "：" + alotNumber + " 共" + carrierListItem.size() + "个弹夹，条数为："
								+ stripNumberStr);
						for (Map<String, String> m : carrierListItem) {
							if (CommonUtility.isEmpty(m.get(RACK)) || CommonUtility.isEmpty(m.get(SLOT))) {
								builder.append("弹夹[" + m.get(CARRIER_NAME) + "]未被放入passwindow; ");
							}
						}
					}
					if (cmdName.equals("checkCarrierID")) {
						checkNewTagInView(builder.toString());
					}
				} else if (cmdName.equals("checkCarrierID") && resultCode.equals("refresh")) {
					checkNewTagInView("");
				} else if (cmdName.equals("checkCarrierID") && resultCode.equals("rack")) {
					if (!CommonUtility.isEmpty(rack) && !CommonUtility.isEmpty(slot)) {
						alotNumberTextView.setText(rack + ":" + slot);
					}
				} else if (cmdName.equals("startLot")) {
					toastMsg("放入成功并开批");
					global.setScanTarget(Constants.SCAN_TARGET_PASS_WINDOW_INIT);
					global.setCarrierID("");
					tagID = "";
					buttonScanRackIn.setText(R.string.button_scan_rack_in);
					buttonScanRackIn.setEnabled(true);
					buttonScanRackOut.setText(R.string.button_scan_rack_out);
					buttonScanRackOut.setEnabled(true);
				} else if (cmdName.equals("endLot")) {
					toastMsg("取出成功并结批");
					global.setScanTarget(Constants.SCAN_TARGET_PASS_WINDOW_INIT);
					global.setCarrierID("");
					tagID = "";
					buttonScanRackIn.setText(R.string.button_scan_rack_in);
					buttonScanRackIn.setEnabled(true);
					buttonScanRackOut.setText(R.string.button_scan_rack_out);
					buttonScanRackOut.setEnabled(true);
				} else if (cmdName.equals("putItemOnRack")) {
					toastMsg("放入成功");
					global.setScanTarget(Constants.SCAN_TARGET_PASS_WINDOW_INIT);
					global.setCarrierID("");
					tagID = "";
					buttonScanRackIn.setText(R.string.button_scan_rack_in);
					buttonScanRackIn.setEnabled(true);
					buttonScanRackOut.setText(R.string.button_scan_rack_out);
					buttonScanRackOut.setEnabled(true);
				} else if (cmdName.equals("takeItemOffRack")) {
					toastMsg("取出成功");
					global.setScanTarget(Constants.SCAN_TARGET_PASS_WINDOW_INIT);
					global.setCarrierID("");
					tagID = "";
					buttonScanRackIn.setText(R.string.button_scan_rack_in);
					buttonScanRackIn.setEnabled(true);
					buttonScanRackOut.setText(R.string.button_scan_rack_out);
					buttonScanRackOut.setEnabled(true);
				}
			} else {
				global.setCarrierID("");
				tagID = "";
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(PassWindowActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(PassWindowActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void checkNewLotCarrier(Carrier c) throws BaseException {
			if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_INIT)) {
				throw new RfidException("请先选择相应操作", classname, "checkNewLotCarrier", "");
			} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_IN) && (CommonUtility.isEmpty(rack) || CommonUtility.isEmpty(slot))) {
				throw new RfidException("放入物料请先扫描pass window", classname, "checkNewLotCarrier", "");
			}
			if (alotNumber.isEmpty()) { // the first tag of current lot
				alotNumber = c.getLotNumber();
				CommonTrans commonTrans = new CommonTrans();
				DataCollection currentStepContext = commonTrans.getCurrentStepContext(apiExecutorQuery, alotNumber);
				if (currentStepContext.size() > 0) {
					String stepNameSession = currentStepContext.get(0).get(2).trim();
					if (!stepNameSession.equals(STEP_NAME[0]) && !stepNameSession.equals(STEP_NAME[1])) {
						throw new RfidException("Lot [" + alotNumber + "] 不在pass window step", classname, "checkNewLotCarrier", "getCurrentStepContext");
					}
				} else {
					throw new RfidException("Lot [" + alotNumber + "] 不在step", classname, "checkNewLotCarrier", "getCurrentStepContext");
				}
				stripNumberStr = commonTrans.getLatestStripNumber(apiExecutorQuery, alotNumber);
				loadAssignedCarriers(alotNumber);
				resultCode = "load";
			} else if (!alotNumber.equals(c.getLotNumber())) {
				throw new RfidException("混料！当前弹匣[" + c.getTagName() + "]不属于所找批号" + alotNumber + ".请确认后重试。", classname, "checkNewLotCarrier", "");
			} else {
				resultCode = "refresh";
			}
		}
		
		private void loadAssignedCarriers(String alotNumber) throws BaseException {
			// select rc.* from GENESIS.RACKS r,GENESIS.RACK_CONTENTS rc where r.rack_name=rc.rack_name and r.rack_slot=rc.rack_slot and r.RACK_TYPE='MAGAZINE' and rc.CONTENT in ()
			HashMap<String, String[]> dataMap = new HashMap<String, String[]>(); // carrier id -> array{name,loc,rack,slot}
			StringBuilder sqlBuilder = new StringBuilder();
			String api = "getCarrierAttributes(lotNumber='" + alotNumber + "', attributes='carrierId,carrierName,location')";
			DataCollection assignedCarrier = apiExecutorQuery.query("RackMgmt", "loadAssignedCarriers", api);
			if (!CommonUtility.isEmpty(assignedCarrier)) {
				for (ArrayList<String> c : assignedCarrier) {
					dataMap.put(c.get(0), new String[] { c.get(1), ((CommonUtility.isEmpty(c.get(2)) || c.get(2).equalsIgnoreCase("None")) ? "" : c.get(2)), "", "" });
					sqlBuilder.append(",\\\\'").append(c.get(0)).append("\\\\'");
				}
			}
			if (!sqlBuilder.toString().isEmpty()) {
				api = "execSql('select rc.CONTENT,rc.rack_name,rc.rack_slot from GENESIS.RACKS r,GENESIS.RACK_CONTENTS rc where r.rack_name=rc.rack_name and r.rack_slot=rc.rack_slot "
						+ "and r.RACK_TYPE=\\\\'MAGAZINE\\\\' and rc.CONTENT in (" + sqlBuilder.toString().substring(1) + ")')";
				DataCollection rackInfo = apiExecutorQuery.query("RackMgmt", "loadAssignedCarriers", api);
				if (!CommonUtility.isEmpty(rackInfo)) {
					for (ArrayList<String> r : rackInfo) {
						String[] temp = dataMap.get(r.get(0));
						temp[2] = r.get(1);
						temp[3] = r.get(2);
					}
				}
			}
			for (String carrierId : dataMap.keySet()) {
				Map<String, String> m = new HashMap<String, String>();
				m.put(CARRIER_ID, carrierId);
				m.put(CARRIER_NAME, dataMap.get(carrierId)[0]);
				m.put(CHECKED, "N");
				m.put(LOCATION, dataMap.get(carrierId)[1]);
				m.put(RACK, dataMap.get(carrierId)[2]);
				m.put(SLOT, dataMap.get(carrierId)[3]);
				carrierListItem.add(m);
			}
		}
	}

	private class CarrierLineHolder {
		public TextView name;
		public TextView rack;
		public TextView slot;
		public ImageView flag;
	}

	@Override
	protected void onResume() {
		// if (!CommonUtility.isEmpty(global.getCarrierID()) && !global.getCarrierID().equals(this.tagID)) {
		// String carrierID = global.getCarrierID();
		// global.setCarrierID("");
		// setTagId(carrierID);
		// }
		super.onResume();
	}

	public void checkNewTagInView(String errorMsg) {
		boolean isNew = false;
		boolean found = false;
		if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_IN)) {
			for (Map<String, String> m : carrierListItem) {
				if (m.get(CARRIER_ID).equals(tagID)) {
					found = true;
					m.put(RACK, rack);
					m.put(SLOT, slot);
					if (m.get(CHECKED).equals("N")) {
						isNew = true;
						m.put(CHECKED, "Y");
					}
				}
			}
			if (!found) {
				showError(PassWindowActivity.this, "混料！当前弹匣[" + tagID + "]不属于所找批号" + alotNumber + ".请确认后重试。");
			} else if (isNew) {
				carrierListAdapter.notifyDataSetChanged();
				if (errorMsg.isEmpty()) {
					checkForStartEnd();
				} else {
					showError(PassWindowActivity.this, errorMsg);
					buttonScanRackIn.setText(R.string.button_scan_rack_in);
					buttonScanRackIn.setEnabled(true);
					buttonScanRackOut.setText(R.string.button_scan_rack_out);
					buttonScanRackOut.setEnabled(true);
				}
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_OUT)) {
			for (Map<String, String> m : carrierListItem) {
				if (m.get(CARRIER_ID).equals(tagID)) {
					found = true;
					if (m.get(CHECKED).equals("N")) {
						isNew = true;
						m.put(CHECKED, "Y");
					}
				}
			}
			if (!found) {
				showError(PassWindowActivity.this, "混料！当前弹匣[" + tagID + "]不属于所找批号" + alotNumber + ".请确认后重试。");
			} else if (isNew) {
				carrierListAdapter.notifyDataSetChanged();
				if (errorMsg.isEmpty()) {
					checkForStartEnd();
				} else {
					showError(PassWindowActivity.this, errorMsg);
					buttonScanRackIn.setText(R.string.button_scan_rack_in);
					buttonScanRackIn.setEnabled(true);
					buttonScanRackOut.setText(R.string.button_scan_rack_out);
					buttonScanRackOut.setEnabled(true);
				}
			}
		}
	}

	public void checkForStartEnd() {
		if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_IN)) {
			boolean allChecked = true;
			for (Map<String, String> m : carrierListItem) {
				if (m.get(CHECKED).equals("N")) {
					allChecked = false;
				}
			}
			if (allChecked) {
				showStartLotDialog();
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_OUT)) {
			boolean allChecked = true;
			StringBuilder builder = new StringBuilder();
			for (Map<String, String> m : carrierListItem) {
				if (m.get(CHECKED).equals("N")) {
					allChecked = false;
				}
				if (!m.get(LOCATION).equals(m.get(RACK) + ":" + m.get(SLOT))) {
					builder.append("弹夹" + m.get(CARRIER_NAME) + "的rack信息为[" + m.get(RACK) + ":" + m.get(SLOT) + "]，而该弹夹location为" + m.get(LOCATION) + "; ");
				}
			}
			if (allChecked) {
				showEndLotDialog(builder.toString());
			}
		}
	}

	public void showStartLotDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(PassWindowActivity.this);
		builder.setTitle(getResources().getString(R.string.title_message)).setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("所有弹夹都已放入，条数为：" + stripNumberStr + "，是否要开批？");
		if (global.getUser().getDepartment().startsWith("F/E")) {
			builder.setPositiveButton("放入并开批", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (null == qTask) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("startLot");
					}
				}
			});
		} else if (global.getUser().getDepartment().startsWith("B/E")) {
			builder.setPositiveButton("只放入，不开批", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (null == qTask) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("putItemOnRack");
					}
				}
			});
		}
		builder.setNegativeButton(getResources().getString(R.string.close), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				buttonScanRackIn.setText(R.string.button_scan_rack_in);
				buttonScanRackIn.setEnabled(true);
				buttonScanRackOut.setText(R.string.button_scan_rack_out);
				buttonScanRackOut.setEnabled(true);
				global.setScanTarget(Constants.SCAN_TARGET_PASS_WINDOW_INIT);
				alotNumber = "";
				toastMsg("操作取消");
			}
		});
		builder.show();
	}

	public void showEndLotDialog(String alertMsg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(PassWindowActivity.this);
		builder.setTitle(getResources().getString(R.string.title_message)).setIcon(android.R.drawable.ic_dialog_info)
				.setMessage(alertMsg + "所有弹夹都已取出，条数为：" + stripNumberStr + "，是否要结批？");
		builder.setNegativeButton(getResources().getString(R.string.close), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				buttonScanRackIn.setText(R.string.button_scan_rack_in);
				buttonScanRackIn.setEnabled(true);
				buttonScanRackOut.setText(R.string.button_scan_rack_out);
				buttonScanRackOut.setEnabled(true);
				global.setScanTarget(Constants.SCAN_TARGET_PASS_WINDOW_INIT);
				alotNumber = "";
				toastMsg("操作取消");
			}
		});
		if (global.getUser().getDepartment().startsWith("F/E")) {
			builder.setPositiveButton("只取出，不结批", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (null == qTask) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("takeItemOffRack");
					}
				}
			});

		} else if (global.getUser().getDepartment().startsWith("B/E")) {
			builder.setNeutralButton("只取出，不结批", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (null == qTask) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("takeItemOffRack");
					}
				}
			});
			builder.setPositiveButton("取出并结批", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (null == qTask) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("endLot");
					}
				}
			});
		}
		builder.show();
	}

	@Override
	protected void onPause() {
		if (null != qTask) {
			qTask.cancel(true);
		}
		super.onPause();
	}

	public void setBarcodeInput(String input) { // rack name or magazine name
		if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_INIT)) {
			String[] temp = null;
			if (input.contains(".")) {
				temp = input.split("\\.");
			} else if (input.contains(":")) {
				temp = input.split(":");
			}
			if (null != temp) {
				rack = temp[0];
				slot = temp[1];
				alotNumberTextView.setText(rack + ":" + slot);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_IN)) {
			String[] temp = null;
			if (input.contains(".")) {
				temp = input.split("\\.");
			} else if (input.contains(":")) {
				temp = input.split(":");
			}
			if (null != temp) {
				rack = temp[0];
				slot = temp[1];
				alotNumberTextView.setText(rack + ":" + slot);
			} else if (null == qTask) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("checkBarcodeInput", input);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_OUT)) {
			if (input.contains(".") || input.contains(":")) {
			} else if (null == qTask) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("checkBarcodeInput", input);
			}
		}
	}

	@Override
	public void setTagId(String tagId) {
		// mag carrier or rack carrier
		if (null == qTask && global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_INIT)) {
			this.tagID = tagId;
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("checkCarrierID", this.tagID);
		} else if (null == qTask && global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_IN)) {
			this.tagID = tagId;
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("checkCarrierID", this.tagID);
		} else if (null == qTask && global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_OUT)) {
			this.tagID = tagId;
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("checkCarrierID", this.tagID);
		}
	}

	public void startScanBarcode() {
		if (qTask == null) {
			tagBarcodeInput.setText("");
			qTask = new QueryTask();
			qTask.execute("scanBarcode");
		}
	}

	class CarrierListAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private CarrierLineHolder holder = null;

		public CarrierListAdapter(Context context) {
			this.mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return carrierListItem.size();
		}

		@Override
		public Object getItem(int arg0) {
			return carrierListItem.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				holder = new CarrierLineHolder();
				convertView = mInflater.inflate(R.layout.pass_window_carrier_list_item, null);
				holder.flag = (ImageView) convertView.findViewById(R.id.pass_window_carrier_item_flag);
				holder.name = (TextView) convertView.findViewById(R.id.pass_window_carrier_item_name);
				holder.rack = (TextView) convertView.findViewById(R.id.pass_window_carrier_item_rack);
				holder.slot = (TextView) convertView.findViewById(R.id.pass_window_carrier_item_slot);
				convertView.setTag(holder);
			} else {
				holder = (CarrierLineHolder) convertView.getTag();
			}
			holder.name.setText(carrierListItem.get(position).get(CARRIER_NAME));
			holder.rack.setText(carrierListItem.get(position).get(RACK));
			holder.slot.setText(carrierListItem.get(position).get(SLOT));
			if (carrierListItem.get(position).get(CHECKED).equals("Y")) {
				holder.flag.setVisibility(View.VISIBLE);
			} else {
				holder.flag.setVisibility(View.INVISIBLE);
			}
			return convertView;
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		String carrierID = intent.getStringExtra("carrierID");
		if (!CommonUtility.isEmpty(carrierID) && !carrierID.equals(this.tagID)) {
			setTagId(carrierID);
		}
		super.onNewIntent(intent);
	}

}
