package com.fsl.cimei.rfid;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.AOLot;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class RackMgmtActivity extends BaseActivity {

	private QueryTask qTask = null;
	private Button buttonScanRackIn;
	private Button buttonScanRackOut;
	private TextView rackLotInfoView;
	private DataCollection assignedCarrier;
	private LinearLayout assignedCarrierListView;
	private String carrierID = "";
	private final String classname = "RackMgmt";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rack_mgmt);
		setupActionBar();
		mFormView = findViewById(R.id.rack_mgmt_form);
		mStatusView = findViewById(R.id.rack_mgmt_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.rack_mgmt_tb_fragment);
		super.initTagBarcodeInput();

		buttonScanRackIn = (Button) findViewById(R.id.button_scan_rack_in);
		buttonScanRackOut = (Button) findViewById(R.id.button_scan_rack_out);
		rackLotInfoView = (TextView) findViewById(R.id.rack_mgmt_alot_number);
		assignedCarrier = new DataCollection();
		assignedCarrierListView = (LinearLayout) findViewById(R.id.rack_mgmt_assigned_carriers);

		buttonScanRackIn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_INIT)) {
					global.setScanTarget(Constants.SCAN_TARGET_RACK_IN);
					buttonScanRackIn.setText(R.string.button_done);
					buttonScanRackIn.setEnabled(true);
					buttonScanRackOut.setText(R.string.button_scan_rack_out);
					buttonScanRackOut.setEnabled(false);
					rackLotInfoView.setText("");
					assignedCarrierListView.removeAllViews();
				} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_IN)) {
					global.setScanTarget(Constants.SCAN_TARGET_RACK_INIT);
					global.setAoLot(null);
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
				if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_INIT)) {
					global.setScanTarget(Constants.SCAN_TARGET_RACK_OUT);
					buttonScanRackIn.setText(R.string.button_scan_rack_in);
					buttonScanRackIn.setEnabled(false);
					buttonScanRackOut.setText(R.string.button_done);
					buttonScanRackOut.setEnabled(true);
					alotNumberTextView.setText("");
					rackLotInfoView.setText("");
					assignedCarrierListView.removeAllViews();
					global.setRackName("");
					global.setSlotName("");
				} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_OUT)) {
					global.setScanTarget(Constants.SCAN_TARGET_RACK_INIT);
					global.setAoLot(null);
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
				if (cmdName.equals("loadAssignedCarriers")) {
					loadAssignedCarriers();
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
				} else if (cmdName.equals("putLotOnRack")) {
					CommonTrans commonTrans = new CommonTrans();
					commonTrans.putLotOnRack(apiExecutorQuery, apiExecutorUpdate, global, params[1]);
					global.setAoLot(new AOLot(params[1]));
					loadAssignedCarriers();
				} else if (cmdName.equals("takeLotOffRack")) {
					CommonTrans commonTrans = new CommonTrans();
					commonTrans.takeLotOffRack(apiExecutorQuery, apiExecutorUpdate, global, params[1]);
					global.setAoLot(new AOLot(params[1]));
					loadAssignedCarriers();
				} else if (cmdName.equals("checkCarrier")) {
					checkCarrier(params[1]);
				}
			} catch (BaseException e) {
				return e;
			}
			return null;
		}

		private void checkCarrier(String mCarrierID) throws BaseException {
			String api = "getCarrierAttributes(carrierId='" + mCarrierID
					+ "', attributes='status,location,receiptDate,lotNumber,carrierName,carrierType,carrierLayer,carrierGroupId')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "getCarrierForRack", api);
			if (!CommonUtility.isEmpty(queryResult) && queryResult.size() == 1) {
				String carrierType = queryResult.get(0).get(5);
				if (!CommonUtility.isEmpty(carrierType) && carrierType.equals("RACK")) {
					String location = queryResult.get(0).get(4); // carrierName
					String[] temp = location.split(":");
					global.setRackName(temp[0]);
					global.setSlotName(temp[1]);
					resultCode = "RACK";
				} else if (!CommonUtility.isEmpty(queryResult.get(0).get(3)) && !queryResult.get(0).get(3).equals("None")) {
					String alotNumber = queryResult.get(0).get(3);
					if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_IN) && !CommonUtility.isEmpty(global.getRackName()) && !CommonUtility.isEmpty(global.getSlotName())) {
						global.setAoLot(new AOLot(alotNumber));
						resultCode = "LOT";
						CommonTrans commonTrans = new CommonTrans();
						commonTrans.putLotOnRack(apiExecutorQuery, apiExecutorUpdate, global, alotNumber);
						loadAssignedCarriers();
					} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_OUT)) {
						global.setAoLot(new AOLot(alotNumber));
						resultCode = "LOT";
						CommonTrans commonTrans = new CommonTrans();
						commonTrans.takeLotOffRack(apiExecutorQuery, apiExecutorUpdate, global, alotNumber);
						loadAssignedCarriers();
					}
				} else {
					throw new RfidException("该标签既不对应物料也不是对应rack", classname, "getCarrierForRack", api);
				}
			} else {
				throw new RfidException("标签不存在或标签信息有误。", classname, "getCarrierForRack", api);
			}
		}

		@Override
		protected void onPostExecute(final BaseException e) {
			qTask = null;
			showProgress(false);
			tagBarcodeInput.requestFocus();
			if (null == e) {
				if (cmdName.equals("loadAssignedCarriers") || cmdName.equals("putLotOnRack") || cmdName.equals("takeLotOffRack")) {
					loadAssignedCarriersAfter();
				} else if (cmdName.equals("checkCarrier")) {
					if (resultCode.equals("RACK")) {
						alotNumberTextView.setText(global.getRackName() + ":" + global.getSlotName());
					} else if (resultCode.equals("LOT")) {
						loadAssignedCarriersAfter();
					}
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(RackMgmtActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(RackMgmtActivity.this, e.getErrorMsg());
				}
			}
		}

		@SuppressLint("InflateParams")
		private void loadAssignedCarriersAfter() {
//			if (!CommonUtility.isEmpty(global.getRackName()) && !CommonUtility.isEmpty(global.getSlotName())) {
//				alotNumberTextView.setText(global.getRackName() + ":" + global.getSlotName());
//			}
			assignedCarrierListView.removeAllViews();
			for (ArrayList<String> c : assignedCarrier) {
				LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.rack_mgmt_carrier_list_item, null);
				TextView tagNameTextView = (TextView) l.findViewById(R.id.rack_mgmt_carrier_title);
				String location = c.get(2);
				if (CommonUtility.isEmpty(location) || location.equalsIgnoreCase("None")) {
					location = "";
				} else {
				}
				tagNameTextView.setText(c.get(1)); // + "[Loc:" + location + "]"
				assignedCarrierListView.addView(l);
			}
			if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_IN)) {
				rackLotInfoView.setText(getResources().getString(R.string.button_scan_rack_in) + ": " + global.getAoLot().getAlotNumber() + " 共" + assignedCarrier.size() + "个弹夹");// ，已放入" + carriersIn + "个弹夹"
			} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_OUT)) {
				rackLotInfoView.setText(getResources().getString(R.string.button_scan_rack_out) + ": " + global.getAoLot().getAlotNumber() + " 共" + assignedCarrier.size() + "个弹夹");// ，已取出" + carriersOut + "个弹夹"
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void loadAssignedCarriers() throws BaseException {
			if (!CommonUtility.isEmpty(global.getAoLot().getAlotNumber())) {
				String api = "getCarrierAttributes(lotNumber='" + global.getAoLot().getAlotNumber() + "', attributes='carrierId,carrierName,location')";
				assignedCarrier = apiExecutorQuery.query(classname, "loadAssignedCarriers", api);
			}
		}
	}

	@Override
	protected void onResume() {
		if (!CommonUtility.isEmpty(global.getRackName()) && !CommonUtility.isEmpty(global.getSlotName())) {
			alotNumberTextView.setText(global.getRackName() + ":" + global.getSlotName());
		}
		if (!CommonUtility.isEmpty(global.getCarrierID()) && !global.getCarrierID().equals(this.carrierID)) {
			carrierID = global.getCarrierID();
			global.setCarrierID("");
			setTagId(carrierID);
		}
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
		// rack name
		if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_INIT)) {
			if (input.contains(".")) {
				String[] temp = input.split("\\.");
				global.setRackName(temp[0]);
				global.setSlotName(temp[1]);
				alotNumberTextView.setText(global.getRackName() + ":" + global.getSlotName());
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_IN)) {
			if (input.contains(".")) {
				String[] temp = input.split("\\.");
				global.setRackName(temp[0]);
				global.setSlotName(temp[1]);
				alotNumberTextView.setText(global.getRackName() + ":" + global.getSlotName());
			} else if (qTask == null && !CommonUtility.isEmpty(global.getRackName()) && !CommonUtility.isEmpty(global.getSlotName()) && !CommonUtility.isEmpty(input)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("putLotOnRack", input);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_OUT)) {
			if (qTask == null && !CommonUtility.isEmpty(input) && !input.contains(".")) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("takeLotOffRack", input);
			}
		}
	}

	@Override
	public void setTagId(String carrierID) {
		// mag carrier or rack carrier
//		super.setTagId(tagId);
		if (null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("checkCarrier", carrierID);
		}
	}

	public void startScanBarcode() {
		if (qTask == null) {
			tagBarcodeInput.setText("");
			qTask = new QueryTask();
			qTask.execute("scanBarcode");
		}
	}
}