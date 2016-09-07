package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.AOLot;
import com.fsl.cimei.rfid.entity.Carrier;
import com.fsl.cimei.rfid.entity.Step;
import com.fsl.cimei.rfid.exception.ApiException;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class StripInspectionActivity extends BaseActivity {

	private QueryTask qTask;
	private LinearLayout assignedCarrierListView;
	private String alotNumber = "";
	private List<Carrier> assignedCarrierList = new ArrayList<Carrier>();
	private Button scanToAssignButton;
	private Button scanToDeassignButton;
	private Button scanToAssignInputButton;
	private Button scanToAssignOutputButton;
	private Button scanToDeassignInputButton;
	private Button scanToDeassignOutputButton;
	private EditText carrierNameInput;
	private Button submitButton;
	private Button submitInputButton;
	private Button submitOutputButton;
	private boolean canManualOper = false;
	private final String transType = "RFID_CARRIER_ASSIGNMENT";
	private final String siName = "NON-LEAD SI";
	private int stripNumber = 0;
	private final String classname = "SI";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_strip_inspection);
		setupActionBar();
		mFormView = findViewById(R.id.si_form);
		mStatusView = findViewById(R.id.si_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.si_tb_fragment);
		super.initTagBarcodeInput();

		assignedCarrierListView = (LinearLayout) findViewById(R.id.si_assigned_list);
		scanToAssignButton = (Button) findViewById(R.id.si_scan_assign_button);
		scanToDeassignButton = (Button) findViewById(R.id.si_scan_deassign_button);
		scanToAssignInputButton = (Button) findViewById(R.id.si_scan_input_assign_button);
		scanToAssignOutputButton = (Button) findViewById(R.id.si_scan_output_assign_button);
		scanToDeassignInputButton = (Button) findViewById(R.id.si_scan_input_deassign_button);
		scanToDeassignOutputButton = (Button) findViewById(R.id.si_scan_output_deassign_button);
		scanToAssignButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (scanToAssignButton.getText().equals(getResources().getString(R.string.button_done))) {
					// scanToAssignButton.setText(getResources().getString(R.string.button_assign));
					// // global.setAoLot(null);
					// global.setScanTarget(Constants.SCAN_TARGET_SI_INIT);
					// updateButtonVisibility();
					int totalLayer = 0;
					for (Carrier c : assignedCarrierList) {
						totalLayer += c.getLayer();
					}
					if (totalLayer >= stripNumber) {
						// formEndStepDialog();
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("endLot");
					} else {
						scanToAssignButton.setText(getResources().getString(R.string.button_assign));
						// global.setAoLot(null);
						global.setScanTarget(Constants.SCAN_TARGET_SI_INIT);
						updateButtonVisibility();
						showError(StripInspectionActivity.this, "弹夹数量不够，不能结料");
					}
				} else {
					scanToAssignButton.setText(getResources().getString(R.string.button_done));
					global.setAoLot(new AOLot(alotNumber));
					global.setScanTarget(Constants.SCAN_TARGET_SI_ASSIGN);
					updateButtonVisibility();
				}
			}
		});
		scanToAssignInputButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (scanToAssignInputButton.getText().equals(getResources().getString(R.string.button_done))) {
					scanToAssignInputButton.setText(getResources().getString(R.string.button_assign_input));
					// global.setAoLot(null);
					global.setScanTarget(Constants.SCAN_TARGET_SI_INIT);
					updateButtonVisibility();
				} else {
					scanToAssignInputButton.setText(getResources().getString(R.string.button_done));
					global.setAoLot(new AOLot(alotNumber));
					global.setScanTarget(Constants.SCAN_TARGET_ASSIGN_INPUT);
					updateButtonVisibility();
				}
			}
		});
		scanToAssignOutputButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (scanToAssignOutputButton.getText().equals(getResources().getString(R.string.button_done))) {
					scanToAssignOutputButton.setText(getResources().getString(R.string.button_assign_output));
					// global.setAoLot(null);
					global.setScanTarget(Constants.SCAN_TARGET_SI_INIT);
					updateButtonVisibility();
				} else {
					scanToAssignOutputButton.setText(getResources().getString(R.string.button_done));
					global.setAoLot(new AOLot(alotNumber));
					global.setScanTarget(Constants.SCAN_TARGET_ASSIGN_OUTPUT);
					updateButtonVisibility();
				}
			}
		});
		scanToDeassignButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (scanToDeassignButton.getText().equals(getResources().getString(R.string.button_done))) {
					scanToDeassignButton.setText(getResources().getString(R.string.button_deassign));
					// global.setAoLot(null);
					global.setScanTarget(Constants.SCAN_TARGET_SI_INIT);
					updateButtonVisibility();
				} else {
					scanToDeassignButton.setText(getResources().getString(R.string.button_done));
					global.setAoLot(new AOLot(alotNumber));
					global.setScanTarget(Constants.SCAN_TARGET_SI_DEASSIGN);
					updateButtonVisibility();
				}
			}
		});
		scanToDeassignInputButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (scanToDeassignInputButton.getText().equals(getResources().getString(R.string.button_done))) {
					scanToDeassignInputButton.setText(getResources().getString(R.string.button_deassign_input));
					// global.setAoLot(null);
					global.setScanTarget(Constants.SCAN_TARGET_SI_INIT);
					updateButtonVisibility();
				} else {
					scanToDeassignInputButton.setText(getResources().getString(R.string.button_done));
					global.setAoLot(new AOLot(alotNumber));
					global.setScanTarget(Constants.SCAN_TARGET_DEASSIGN_INPUT);
					updateButtonVisibility();
				}
			}
		});
		scanToDeassignOutputButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (scanToDeassignOutputButton.getText().equals(getResources().getString(R.string.button_done))) {
					scanToDeassignOutputButton.setText(getResources().getString(R.string.button_deassign_output));
					// global.setAoLot(null);
					global.setScanTarget(Constants.SCAN_TARGET_SI_INIT);
					updateButtonVisibility();
				} else {
					scanToDeassignOutputButton.setText(getResources().getString(R.string.button_done));
					global.setAoLot(new AOLot(alotNumber));
					global.setScanTarget(Constants.SCAN_TARGET_DEASSIGN_OUTPUT);
					updateButtonVisibility();
				}
			}
		});
		submitButton = (Button) findViewById(R.id.si_submit);
		submitInputButton = (Button) findViewById(R.id.si_input_submit);
		submitOutputButton = (Button) findViewById(R.id.si_output_submit);
		carrierNameInput = (EditText) findViewById(R.id.si_carrier_name_input);
		submitButton.setOnClickListener(new OnClickListener() {
			@SuppressLint("DefaultLocale")
			@Override
			public void onClick(View v) {
				String carrierName = carrierNameInput.getText().toString().toUpperCase();
				if (!CommonUtility.isEmpty(carrierName) && null == qTask) {
					qTask = new QueryTask();
					qTask.execute("assignByCarrierName", carrierName, "INTRANS", "");
				}
			}
		});
		submitInputButton.setOnClickListener(new OnClickListener() {
			@SuppressLint("DefaultLocale")
			@Override
			public void onClick(View v) {
				String carrierName = carrierNameInput.getText().toString().toUpperCase();
				if (!CommonUtility.isEmpty(carrierName) && null == qTask) {
					qTask = new QueryTask();
					qTask.execute("assignByCarrierName", carrierName, "IN", "INTRANS");
				}
			}
		});
		submitOutputButton.setOnClickListener(new OnClickListener() {
			@SuppressLint("DefaultLocale")
			@Override
			public void onClick(View v) {
				String carrierName = carrierNameInput.getText().toString().toUpperCase();
				if (!CommonUtility.isEmpty(carrierName) && null == qTask) {
					qTask = new QueryTask();
					qTask.execute("assignByCarrierName", carrierName, "OUT", "INTRANS");
				}
			}
		});
	}

	@Override
	protected void onResume() {
		submitButton.setVisibility(View.GONE);
		submitInputButton.setVisibility(View.GONE);
		submitOutputButton.setVisibility(View.GONE);
		carrierNameInput.setVisibility(View.GONE);
		updateButtonVisibility();
		// if (null != global.getAoLot()) {
		// setLotNumber(global.getAoLot().getAlotNumber());
		// }
		if (null != global.getAoLot() && qTask == null) {
			this.alotNumber = global.getAoLot().getAlotNumber();
			alotNumberTextView.setText(this.alotNumber);
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("loadAssignedCarriers", this.alotNumber);
		}
		super.onResume();
	}

	public class QueryTask extends AsyncTask<String, Void, BaseException> {
		DataCollection queryResult = null;
		String cmdName = "";
		String carrierName = "";
		String lot = "";
		String carrierId = "";

		@SuppressLint("DefaultLocale")
		@Override
		protected BaseException doInBackground(String... params) {
			BaseException exception = null;
			cmdName = params[0];
			try {
				if (cmdName.equals("loadAssignedCarriers")) {
					String mLotNumber = params[1];
					loadAssignedCarriers(mLotNumber);
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
				} else if (cmdName.equals("carrierDeassign")) {
					String mLotNumber = params[1];
					String mCarrierID = params[2];
					String port = params[3];
					String comments = params[4];
					carrierDeassign(mLotNumber, mCarrierID, port, comments);
					loadAssignedCarriers(mLotNumber);
				} else if (cmdName.equals("assignByCarrierName")) {
					String api = "getCarrierAttributes(carrierName='" + params[1].toUpperCase()
							+ "', attributes='status,location,receiptDate,lotNumber,carrierName,carrierType,carrierLayer,carrierGroupId,carrierId')";
					DataCollection carrierInfo = apiExecutorQuery.query(classname, "assignByCarrierName", api);
					if (CommonUtility.isEmpty(carrierInfo)) {
						throw new RfidException("该RFID标签[" + params[1].toUpperCase() + "]不存在。", classname, "assignByCarrierName", api);
					} else {
						String mCarrierID = carrierInfo.get(0).get(8);
						String port = params[2];
						String comments = params[3];
						CommonTrans commonTrans = new CommonTrans();
						commonTrans.putLotIntoCarriers(apiExecutorQuery, apiExecutorUpdate, global, alotNumber, mCarrierID, port, comments, "");
						loadAssignedCarriers(alotNumber);
					}
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
				} else if (cmdName.equals("endLot")) {
					CommonTrans commonTrans = new CommonTrans();
					DataCollection currentStepContext = commonTrans.getCurrentStepContext(apiExecutorQuery, global.getAoLot().getAlotNumber());
					Step currentStep = new Step();
					if (currentStepContext.size() > 0) {
						String ProcNameResult = currentStepContext.get(0).get(0).trim();
						String StepSeqResult = currentStepContext.get(0).get(1).trim();
						String stepNameSession = currentStepContext.get(0).get(2).trim();
						currentStep.setProcName(ProcNameResult);
						currentStep.setStepSeq(StepSeqResult);
						currentStep.setStepName(stepNameSession);
					} else {
						throw new RfidException("Lot [" + global.getAoLot().getAlotNumber() + "] 不在step", classname, "endLot", "getCurrentStepContext");
					}
					if (!currentStep.getStepName().equals(siName)) {
						throw new RfidException("Lot [" + global.getAoLot().getAlotNumber() + "] 不在Strip Inspection Step", classname, "endLot", "getCurrentStepContext");
					}
					String api = "endLotAtStep(transUserId='" + global.getUser().getUserID() + "', lotNumber='" + global.getAoLot().getAlotNumber() + "',stepName='"
							+ currentStep.getStepName() + "')";
					try {
						apiExecutorUpdate.transact("SI", "endLot", api);
					} catch (ApiException e) {
						logLotTrans(currentStep.getProcName(), currentStep.getStepName(), currentStep.getStepSeq(), e.getMessage());
						throw e;
					}
					logLotTrans(currentStep.getProcName(), currentStep.getStepName(), currentStep.getStepSeq(), "Lot Ended Successfully");
				} else if (cmdName.equals("carrierNameAssign")) {
					String alotNumber = params[1];
					carrierName = params[2];
					String port = params[3];
					String comments = params[4];
					CommonTrans commonTrans = new CommonTrans();
					String[] result = commonTrans.checkBarcodeInput(apiExecutorQuery, carrierName, global.getUser().getDepartment());
					String mCarrierID;
					if (result[0].equals("carrier")) {
						mCarrierID = result[2];
					} else {
						mCarrierID = checkCarrierIdByName(carrierName, alotNumber);
					}
					carrierAssign(alotNumber, mCarrierID, port, comments);
					loadAssignedCarriers(alotNumber);
				} else if (cmdName.equals("carrierNameDeassign")) {
					String alotNumber = params[1];
					carrierName = params[2];
					String port = params[3];
					String comments = params[4];
					CommonTrans commonTrans = new CommonTrans();
					String[] result = commonTrans.checkBarcodeInput(apiExecutorQuery, carrierName, global.getUser().getDepartment());
					String mCarrierID;
					if (result[0].equals("carrier")) {
						mCarrierID = result[2];
					} else {
						mCarrierID = checkCarrierIdByName(carrierName, alotNumber);
					}
					carrierDeassign(alotNumber, mCarrierID, port, comments);
					loadAssignedCarriers(alotNumber);
				} else if (cmdName.equals("deassignThenAssign")) {
					String oldLotNumber = params[1];
					String newLotNumber = params[2];
					String mCarrierID = params[3];
					String port = params[4];
					String comments = params[5];
					deassignThenAssign(mCarrierID, oldLotNumber, newLotNumber, port, comments);
					loadAssignedCarriers(alotNumber);
				} else if (cmdName.equals("checkBarcodeInput")) {
					CommonTrans commonTrans = new CommonTrans();
					String[] result = commonTrans.checkBarcodeInput(apiExecutorQuery, params[1], global.getUser().getDepartment());
					resultCode = result[0];
					lot = result[1];
					carrierId = result[2];
					carrierName = result[3];
					if (resultCode.isEmpty() || resultCode.equals("lot") || (resultCode.equals("carrier") && !lot.isEmpty())) {
						alotNumber = lot;
						global.setAoLot(new AOLot(lot));
						loadAssignedCarriers(lot);
					}
				}
			} catch (BaseException e) {
				exception = e;
			}
			return exception;
		}

		@SuppressLint("InflateParams")
		@Override
		protected void onPostExecute(BaseException exception) {
			qTask = null;
			showProgress(false);
			tagBarcodeInput.requestFocus();
			// int totalLayer = 0;
			if (cmdName.equals("endLot")) {
				if (exception == null) {
					String msg = global.getAoLot().getAlotNumber() + " 结料成功";
					clearUI();
					scanToAssignButton.setText(getResources().getString(R.string.button_assign));
					global.setScanTarget(Constants.SCAN_TARGET_SI_INIT);
					global.setAoLot(null);
					updateButtonVisibility();
					showMsg(StripInspectionActivity.this, msg);
					// Toast.makeText(StripInspectionActivity.this, msg, Toast.LENGTH_SHORT).show();
				}
			} else {
				alotNumberTextView.setText(alotNumber);
				assignedCarrierListView.removeAllViews();
				for (Carrier c : assignedCarrierList) {
					// totalLayer += c.getLayer();
					LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.carrier_assign_list_item, null);
					TextView tagNameTextView = (TextView) l.findViewById(R.id.carrier_assign_tag_name);
					tagNameTextView.setText(c.getTagName());
					Button deassignButton = (Button) l.findViewById(R.id.carrier_assign_button_deassign);
					Button deassignInputButton = (Button) l.findViewById(R.id.carrier_assign_button_deassign_input);
					Button deassignOutputButton = (Button) l.findViewById(R.id.carrier_assign_button_deassign_output);
					if (canManualOper) {
						deassignInputButton.setVisibility(View.GONE);
						deassignOutputButton.setVisibility(View.GONE);
						DeassignButtonOnClickListener listener = new DeassignButtonOnClickListener(c.getTagID(), "INTRANS", "");
						deassignButton.setOnClickListener(listener);
						submitButton.setVisibility(View.VISIBLE);
						carrierNameInput.setVisibility(View.VISIBLE);
					} else {
						deassignButton.setVisibility(View.GONE);
						deassignInputButton.setVisibility(View.GONE);
						deassignOutputButton.setVisibility(View.GONE);
						submitButton.setVisibility(View.GONE);
						submitInputButton.setVisibility(View.GONE);
						submitOutputButton.setVisibility(View.GONE);
						carrierNameInput.setVisibility(View.GONE);
					}
					assignedCarrierListView.addView(l);
				}
				updateButtonVisibility();
			}
			if (exception == null) {
				// if (totalLayer >= stripNumber) {
				// formEndStepDialog();
				// }
				if (cmdName.equals("getALotByTag")) {
				} else if (cmdName.equals("carrierAssign")) {
				} else if (cmdName.equals("carrierDeassign")) {
				} else if (cmdName.equals("assignByCarrierName")) {
					carrierNameInput.setText("");
				} else if (cmdName.equals("carrierNameDeassign")) {
				} else if (cmdName.equals("carrierNameAssign")) {
					if (resultCode.equals("deassignToAssign")) {
						final String assignButtonName = getResources().getString(R.string.button_assign);
						final String port = "INTRANS";
						final String comments = "";
						AlertDialog.Builder builder = new AlertDialog.Builder(StripInspectionActivity.this);
						builder.setTitle(getResources().getString(R.string.title_error));
						builder.setIcon(R.drawable.error);
						builder.setMessage("此RFID标签[" + carrierName + "]已对应[" + oldLotNumber + "]，需要与该lot解绑并绑定到新lot[" + newLotNumber + "]吗？");
						builder.setPositiveButton(assignButtonName, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (null == qTask) {
									showProgress(true);
									qTask = new QueryTask();
									qTask.execute("deassignThenAssign", oldLotNumber, newLotNumber, carrierID, port, comments);
								}
							}
						});
						builder.setNegativeButton(getResources().getString(R.string.button_cancel), null);
						builder.show();
					}
				} else if (cmdName.equals("deassignThenAssign")) {
				} else if (cmdName.equals("checkBarcodeInput")) {
					if (resultCode.equals("carrier") && lot.isEmpty()) {
						setTagId(carrierId);
					} else if (resultCode.equals("multiCarrier")) {
						showError(StripInspectionActivity.this, carrierName + " 对应多个carrier");
					}
				}
			} else {
				logf(exception.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(StripInspectionActivity.this, exception.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(StripInspectionActivity.this, exception.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
		}
		
		private void logLotTrans(String stepName, String procName, String stepSeq, String message) {
			String api = "insertLotProcEventHist(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + global.getAoLot().getAlotNumber() + "',stepName='"
					+ stepName + "',eventCode='RFID_TRANS',procName='" + procName
					+ "',comments='"+message+"', stepSeq=" + stepSeq + " , operatorId = '" + global.getUser().getUserID()
					+ "', hostname='" + global.getAndroidSecureID() + "')";
			logf(api);
			try {
				apiExecutorUpdate.transact(classname, "logLotTrans", api);
			} catch(ApiException e) {
				logf(e.toString());
			}
		}

		private String checkCarrierIdByName(String mCarrierName, String alotNumber) throws BaseException {
			String api = "getRFIDCarrierIDByCarrierName(carrierName='" + mCarrierName + "',lotNumber='" + alotNumber + "')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "checkCarrierIdByName", api);
			if (CommonUtility.isEmpty(queryResult)) {
				throw new RfidException("此弹夹不存在。", classname, "checkCarrierIdByName", api);
			} else {
				String carrierId = queryResult.get(0).get(0);
				return carrierId;
			}
		}

		String resultCode = "";
		String carrierID = "";
		String oldLotNumber = "";
		String newLotNumber = "";

		private void carrierAssign(String mALotNumber, String mCarrierID, String port, String comments) throws BaseException {
			String api = "getCarrierAttributes(carrierId='" + mCarrierID + "', attributes='status,lotNumber,carrierName,carrierType')";
			DataCollection carrierInfo = apiExecutorQuery.query(classname, "carrierAssign", api);
			if (CommonUtility.isEmpty(carrierInfo)) {
				throw new RfidException("此RFID标签不存在。", classname, "carrierAssign", api);
			} else if (carrierInfo.get(0).get(0).equals("GOOD") && carrierInfo.get(0).get(1).equals("None")) {
				CommonTrans commonTrans = new CommonTrans();
				commonTrans.putLotIntoCarriers(apiExecutorQuery, apiExecutorUpdate, global, mALotNumber, mCarrierID, port, comments, "");
			} else {
				if (!carrierInfo.get(0).get(1).equals(mALotNumber)) {
					resultCode = "deassignToAssign";
					carrierID = mCarrierID;
					carrierName = carrierInfo.get(0).get(2);
					// throw new RfidException("此RFID标签[" + carrierName + "]已被绑定。", classname, "carrierAssign", api);
					oldLotNumber = carrierInfo.get(0).get(1);
					newLotNumber = mALotNumber;
				}
			}
		}

		private void carrierDeassign(String mALotNumber, String mCarrierID, String port, String comments) throws BaseException {
			String api = "getCarrierAttributes(carrierId='" + mCarrierID + "', attributes='status,lotNumber,carrierName,carrierType')";
			DataCollection carrierInfo = apiExecutorQuery.query(classname, "carrierDeassign", api);
			if (CommonUtility.isEmpty(carrierInfo)) {
				throw new RfidException("此RFID标签不存在。", classname, "carrierDeassign", api);
			} else if (carrierInfo.get(0).get(0).equals("OCCUPIED") && carrierInfo.get(0).get(1).equals(mALotNumber)) {
				CommonTrans commonTrans = new CommonTrans();
				commonTrans.removeLotFromCarriers(apiExecutorQuery, apiExecutorUpdate, global, mALotNumber, mCarrierID, port, comments, "");
			} else {
				carrierName = carrierInfo.get(0).get(2);
				throw new RfidException("此RFID标签[" + carrierName + "]未被绑定到当前lot[" + mALotNumber + "]。", classname, "carrierDeassign", api);
			}
		}

		private void loadAssignedCarriers(String mLotNumber) throws BaseException {
			assignedCarrierList.clear();
			CommonTrans commonTrans = new CommonTrans();
			int level = commonTrans.checkUserPrivilege(apiExecutorQuery, global.getUser().getUserID(), transType);
			if (level == 2) {
				canManualOper = true;
			} else {
				canManualOper = false;
			}
			DataCollection currentStepContext = commonTrans.getCurrentStepContext(apiExecutorQuery, mLotNumber);
			AOLot aoLot = global.getAoLot();
			Step currentStep = new Step();
			if (currentStepContext.size() > 0) {
				String ProcNameResult = currentStepContext.get(0).get(0).trim();
				String StepSeqResult = currentStepContext.get(0).get(1).trim();
				String stepNameSession = currentStepContext.get(0).get(2).trim();
				currentStep.setProcName(ProcNameResult);
				currentStep.setStepSeq(StepSeqResult);
				currentStep.setStepName(stepNameSession);
				aoLot.setAlotNumber(mLotNumber);
				aoLot.setCurrentStep(currentStep);
			} else {
				throw new RfidException("Lot [" + mLotNumber + "] 不在step", classname, "loadAssignedCarriers", "getCurrentStepContext");
			}
			if (!currentStep.getStepName().equals(siName)) {
				throw new RfidException("Lot [" + mLotNumber + "] 不在Strip Inspection Step", classname, "loadAssignedCarriers", "getCurrentStepContext");
			}
			String api = "getCarrierAttributes(lotNumber='" + mLotNumber + "', attributes='carrierId,carrierName,carrierLayer')";
			queryResult = apiExecutorQuery.query(classname, "loadAssignedCarriers", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				for (ArrayList<String> temp : queryResult) {
					Carrier c = new Carrier();
					c.setTagID(temp.get(0));
					c.setTagName(temp.get(1));
					if (!CommonUtility.isEmpty(temp.get(2)) && CommonUtility.isValidNumber(temp.get(2))) {
						c.setLayer(Integer.parseInt(temp.get(2)));
					}
					assignedCarrierList.add(c);
				}
			}
			String stripNumberStr = commonTrans.getLatestStripNumber(apiExecutorQuery, mLotNumber);
			if (CommonUtility.isValidNumber(stripNumberStr)) {
				stripNumber = Integer.parseInt(stripNumberStr);
			} else {
				throw new RfidException("Lot [" + mLotNumber + "] 没有条数", classname, "loadAssignedCarriers", api);
			}
		}

		private void deassignThenAssign(String mCarrierID, String oldLotNumber, String newLotNumber, String port, String comments) throws BaseException {
			CommonTrans commonTrans = new CommonTrans();
			try {
				commonTrans.removeLotFromCarriers(apiExecutorQuery, apiExecutorUpdate, global, oldLotNumber, mCarrierID, "INTRANS", "", "");
			} catch (RfidException e) {
				logf(e.toString());
			}
			commonTrans.putLotIntoCarriers(apiExecutorQuery, apiExecutorUpdate, global, newLotNumber, mCarrierID, port, comments, "");
		}
	}

	private class DeassignButtonOnClickListener implements OnClickListener {
		String tagID;
		String port;
		String comments;

		DeassignButtonOnClickListener(String tagID, String port, String comments) {
			this.tagID = tagID;
			this.port = port;
			this.comments = comments;
		}

		@Override
		public void onClick(View arg0) {
			if (qTask == null && !CommonUtility.isEmpty(alotNumber) && !CommonUtility.isEmpty(tagID)) {
				qTask = new QueryTask();
				qTask.execute(new String[] { "carrierDeassign", alotNumber, tagID, port, comments });
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

	public void setBarcodeInput(String input) {
		if (null == qTask) {
			if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_INIT)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("checkBarcodeInput", input);
			} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_ASSIGN)) {
				if (qTask == null && null != global.getAoLot() && !CommonUtility.isEmpty(input)) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("carrierNameAssign", global.getAoLot().getAlotNumber(), input, "INTRANS", "");
				}
			} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_DEASSIGN)) {
				if (qTask == null && null != global.getAoLot() && !CommonUtility.isEmpty(input)) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("carrierNameDeassign", global.getAoLot().getAlotNumber(), input, "INTRANS", "");
				}
			}
		}
	}

	public void startScanBarcode() {
		if (qTask == null) {
			tagBarcodeInput.setText("");
			qTask = new QueryTask();
			qTask.execute("scanBarcode");
		}
	}

	private void updateButtonVisibility() {
		scanToAssignButton.setVisibility(View.GONE);
		scanToDeassignButton.setVisibility(View.GONE);
		scanToAssignInputButton.setVisibility(View.GONE);
		scanToAssignOutputButton.setVisibility(View.GONE);
		scanToDeassignInputButton.setVisibility(View.GONE);
		scanToDeassignOutputButton.setVisibility(View.GONE);
		if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_INIT)) {
			if (null == global.getAoLot()) {
				scanToAssignButton.setVisibility(View.INVISIBLE);
				scanToDeassignButton.setVisibility(View.INVISIBLE);
			} else {
				scanToAssignButton.setVisibility(View.VISIBLE);
				scanToDeassignButton.setVisibility(View.VISIBLE);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_ASSIGN)) {
			scanToAssignButton.setVisibility(View.VISIBLE);
			scanToDeassignButton.setVisibility(View.INVISIBLE);
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_DEASSIGN)) {
			scanToAssignButton.setVisibility(View.INVISIBLE);
			scanToDeassignButton.setVisibility(View.VISIBLE);
		}
	}

	// private void formEndStepDialog() {
	// AlertDialog.Builder builder = new AlertDialog.Builder(StripInspectionActivity.this);
	// builder.setTitle(getResources().getString(R.string.title_activity_strip_inspection));
	// builder.setMessage("是否结料？");
	// builder.setPositiveButton(getResources().getString(R.string.button_end), new DialogInterface.OnClickListener() {
	// @Override
	// public void onClick(DialogInterface dialog, int which) {
	// if (qTask == null) {
	// showProgress(true);
	// qTask = new QueryTask();
	// qTask.execute("endLot");
	// }
	// }
	// });
	// builder.setNegativeButton(getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
	// @Override
	// public void onClick(DialogInterface dialog, int which) {
	// }
	// });
	// builder.show();
	// }

	private void clearUI() {
		alotNumberTextView.setText("");
		assignedCarrierListView.removeAllViews();
		submitButton.setVisibility(View.GONE);
		submitInputButton.setVisibility(View.GONE);
		submitOutputButton.setVisibility(View.GONE);
		carrierNameInput.setVisibility(View.GONE);
	}

}
