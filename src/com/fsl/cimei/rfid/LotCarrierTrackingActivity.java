package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.Carrier;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class LotCarrierTrackingActivity extends BaseActivity {

	private QueryTask qTask = null;
	// private String tagId = "";
	private String waferLotNumber = "";
	private String devcNumber = "";
	private String stepName = "";
	private String actualWaferLot = "";
	private List<Carrier> carrierList = new ArrayList<Carrier>();
	private List<AoLot> aoLotList = new ArrayList<AoLot>();
	private LinearLayout listView;
	private Button submitButton;
	private Button clearButton;
	private Button exitButton;
	private final String classname = "CassetteAssign";
	private final int TYPE_AO_LOT_ASSIGN = 0;
	private final int TYPE_WAFER_LOT_ASSIGN = 1;
	private int functionType = 0;
	private RadioGroup funcRadioGroup;
	private RadioButton funcAolotRadio;
	private RadioButton funcWaferLotRadio;
	private CheckBox defectCheckBox;
	private LinearLayout defectLine;
	private LinearLayout seperateLine;
	private LinearLayout carrierListTitleLine;
	private LinearLayout aoLotSummaryLine;
	private TextView totalLotsView;
	private TextView totalQtyView;
	private TextView carrierListTitleDefect;
	private LinearLayout aoLotTitleLine;
	private LinearLayout waferLotLine;
	private TextView waferLotView;
	private LinearLayout devcNumberLine;
	private TextView devcNumberView;
	private LinearLayout stepNameLine;
	private TextView stepNameView;
	private LinearLayout actualWaferLotLine;
	private TextView actualWaferLotView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_carrier_tracking);
		mFormView = findViewById(R.id.lot_carrier_tracking_form);
		mStatusView = findViewById(R.id.lot_carrier_tracking_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		showProgress(false);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.lot_carrier_tracking_tb_fragment);
		super.initTagBarcodeInput();
		listView = (LinearLayout) findViewById(R.id.lot_carrier_tracking_list);
		submitButton = (Button) findViewById(R.id.lot_carrier_tracking_submit);
		clearButton = (Button) findViewById(R.id.lot_carrier_tracking_clear);
		exitButton = (Button) findViewById(R.id.lot_carrier_tracking_exit);
		submitButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null == qTask && !carrierList.isEmpty()) {
					if (functionType == TYPE_WAFER_LOT_ASSIGN && !waferLotNumber.isEmpty()) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("assign");
					} else if (functionType == TYPE_AO_LOT_ASSIGN && !aoLotList.isEmpty()) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("assign");
					}
				}
			}
		});
		clearButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clear();
			}
		});
		exitButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		defectCheckBox = (CheckBox) findViewById(R.id.lot_carrier_tracking_defect);
		defectLine = (LinearLayout) findViewById(R.id.lot_carrier_tracking_defect_line);
		funcRadioGroup = (RadioGroup) this.findViewById(R.id.lot_carrier_tracking_radio);
		funcAolotRadio = (RadioButton) this.findViewById(R.id.lot_carrier_tracking_radio_aolot);
		funcWaferLotRadio = (RadioButton) this.findViewById(R.id.lot_carrier_tracking_radio_waferlot);
		funcRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup rg, int checkedId) {
				if (checkedId == funcAolotRadio.getId()) {
					functionType = TYPE_AO_LOT_ASSIGN;
					defectLine.setVisibility(View.VISIBLE);
					if (!carrierList.isEmpty()) {
						listView.removeViews(6, carrierList.size());
					}
					listView.removeViews(0, 4);
					showAoLotLayout();
				} else if (checkedId == funcWaferLotRadio.getId()) {
					functionType = TYPE_WAFER_LOT_ASSIGN;
					defectLine.setVisibility(View.GONE);
					if (!carrierList.isEmpty()) {
						listView.removeViews(carrierList.size() + 4, carrierList.size());
					}
					listView.removeViews(0, carrierList.size() + 2);
					showWaferLotLayout();
				}
			}
		});
		initAoLotLayout();
		initWaferLotLayout();
		initCarrierListTitleLayout();
		showAoLotLayout();
		listView.addView(seperateLine, 2);
		listView.addView(carrierListTitleLine, 3);
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";
		String carrierLotType = "";
		String lotList = "";
		String mCarrierId = "";
		String mCarrierName = "";
		String isDefect = "";
		String alotNumber = "";
		String lotCarrierType = "";
		String carrierIdList = "";
		String carrierIdNameList = "";
		String lotQty = "";

		@Override
		protected BaseException doInBackground(String... params) {
			BaseException exception = null;
			cmdName = params[0];
			try {
				if (cmdName.equals("getWaferLotInfo")) {
					String waferLotNumber = params[1];
					getWaferLotInfo(waferLotNumber);
				} else if (cmdName.equals("getAoLotInfo")) {
					String aoLotNumber = params[1];
					getAoLotInfo(aoLotNumber);
				} else if (cmdName.equals("checkCarrierId")) {
					mCarrierId = params[1];
					checkCarrierId(mCarrierId);
				} else if (cmdName.equals("carrierDeassign")) {
					mCarrierId = params[1];
					lotList = params[2];
					String type = params[3];
					mCarrierName = params[4];
					isDefect = params[5];
					carrierDeassign(mCarrierId, lotList, type);
				} else if (cmdName.equals("lotDeassign")) {
					// lotNumber, carrierNameList, carrierIdList, type
					alotNumber = params[1];
					String carrierIdList = params[2];
					String type = params[3];
					lotQty = params[4];
					CommonTrans commonTrans = new CommonTrans();
					commonTrans.lotDeassign(apiExecutorUpdate, alotNumber, carrierIdList, type, global.getUser().getUserID());
				} else if (cmdName.equals("assign")) {
					if (functionType == TYPE_AO_LOT_ASSIGN) {
						StringBuilder lotList = new StringBuilder();
						for (AoLot a : aoLotList) {
							lotList.append(",'").append(a.lotNumber).append("'");
						}
						StringBuilder carrierIdList = new StringBuilder();
						for (Carrier c : carrierList) {
							carrierIdList.append(",('").append(c.getTagID()).append("','").append(c.isDefect() ? "Y" : "N").append("')");
						}
						String api = "putAOLotIntoCarriers(transUserId='" + global.getUser().getUserID() + "',carrierInforList=[" + carrierIdList.toString().substring(1) + "],lotNumberList=[" + lotList.toString().substring(1) + "],logCarrierHist= 'Y')";
						apiExecutorUpdate.query(classname, "assign", api);
					}else if (functionType == TYPE_WAFER_LOT_ASSIGN) {
						StringBuilder carrierIdList = new StringBuilder();
						for (Carrier c : carrierList) {
							carrierIdList.append(",'").append(c.getTagID()).append("'");
						}
						String api = "putWaferLotIntoCarriers(transUserId= '" + global.getUser().getUserID() + "',carrierIdList=[" + carrierIdList.toString().substring(1) + "],waferLotNumber='"
								+ waferLotNumber + "')";
						apiExecutorUpdate.query(classname, "assign", api);
					}
				}
			} catch (BaseException e) {
				exception = e;
			}
			return exception;
		}

		@Override
		protected void onPostExecute(BaseException e) {
			qTask = null;
			showProgress(false);
			tagBarcodeInput.setText("");
			tagBarcodeInput.requestFocus();
			if (e == null) {
				if (cmdName.equals("getWaferLotInfo")) {
					getWaferLotInfoAfter();
				} else if (cmdName.equals("getAoLotInfo")) {
					getAoLotInfoAfter();
				} else if (cmdName.equals("checkCarrierId")) {
					checkCarrierIdAfter();
				} else if (cmdName.equals("carrierDeassign")) {
					carrierDeassignAfter();
				} else if (cmdName.equals("lotDeassign")) {
					lotDeassignAfter();
				} else if (cmdName.equals("assign")) {
					toastMsg("成功绑定");
					clear();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotCarrierTrackingActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotCarrierTrackingActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void carrierDeassign(String carrierId, String lotList, String type) throws BaseException {
			if (type.equals("MagazineAO")) {
				String api = "removeLotFromCarriers(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + lotList + "',carrierIdList=['" + carrierId
						+ "'], logCarrierHist='Y')";
				apiExecutorUpdate.query(classname, "carrierDeassign", api);
			} else if (type.equals("CassetteAO")) {
				String api = "removeBatchLotsFromCarriers(transUserId='" + global.getUser().getUserID() + "',carrierIdList=['" + carrierId + "'], logCarrierHist='Y')";
				apiExecutorUpdate.query(classname, "carrierDeassign", api);
			} else if (type.equals("CarrierWafer")) {
				String api = "removeWaferLotFromCarriers(transUserId='" + global.getUser().getUserID() + "',waferLotNumber='" + lotList + "',carrierIdList=['"
						+ carrierId + "'])";
				apiExecutorUpdate.query(classname, "carrierDeassign", api);
			} else if (type.equals("CarrierPP")) {
				String api = "removeDeviceFromCarriers(transUserId='" + global.getUser().getUserID() + "',devcNumber='" + lotList + "',carrierIdList=['" + carrierId
						+ "'])";
				apiExecutorUpdate.query(classname, "carrierDeassign", api);
			}
		}

		private void carrierDeassignAfter() {
			refreshCarrierListView(mCarrierId, mCarrierName, (isDefect.equals("Y")), true);
		}

		private void checkCarrierId(String carrierId) throws BaseException {
			String api = "getCarrierAttributes(attributes='carrierId,carrierName,status,lotNumber,cassetteLotNumber,waferLotNumber,devcNumber',carrierId='" + carrierId
					+ "')";
			DataCollection dc = apiExecutorQuery.query(classname, "getWaferlotFromCarrier", api);
			if (CommonUtility.isEmpty(dc)) {
				throw new RfidException("该标签信息有误 " + carrierId, classname, "checkCarrierId", api);
			}
			mCarrierId = dc.get(0).get(0);
			mCarrierName = dc.get(0).get(1);
			if (dc.get(0).get(2).equalsIgnoreCase("OCCUPIED")) {
				for (ArrayList<String> temp : dc) {
					if (!temp.get(3).equalsIgnoreCase("none")) { // lotNumber
						if (lotList.isEmpty()) {
							lotList = temp.get(3);
						} else {
							lotList = lotList + "," + temp.get(3);
						}
						carrierLotType = "MagazineAO";
					}
					if (!temp.get(4).equalsIgnoreCase("none")) { // cassetteLotNumber
						if (lotList.isEmpty()) {
							lotList = temp.get(4);
						} else {
							lotList = lotList + "," + temp.get(4);
						}
						carrierLotType = "CassetteAO";
					}
					if (!temp.get(5).equalsIgnoreCase("none")) { // waferLotNumber
						if (lotList.isEmpty()) {
							lotList = temp.get(5);
						} else {
							lotList = lotList + "," + temp.get(5);
						}
						carrierLotType = "CarrierWafer";
					}
					if (!temp.get(6).equalsIgnoreCase("none")) { // devcNumber
						if (lotList.isEmpty()) {
							lotList = temp.get(6);
						} else {
							lotList = lotList + "," + temp.get(6);
						}
						carrierLotType = "CarrierPP";
					}
				}
			} else {
				carrierLotType = "";
			}
		}

		private void checkCarrierIdAfter() {
			if (!CommonUtility.isEmpty(carrierLotType)) {
				showCarrierDeassignDialog(mCarrierId, mCarrierName, lotList, carrierLotType, defectCheckBox.isChecked());
			} else {
				refreshCarrierListView(mCarrierId, mCarrierName, defectCheckBox.isChecked(), true);
			}
			defectCheckBox.setChecked(false);
		}

		private void getWaferLotInfo(String waferLotNumber) throws BaseException {
			String api = "getWaferLotAttributes(attributes='devcNumber,lotStatus,actualWlotNumber',lotNumber='" + waferLotNumber + "')";
			DataCollection dc = apiExecutorQuery.query(classname, "getWaferLotInfo", api);
			if (CommonUtility.isEmpty(dc)) {
				throw new RfidException("Wafer Lot信息有误 " + waferLotNumber, classname, "getWaferLotInfo", api);
			}
			String status = dc.get(0).get(1);
			if (!status.equals("HO") && status.equals("FR") && status.equals("AC")) {
				throw new RfidException("Wafer Lot的状态不允许进行绑定操作 " + waferLotNumber, classname, "getWaferLotInfo", api);
			}
			api = "getWlotStepHist(attributes='stepName',lotNumber='" + waferLotNumber + "',currentFlag ='Y')";
			DataCollection dc2 = apiExecutorQuery.query(classname, "getWaferLotInfo", api);
			if (CommonUtility.isEmpty(dc2)) {
				throw new RfidException("Wafer Lot Step信息有误 " + waferLotNumber, classname, "getWaferLotInfo", api);
			}
			LotCarrierTrackingActivity.this.waferLotNumber = waferLotNumber;
			devcNumber = dc.get(0).get(0);
			actualWaferLot = dc.get(0).get(2);
			stepName = dc2.get(0).get(0);
		}

		private void getWaferLotInfoAfter() {
			waferLotView.setText(waferLotNumber);
			devcNumberView.setText(devcNumber);
			stepNameView.setText(stepName);
			actualWaferLotView.setText(actualWaferLot);
		}

		private void getAoLotInfo(String aoLotNumber) throws BaseException {
			String api = "getLotAttributes(attributes='lotStatus,lotNumber,lotQty',lotNumber='" + aoLotNumber + "')";
			DataCollection dc = apiExecutorQuery.query(classname, "getAoLotInfo", api);
			if (CommonUtility.isEmpty(dc)) {
				throw new RfidException("Lot " + aoLotNumber + "不存在", classname, "getAoLotInfo", api);
			}
			String aoStatus = dc.get(0).get(0);
			lotQty = dc.get(0).get(2);
			if (!aoStatus.equals("HO") && !aoStatus.equals("FR") && !aoStatus.equals("AC")) {
				throw new RfidException("Lot Status " + aoStatus + " 不能进行绑定操作", classname, "getAoLotInfo", api);
			}
			alotNumber = aoLotNumber;

			CommonTrans commonTrans = new CommonTrans();
			String[] result = commonTrans.getAoLotAssignedCarrier(apiExecutorQuery, aoLotNumber);
			if (result[2].isEmpty()) {
			} else {
				lotCarrierType = result[0];
				carrierIdList = result[1];
				carrierIdNameList = result[2];
			}
		}

		private void getAoLotInfoAfter() {
			if (!CommonUtility.isEmpty(lotCarrierType)) {
				showLotDeassignDialog(alotNumber, carrierIdNameList, carrierIdList, lotCarrierType, lotQty);
			} else {
				lotDeassignAfter();
			}
		}
		
		private void lotDeassignAfter() {
			int lotQtyValue = 0;
			try {
				lotQtyValue = Integer.parseInt(lotQty);
			} catch (NumberFormatException e) {
			}
			refreshAoLotListView(alotNumber, lotQtyValue, true);
			getAOLotsFromSameWlotRFIDAssign(alotNumber);
		}
	
		private void getAOLotsFromSameWlotRFIDAssign(String aoLotNumber) {
			Intent i = new Intent(LotCarrierTrackingActivity.this, LotCarrierTrackingDeassignActivity.class);
			i.putExtra("lotNumber", aoLotNumber);
			startActivityForResult(i, 11);
		}
	}

	@Override
	public void setTagId(String tagId) {
		// this.tagId = tagId;
		boolean found = false;
		for (Carrier c : carrierList) {
			if (c.getTagID().equals(tagId)) {
				found = true;
			}
		}
		if (!found && null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("checkCarrierId", tagId);
		}
	}

	@Override
	public void setBarcodeInput(String input) {
		if (null == qTask) { // input as wafer lot number
			if (functionType == TYPE_AO_LOT_ASSIGN) {
				boolean found = false;
				for (AoLot aoLot : aoLotList) {
					if (aoLot.lotNumber.equalsIgnoreCase(input)) {
						found = true;
					}
				}
				if (!found) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("getAoLotInfo", input);
				}
			} else if (functionType == TYPE_WAFER_LOT_ASSIGN) {
				if (input.startsWith("10T")) {
					input = input.substring(3);
				}
				if (!waferLotNumber.equalsIgnoreCase(input)) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("getWaferLotInfo", input);
				}
			}
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		String carrierId = intent.getStringExtra("carrierID");
		if (!CommonUtility.isEmpty(carrierId)) { //  && !carrierId.equals(this.tagId)
			setTagId(carrierId);
		}
		super.onNewIntent(intent);
	}

	private void clear() {
		this.waferLotNumber = "";
		this.devcNumber = "";
		this.stepName = "";
		this.actualWaferLot = "";
		if (functionType == TYPE_AO_LOT_ASSIGN) {
			defectLine.setVisibility(View.VISIBLE);
			if (!carrierList.isEmpty()) {
				listView.removeViews(1 + aoLotList.size() + 1 + 2, carrierList.size());
			}
			if (!aoLotList.isEmpty()) {
				listView.removeViews(1, aoLotList.size());
			}
			totalLotsView.setText("Total Lots: ");
			totalQtyView.setText("Total Qty: ");
		} else if (functionType == TYPE_WAFER_LOT_ASSIGN) {
			defectLine.setVisibility(View.GONE);
			if (!carrierList.isEmpty()) {
				listView.removeViews(6, carrierList.size());
			}
			waferLotView.setText("");
			devcNumberView.setText("");
			stepNameView.setText("");
			actualWaferLotView.setText("");
		}
		defectCheckBox.setChecked(false);
		aoLotList = new ArrayList<AoLot>();
		carrierList = new ArrayList<Carrier>();
	}

	private void showCarrierDeassignDialog(final String carrierId, final String carrierName, final String lotList, final String type, final boolean isDefect) {
		AlertDialog.Builder builder = new AlertDialog.Builder(LotCarrierTrackingActivity.this);
		builder.setTitle(getResources().getString(R.string.title_message)).setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("Carrier " + carrierName + " 已绑定到 lot/Device：" + lotList + "，是否解绑？");
		builder.setPositiveButton(getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (null == qTask) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("carrierDeassign", carrierId, lotList, type, carrierName, isDefect ? "Y" : "N");
				}
			}
		});
		builder.setNegativeButton(getResources().getString(R.string.cancel), null);
		builder.show();
	}

	private void showLotDeassignDialog(final String lotNumber, final String carrierNameList, final String carrierIdList, final String type, final String lotQty) {
		AlertDialog.Builder builder = new AlertDialog.Builder(LotCarrierTrackingActivity.this);
		builder.setTitle(getResources().getString(R.string.title_message)).setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("Carrier " + carrierNameList + " 已绑定到 lot " + lotNumber + "，是否解绑？");
		builder.setPositiveButton(getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (null == qTask) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("lotDeassign", lotNumber, carrierIdList, type, lotQty);
				}
			}
		});
		builder.setNegativeButton(getResources().getString(R.string.cancel), null);
		builder.show();
	}

	@SuppressLint("InflateParams")
	private void refreshCarrierListView(final String carrierId, final String carrierName, final boolean isDefect, boolean isAdd) {
		if (isAdd) {
			carrierList.add(new Carrier(carrierId, carrierName, isDefect));
			LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.lot_carrier_tracking_list_item, null);
			TextView name = (TextView) l.findViewById(R.id.lot_carrier_tracking_name);
			name.setText(carrierName);
			TextView value = (TextView) l.findViewById(R.id.lot_carrier_tracking_value);
			value.setVisibility(functionType == TYPE_AO_LOT_ASSIGN ? View.VISIBLE : View.GONE);
			value.setText(isDefect ? "Y" : "N");
			ImageView removeButton = (ImageView) l.findViewById(R.id.lot_carrier_tracking_item_remove);
			removeButton.setOnClickListener(new CarrierRemoveButtonOnClickListener(carrierId));
			listView.addView(l);
		} else {
			int carrierIndex = -1;
			for (int i = 0; i < carrierList.size(); i++) {
				if (carrierList.get(i).getTagID().equals(carrierId)) {
					carrierIndex = i;
				}
			}
			if (carrierIndex != -1) {
				carrierList.remove(carrierIndex);
				if (functionType == TYPE_AO_LOT_ASSIGN) {
					carrierIndex += aoLotList.size() + 4;
				} else if (functionType == TYPE_WAFER_LOT_ASSIGN) {
					carrierIndex += 6;
				}
				listView.removeViewAt(carrierIndex);
			}
		}
	}

	@SuppressLint("InflateParams")
	private void refreshAoLotListView(final String lotNumber, final int lotQty, boolean isAdd) {
		if (isAdd) {
			boolean found = false;
			for (AoLot aoLot : aoLotList) {
				if (aoLot.lotNumber.equalsIgnoreCase(lotNumber)) {
					found = true;
				}
			}
			if (!found) {
				aoLotList.add(new AoLot(lotNumber, lotQty));
				LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.lot_carrier_tracking_list_item, null);
				TextView name = (TextView) l.findViewById(R.id.lot_carrier_tracking_name);
				name.setText(lotNumber);
				TextView value = (TextView) l.findViewById(R.id.lot_carrier_tracking_value);
				value.setVisibility(functionType == TYPE_AO_LOT_ASSIGN ? View.VISIBLE : View.GONE);
				value.setText("" + lotQty);
				ImageView removeButton = (ImageView) l.findViewById(R.id.lot_carrier_tracking_item_remove);
				removeButton.setOnClickListener(new AoLotRemoveButtonOnClickListener(lotNumber));
				listView.addView(l, aoLotList.size());
				int total = 0;
				for (AoLot alot : aoLotList) {
					total += alot.qty;
				}
				totalLotsView.setText("Total Lots: " + aoLotList.size());
				totalQtyView.setText("Total Qty: " + total);
			}
		} else {
			int aoLotIndex = -1;
			for (int i = 0; i < aoLotList.size(); i++) {
				if (aoLotList.get(i).lotNumber.equals(lotNumber)) {
					aoLotIndex = i;
				}
			}
			if (aoLotIndex != -1) {
				aoLotList.remove(aoLotIndex);
				if (functionType == TYPE_AO_LOT_ASSIGN) {
					aoLotIndex += 1;
					listView.removeViewAt(aoLotIndex);
				}
				int total = 0;
				for (AoLot alot : aoLotList) {
					total += alot.qty;
				}
				totalLotsView.setText("Total Lots: " + aoLotList.size());
				totalQtyView.setText("Total Qty: " + total);
			}
		}
	}

	private class CarrierRemoveButtonOnClickListener implements OnClickListener {
		String carrierId;

		CarrierRemoveButtonOnClickListener(String carrierId) {
			this.carrierId = carrierId;
		}

		@Override
		public void onClick(View arg0) {
			refreshCarrierListView(carrierId, "", false, false);
		}
	}

	private class AoLotRemoveButtonOnClickListener implements OnClickListener {
		String lotNumber;

		AoLotRemoveButtonOnClickListener(String lotNumber) {
			this.lotNumber = lotNumber;
		}

		@Override
		public void onClick(View arg0) {
			refreshAoLotListView(lotNumber, 0, false);
		}
	}

	public class AoLot {
		String lotNumber = "";
		int qty = 0;
		public AoLot(){}
		AoLot(String lotNumber, int qty) {
			this.lotNumber = lotNumber;
			this.qty = qty;
		}
	}

	@SuppressLint("InflateParams")
	private void initWaferLotLayout() {
		waferLotLine = (LinearLayout) getLayoutInflater().inflate(R.layout.lot_carrier_tracking_list_item, null);
		TextView name1 = (TextView) waferLotLine.findViewById(R.id.lot_carrier_tracking_name);
		name1.setText(getResources().getString(R.string.wafer_lot_number));
		waferLotView = (TextView) waferLotLine.findViewById(R.id.lot_carrier_tracking_value);
		waferLotView.setText(waferLotNumber);
		ImageView remove1 = (ImageView) waferLotLine.findViewById(R.id.lot_carrier_tracking_item_remove);
		remove1.setVisibility(View.GONE);

		devcNumberLine = (LinearLayout) getLayoutInflater().inflate(R.layout.lot_carrier_tracking_list_item, null);
		TextView name2 = (TextView) devcNumberLine.findViewById(R.id.lot_carrier_tracking_name);
		name2.setText(getResources().getString(R.string.device_number));
		devcNumberView = (TextView) devcNumberLine.findViewById(R.id.lot_carrier_tracking_value);
		devcNumberView.setText(devcNumber);
		ImageView remove2 = (ImageView) devcNumberLine.findViewById(R.id.lot_carrier_tracking_item_remove);
		remove2.setVisibility(View.GONE);

		stepNameLine = (LinearLayout) getLayoutInflater().inflate(R.layout.lot_carrier_tracking_list_item, null);
		TextView name3 = (TextView) stepNameLine.findViewById(R.id.lot_carrier_tracking_name);
		name3.setText(getResources().getString(R.string.step_name));
		stepNameView = (TextView) stepNameLine.findViewById(R.id.lot_carrier_tracking_value);
		stepNameView.setText(stepName);
		ImageView remove3 = (ImageView) stepNameLine.findViewById(R.id.lot_carrier_tracking_item_remove);
		remove3.setVisibility(View.GONE);

		actualWaferLotLine = (LinearLayout) getLayoutInflater().inflate(R.layout.lot_carrier_tracking_list_item, null);
		TextView name4 = (TextView) actualWaferLotLine.findViewById(R.id.lot_carrier_tracking_name);
		name4.setText(getResources().getString(R.string.actual_wafer_lot));
		actualWaferLotView = (TextView) actualWaferLotLine.findViewById(R.id.lot_carrier_tracking_value);
		actualWaferLotView.setText(actualWaferLot);
		ImageView remove4 = (ImageView) actualWaferLotLine.findViewById(R.id.lot_carrier_tracking_item_remove);
		remove4.setVisibility(View.GONE);

	}

	@SuppressLint("InflateParams")
	private void initAoLotLayout() {
		aoLotTitleLine = (LinearLayout) getLayoutInflater().inflate(R.layout.lot_carrier_tracking_list_item, null);
		TextView name1 = (TextView) aoLotTitleLine.findViewById(R.id.lot_carrier_tracking_name);
		name1.setText(getResources().getString(R.string.lot_number));
		TextView value1 = (TextView) aoLotTitleLine.findViewById(R.id.lot_carrier_tracking_value);
		value1.setText(getResources().getString(R.string.quantity));
		ImageView remove1 = (ImageView) aoLotTitleLine.findViewById(R.id.lot_carrier_tracking_item_remove);
		remove1.setVisibility(View.INVISIBLE);

		aoLotSummaryLine = (LinearLayout) getLayoutInflater().inflate(R.layout.lot_carrier_tracking_list_item, null);
		totalLotsView = (TextView) aoLotSummaryLine.findViewById(R.id.lot_carrier_tracking_name);
		totalLotsView.setText("Total Lots: ");
		totalLotsView.setTextColor(getResources().getColor(R.color.bg_blue));
		totalQtyView = (TextView) aoLotSummaryLine.findViewById(R.id.lot_carrier_tracking_value);
		totalQtyView.setText("Total Qty: ");
		totalQtyView.setTextColor(getResources().getColor(R.color.bg_blue));
		ImageView remove2 = (ImageView) aoLotSummaryLine.findViewById(R.id.lot_carrier_tracking_item_remove);
		remove2.setVisibility(View.GONE);
	}

	@SuppressLint("InflateParams")
	private void initCarrierListTitleLayout() {
		seperateLine = (LinearLayout) getLayoutInflater().inflate(R.layout.lot_carrier_tracking_list_item, null);
		TextView name0 = (TextView) seperateLine.findViewById(R.id.lot_carrier_tracking_name);
		name0.setText(" ");
		TextView value0 = (TextView) seperateLine.findViewById(R.id.lot_carrier_tracking_value);
		value0.setText(" ");
		ImageView remove0 = (ImageView) seperateLine.findViewById(R.id.lot_carrier_tracking_item_remove);
		remove0.setVisibility(View.INVISIBLE);

		carrierListTitleLine = (LinearLayout) getLayoutInflater().inflate(R.layout.lot_carrier_tracking_list_item, null);
		TextView name1 = (TextView) carrierListTitleLine.findViewById(R.id.lot_carrier_tracking_name);
		name1.setText(getResources().getString(R.string.carrier_name));
		carrierListTitleDefect = (TextView) carrierListTitleLine.findViewById(R.id.lot_carrier_tracking_value);
		carrierListTitleDefect.setText(getResources().getString(R.string.defect_flag));
		ImageView remove1 = (ImageView) carrierListTitleLine.findViewById(R.id.lot_carrier_tracking_item_remove);
		remove1.setVisibility(View.INVISIBLE);

	}

	private void showAoLotLayout() {
		listView.addView(aoLotTitleLine, 0);
		listView.addView(aoLotSummaryLine, 1);
		carrierListTitleDefect.setVisibility(View.VISIBLE);
	}

	private void showWaferLotLayout() {
		listView.addView(waferLotLine, 0);
		listView.addView(devcNumberLine, 1);
		listView.addView(stepNameLine, 2);
		listView.addView(actualWaferLotLine, 3);
		carrierListTitleDefect.setVisibility(View.GONE);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null) {
			switch (resultCode) {
			case -1:
				String result = data.getStringExtra("SCAN_RESULT");
				if (!CommonUtility.isEmpty(result)) {
					tagBarcodeInput.setText(result.trim() + ";");
				}
				break;
			case 11:
				ArrayList<String> availableLotList = data.getStringArrayListExtra("availableLotList");
				for (String temp : availableLotList) {
					String[] arr = temp.split("\\|");
					try {
						refreshAoLotListView(arr[0], Integer.parseInt(arr[1]), true);
					} catch (NumberFormatException e) {
						toastMsg("lot数量有误：" + temp);
					}
				}
				break;
			}
		}
	}
}
