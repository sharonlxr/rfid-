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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.entity.DataCollection;
import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.Carrier;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class CassetteAssignActivity extends BaseActivity {

	private QueryTask qTask = null;
	private String tagId = "";
	private String waferLotNumber = "";
	private String devcNumber = "";
	private String stepName = "";
	private String actualWaferLot = "";
	private List<Carrier> carrierList = new ArrayList<Carrier>();
	private LinearLayout carrierListView;
	private TextView waferLotNumberView;
	private TextView devcNumberView;
	private TextView stepNameView;
	private TextView actualWaferLotView;
	private Button submitButton;
	private Button clearButton;
	private Button exitButton;
	private final String classname = "CassetteAssign";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cassette_assign);
		mFormView = findViewById(R.id.cassette_assign_form);
		mStatusView = findViewById(R.id.cassette_assign_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		showProgress(false);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.cassette_assign_tb_fragment);
		super.initTagBarcodeInput();
		carrierListView = (LinearLayout) findViewById(R.id.cassette_assign_carrier_list);
		waferLotNumberView = (TextView) findViewById(R.id.cassette_assign_wafer_lot_number);
		devcNumberView = (TextView) findViewById(R.id.cassette_assign_devc_number);
		stepNameView = (TextView) findViewById(R.id.cassette_assign_step_name);
		actualWaferLotView = (TextView) findViewById(R.id.cassette_assign_actual_wafer_lot);
		submitButton = (Button) findViewById(R.id.cassette_assign_submit);
		clearButton = (Button) findViewById(R.id.cassette_assign_clear);
		exitButton = (Button) findViewById(R.id.cassette_assign_exit);
		submitButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (null == qTask && !CommonUtility.isEmpty(waferLotNumber) && !carrierList.isEmpty()) {
					StringBuilder carrierIdList = new StringBuilder();
					for (Carrier c : carrierList) {
						carrierIdList.append(",'").append(c.getTagID()).append("'");
					}
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("assign", waferLotNumber, carrierIdList.toString().substring(1));
				}
			}
		});
		clearButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				clear();
			}
		});
		exitButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";
		String carrierLotType = "";
		String lotList = "";
		String mCarrierId = "";
		String mCarrierName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			BaseException exception = null;
			cmdName = params[0];
			try {
				if (cmdName.equals("getWaferLotInfo")) {
					String waferLotNumber = params[1];
					getWaferLotInfo(waferLotNumber);
				} else if (cmdName.equals("checkCarrierId")) {
					mCarrierId = params[1];
					checkCarrierId(mCarrierId);
				} else if (cmdName.equals("deassign")) {
					mCarrierId = params[1];
					mCarrierName = params[2];
					lotList = params[3];
					String type = params[4];
					deassign(mCarrierId, mCarrierName, lotList, type);
				} else if (cmdName.equals("assign")) {
					String mWaferLotNumber = params[1];
					String mCarrierIdList = params[2];
					String api = "putWaferLotIntoCarriers(transUserId= '" + global.getUser().getUserID() + "',carrierIdList=[" + mCarrierIdList + "],waferLotNumber='" + mWaferLotNumber + "')";
					apiExecutorUpdate.query(classname, "assign", api);
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
			if (cmdName.equals("getWaferLotInfo")) {
				getWaferLotInfoAfter();
			} else if (cmdName.equals("checkCarrierId")) {
				checkCarrierIdAfter();
			} else if (cmdName.equals("deassign")) {
				deassignAfter();
			} else if (cmdName.equals("assign")) {
				toastMsg("assign成功");
				clear();
			}
			if (e == null) {
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(CassetteAssignActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(CassetteAssignActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void deassign(String carrierId, String carrierName, String lotList, String type) throws BaseException {
			if (type.equals("MagazineAO")) {
				String api = "removeLotFromCarriers(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + lotList + "',carrierIdList=['" + carrierId + "'], logCarrierHist='Y')";
				apiExecutorUpdate.query(classname, "deassign", api);
			} else if (type.equals("CassetteAO")) {
				String api = "removeBatchLotsFromCarriers(transUserId='" + global.getUser().getUserID() + "',carrierIdList=['" + carrierId + "'], logCarrierHist='Y')";
				apiExecutorUpdate.query(classname, "deassign", api);
			} else if (type.equals("CarrierWafer")) {
				String api = "removeWaferLotFromCarriers(transUserId='" + global.getUser().getUserID() + "',waferLotNumber='" + lotList + "',carrierIdList=['" + carrierId + "'])";
				apiExecutorUpdate.query(classname, "deassign", api);
			} else if (type.equals("CarrierPP")) {
				String api = "removeDeviceFromCarriers(transUserId='" + global.getUser().getUserID() + "',devcNumber='" + lotList + "',carrierIdList=['" + carrierId + "'])";
				apiExecutorUpdate.query(classname, "deassign", api);
			}
		}
		
		private void deassignAfter() {
			refreshCarrierListView(mCarrierId, mCarrierName, true);
		}
		
		private void checkCarrierId(String carrierId) throws BaseException {
			String api = "getCarrierAttributes(attributes='carrierId,carrierName,status,lotNumber,cassetteLotNumber,waferLotNumber,devcNumber',carrierId='" + carrierId + "')";
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
				showDeassignDialog(mCarrierId, mCarrierName, lotList, carrierLotType);
			} else {
				refreshCarrierListView(mCarrierId, mCarrierName, true);
			}
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
			CassetteAssignActivity.this.waferLotNumber = waferLotNumber;
			devcNumber = dc.get(0).get(0);
			actualWaferLot = dc.get(0).get(2);
			stepName = dc2.get(0).get(0);
		}
		
		private void getWaferLotInfoAfter() {
			waferLotNumberView.setText(waferLotNumber);
			devcNumberView.setText(devcNumber);
			stepNameView.setText(stepName);
			actualWaferLotView.setText(actualWaferLot);
		}

	}
	
	@Override
	public void setTagId(String tagId) {
		this.tagId = tagId;
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
			if (input.startsWith("10T")) {
				input = input.substring(3);
			}
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("getWaferLotInfo", input);
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		String carrierId = intent.getStringExtra("carrierID");
		if (!CommonUtility.isEmpty(carrierId) && !carrierId.equals(this.tagId)) {
			setTagId(carrierId);
		}
		super.onNewIntent(intent);
	}
	
	private void clear() {
		this.waferLotNumber = "";
		this.devcNumber = "";
		this.stepName = "";
		this.actualWaferLot = "";
		waferLotNumberView.setText(waferLotNumber);
		devcNumberView.setText(devcNumber);
		stepNameView.setText(stepName);
		actualWaferLotView.setText(actualWaferLot);
		carrierListView.removeAllViews();
		carrierList = new ArrayList<Carrier>();
	}
	
	private void showDeassignDialog(final String carrierId, final String carrierName, final String lotList, final String type) {
		AlertDialog.Builder builder = new AlertDialog.Builder(CassetteAssignActivity.this);
		builder.setTitle(getResources().getString(R.string.title_message)).setIcon(android.R.drawable.ic_dialog_info)
				.setMessage("Carrier " + carrierName + " 已绑定到 lot/Device：" + lotList + "，是否解绑？");
		builder.setPositiveButton(getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (null == qTask) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("deassign", carrierId, carrierName, lotList, type);
				}
			}
		});
		builder.setNegativeButton(getResources().getString(R.string.cancel), null);
		builder.show();
	}
	
	@SuppressLint("InflateParams")
	private void refreshCarrierListView(final String carrierId, final String carrierName, boolean isAdd) {
		carrierListView.removeAllViews();
		if (isAdd) {
			carrierList.add(new Carrier(carrierId, carrierName));
		} else {
			List<Carrier> newCarrierList = new ArrayList<Carrier>();
			for (Carrier c : carrierList) {
				if (!c.getTagID().equals(carrierId)) {
					newCarrierList.add(c);
				}
			}
			carrierList = newCarrierList;
		}
		for (Carrier c : carrierList) {
			LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.cassette_assign_list_item, null);
			TextView tagNameTextView = (TextView) l.findViewById(R.id.cassette_assign_carrier_name);
			tagNameTextView.setText(c.getTagName());
			ImageView removeButton = (ImageView) l.findViewById(R.id.cassette_assign_item_remove);
			removeButton.setOnClickListener(new RemoveButtonOnClickListener(c.getTagID()));
			carrierListView.addView(l);
		}
	}
	
	private class RemoveButtonOnClickListener implements OnClickListener {
		String tagId;
		RemoveButtonOnClickListener(String tagId) {
			this.tagId = tagId;
		}
		@Override
		public void onClick(View arg0) {
			refreshCarrierListView(this.tagId, "", false);
		}
	}
	
}
