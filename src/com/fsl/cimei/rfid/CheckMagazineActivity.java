package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class CheckMagazineActivity extends BaseActivity {
	private QueryTask qTask;
	private String alotNumber;
	private TextView magStaView;
	private Button checkFlagButton;
	private LinearLayout assignedCarrierListView;
	private List<Carrier> carriers = new ArrayList<Carrier>();
	// public final static String EVENT_CODE = "RFID_LotMagCheck";
	private final String classname = "CheckMag";

	// private TextView alotNumberTextView;
	// private EditText tagOrBarcodeInput;
	// private Button n7ScanBarcode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_check_magazine);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		mFormView = findViewById(R.id.check_mag_form);
		mStatusView = findViewById(R.id.check_mag_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.check_mag_tb_fragment);
		super.initTagBarcodeInput();
		// alotNumberTextView = (TextView) findViewById(R.id.check_mag_lot);
		checkFlagButton = (Button) findViewById(R.id.check_mag_check_flag);
		checkFlagButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (global.getScanTarget().equals(Constants.SCAN_TARGET_CHECK_MAGAZINE_INIT)) {
					global.setScanTarget(Constants.SCAN_TARGET_CHECK_MAGAZINE);
					checkFlagButton.setText("检查完成");
				} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_CHECK_MAGAZINE)) {
					if (qTask == null) {
						String checkResult;
						StringBuilder missing = new StringBuilder();
						for (Carrier c : carriers) {
							if (!c.isChecked) {
								missing.append(",").append(c.name);
							}
						}
						String missingStr = missing.toString();
						if (missingStr.isEmpty()) {
							checkResult = "Pass";
						} else {
							checkResult = "Missing " + missingStr.substring(1);
						}
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("logCheckResult", global.getAoLot().getAlotNumber(), checkResult);
					}
//					global.setScanTarget(Constants.SCAN_TARGET_CHECK_MAGAZINE_INIT);
//					global.setAoLot(null);
//					checkFlagButton.setVisibility(View.INVISIBLE);
//					assignedCarrierListView.removeAllViews();
//					magStaView.setText("");
//					alotNumberTextView.setText("");
				}
			}
		});
		assignedCarrierListView = (LinearLayout) findViewById(R.id.check_mag_list);
		magStaView = (TextView) findViewById(R.id.check_mag_sta);
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			BaseException exception = null;
			cmdName = params[0];
			try {
				if (cmdName.equals("loadAssignedCarriers")) {
					String mLotNumber = params[1];
					String mCarrierId;
					if (params.length == 3) {
						mCarrierId = params[2];
					} else {
						mCarrierId = "";
					}
					loadAssignedCarriers(mLotNumber, mCarrierId);
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
				} else if (cmdName.equals("logCheckResult")) {
					String alotNumber = params[1];
					String comment= params[2];
					logCheckResult(alotNumber, comment);
				}
			} catch (BaseException e) {
				exception = e;
			}
			return exception;
		}

		private void logCheckResult(String alotNumber, String comment) throws BaseException {
			CommonTrans commonTrans = new CommonTrans();
			DataCollection currentStepContext = commonTrans.getCurrentStepContext(apiExecutorQuery, alotNumber);
			if (CommonUtility.isEmpty(currentStepContext)) {
				throw new RfidException(alotNumber + "不在step", classname, "logCheckResult", "getCurrentStepContext " + alotNumber);
			}
			String procName = currentStepContext.get(0).get(0).trim();
			String stepSeq = currentStepContext.get(0).get(1).trim();
			String stepName = currentStepContext.get(0).get(2).trim();
			// ,machines = '" + machineIds + "'
			String api = "insertLotProcEventHist(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + alotNumber + "',stepName='" + stepName + "',eventCode='" + Constants.EVENT_CODE
					+ "',procName='" + procName + "',comments='" + comment + "', stepSeq =  " + stepSeq + " , operatorId = '"
					+ global.getUser().getUserID() + "', hostname='" + global.getAndroidSecureID() + "')";
			logf(api);
			apiExecutorUpdate.transact(classname, "logCheckResult", api);
		}

		@Override
		protected void onPostExecute(BaseException exception) {
			qTask = null;
			showProgress(false);
			tagBarcodeInput.requestFocus();
			if (exception == null) {
				if (cmdName.equals("loadAssignedCarriers")) {
					loadAssignedCarriersAfter();
				} else if (cmdName.equals("logCheckResult")) {
					logCheckResultAfter();
				}
			} else {
				logf(exception.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(CheckMagazineActivity.this, exception.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(CheckMagazineActivity.this, exception.toString());
				}
			}
		}

		private void logCheckResultAfter() {
			global.setScanTarget(Constants.SCAN_TARGET_CHECK_MAGAZINE_INIT);
			global.setAoLot(null);
			checkFlagButton.setVisibility(View.INVISIBLE);
			assignedCarrierListView.removeAllViews();
			magStaView.setText("");
			alotNumberTextView.setText("");
			toastMsg("检查完成");
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void loadAssignedCarriers(String mLotNumber, String mCarrierId) throws BaseException {
			carriers.clear();
			String api = "getCarrierAttributes(lotNumber='" + mLotNumber + "', attributes='carrierId,carrierName')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "loadAssignedCarriers", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				for (ArrayList<String> temp : queryResult) {
					Carrier c = new Carrier();
					c.id = temp.get(0);
					c.name = temp.get(1);
					c.isChecked = (c.id.equals(mCarrierId));
					carriers.add(c);
				}
			}
			alotNumber = mLotNumber;
		}

		@SuppressLint("InflateParams")
		private void loadAssignedCarriersAfter() {
			alotNumberTextView.setText(alotNumber);
			assignedCarrierListView.removeAllViews();
			for (Carrier c : carriers) {
				LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.check_mag_list_item, null);
				c.tagNameTextView = (TextView) l.findViewById(R.id.check_mag_item_name);
				c.tagNameTextView.setText(c.name);
				c.tagCheckedFlag = (ImageView) l.findViewById(R.id.check_mag_item_flag);
				if (c.isChecked) {
					c.tagCheckedFlag.setVisibility(View.VISIBLE);
				} else {
					c.tagCheckedFlag.setVisibility(View.INVISIBLE);
				}
				assignedCarrierListView.addView(l);
			}
			checkFlagButton.setText("开始检查");
			checkFlagButton.setVisibility(View.VISIBLE);
			magStaView.setText("共" + carriers.size() + "个弹夹");
		}

	}

	@Override
	protected void onResume() {
		if (global.getScanTarget().equals(Constants.SCAN_TARGET_CHECK_MAGAZINE_INIT)) {
			if (null != global.getAoLot()) {
				alotNumber = global.getAoLot().getAlotNumber();
				alotNumberTextView.setText(alotNumber);
				if (null == qTask) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("loadAssignedCarriers", alotNumber, global.getCarrierID());
				}
			} else {
				checkFlagButton.setVisibility(View.GONE);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_CHECK_MAGAZINE)) {
			checkNewTag(global.getCarrierID());
		}
		super.onResume();
	}

	private void checkNewTag(String mCarrierId) {
		boolean contained = false;
		for (Carrier c : carriers) {
			if (c.id.equals(mCarrierId)) {
				contained = true;
				c.tagCheckedFlag.setVisibility(View.VISIBLE);
				c.isChecked = true;
				break;
			}
		}
		if (!contained) {
			// showError(CheckMagazineActivity.this, "错误的弹夹");
			// Toast.makeText(CheckMagazineActivity.this, "错误的弹夹", Toast.LENGTH_SHORT).show();
			toastMsg("错误的弹夹");
		} else {
			boolean allChecked = true;
			for (Carrier c : carriers) {
				if (!c.isChecked) {
					allChecked = false;
					break;
				}
			}
			if (allChecked) {
//				showMsg(CheckMagazineActivity.this, "已扫描到所有的弹夹");
//				Toast.makeText(CheckMagazineActivity.this, "已扫描到所有的弹夹", Toast.LENGTH_SHORT).show();
				toastMsg("已扫描到所有的弹夹");
			}
		}
	}

	class Carrier {
		String id = "";
		String name = "";
		boolean isChecked = false;
		String lotNumber = "";
		TextView tagNameTextView = null;
		ImageView tagCheckedFlag = null;
	}

	public void setBarcodeInput(String alotNumber) {
		log("CheckMag setLotNumber " + alotNumber);
		this.alotNumber = alotNumber;
		if (null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("loadAssignedCarriers", alotNumber);
		}
	}

	public void startScanBarcode() {
		log("CheckMag startScanBarcode");
		if (qTask == null) {
			tagBarcodeInput.setText("");
			qTask = new QueryTask();
			qTask.execute("scanBarcode");
		}
	}

	@Override
	public void setTagId(String tagId) {
		log("CheckMag setTagId " + tagId);
		if (global.getScanTarget().equals(Constants.SCAN_TARGET_CHECK_MAGAZINE_INIT)) {
			Intent intent = new Intent(CheckMagazineActivity.this, NewNFCTagActivity.class);
			intent.putExtra("carrierID", tagId);
			startActivity(intent);
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_CHECK_MAGAZINE)) {
			checkNewTag(tagId);
		}
	}
}
