package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;

public class InOutCarrierAssignmentActivity extends BaseActivity {

	private QueryTask qTask;
	private EditText alotNumberInput;
	private Button alotSubmitButton;
	private LinearLayout groupLinear; // root input / output carrier groups view
	private List<InputCarrier> groups;

	private String alotNumber = "";
	private String selectedInput = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_in_out_carrier_assignment);
		mFormView = findViewById(R.id.in_out_carrier_form);
		mStatusView = findViewById(R.id.in_out_carrier_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		alotNumberInput = (EditText) findViewById(R.id.in_out_carrier_alot_number);
		alotNumberInputHandler = new MyBaseHandler(alotNumberInput);
		groupLinear = (LinearLayout) findViewById(R.id.in_out_carrier_group);
		groups = new ArrayList<InputCarrier>();
		alotNumberInput.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == 131 && qTask == null) {
					showProgress(true);
					groups.clear();
					groupLinear.removeAllViews();
					qTask = new QueryTask();
					qTask.execute("scanBarcode");
				}
				return false;
			}
		});
		alotSubmitButton = (Button) findViewById(R.id.in_out_carrier_alot_number_submit);
		alotSubmitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				alotNumber = alotNumberInput.getText().toString();
				if (qTask == null) {
					showProgress(true);
					groups.clear();
					groupLinear.removeAllViews();
					qTask = new QueryTask();
					qTask.execute("getInOutCarrierByLot");
				}
			}
		});
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";
		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
				} else if (cmdName.equals("getInOutCarrierByLot")) {
					getInOutCarrierByLot();
				} else if (cmdName.equals("getInOutCarrierByTag")) {
					getInOutCarrierByTag();
					if (!CommonUtility.isEmpty(alotNumber)) {
						getInOutCarrierByLot();
					}
				} else if (cmdName.equals("setInOutCarrierAssignment")) {
					setInOutCarrierAssignment();
					getInOutCarrierByLot();
				} else if (cmdName.equals("setInOutCarrierDeassignment")) {
					String inputId = params[1];
					String outputId = params[2];
					setInOutCarrierDeassignment(inputId, outputId);
					getInOutCarrierByLot();
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
			if (!CommonUtility.isEmpty(alotNumber)) {
				alotNumberInput.setText(alotNumber);
			}
			loadPageAfter();
			if (null == e) {
				if (cmdName.equals("getInOutCarrierByLot")) {
				} else if (cmdName.equals("setInOutCarrierAssignment")) {
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(InOutCarrierAssignmentActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(InOutCarrierAssignmentActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void getInOutCarrierByLot() throws BaseException {
			String api = "getInOutCarrierAssignment(attributes='inputCarrierId,inputCarrierName,outputCarrierId,outputCarrierName,status', lotNumber='" + alotNumber
					+ "')";
			DataCollection inOutCarriers = apiExecutorQuery.query("InOutCarrierAssign", "getInOutCarrierByLot", api);
			api = "getCarrierAttributes(lotNumber='" + alotNumber + "', attributes='carrierId,carrierName')";
			DataCollection allCarriers = apiExecutorQuery.query("InOutCarrierAssign", "getInOutCarrierByLot", api);
			Map<String, String> allCarriersMap = new HashMap<String, String>();
			if (!CommonUtility.isEmpty(allCarriers)) {
				for (ArrayList<String> temp : allCarriers) {
					allCarriersMap.put(temp.get(0), temp.get(1));
				}
			}
			if (!CommonUtility.isEmpty(inOutCarriers)) {
				for (ArrayList<String> temp : inOutCarriers) {
					InputCarrier input = null;
					for (InputCarrier temp1 : groups) {
						if (temp1.inputId.equals(temp.get(0))) {
							input = temp1;
							break;
						}
					}
					if (input == null) {
						input = new InputCarrier();
						input.inputId = temp.get(0);
						input.inputName = temp.get(1);
						input.outputs = new ArrayList<OutputCarrier>();
						groups.add(input);
						if (allCarriersMap.containsKey(temp.get(0))) {
							allCarriersMap.remove(temp.get(0));
						}
					}
					OutputCarrier o = new OutputCarrier();
					o.outputId = temp.get(2);
					o.outputName = temp.get(3);
					o.outputStatus = temp.get(4);
					input.outputs.add(o);
					if (allCarriersMap.containsKey(temp.get(2))) {
						allCarriersMap.remove(temp.get(2));
					}
				}
			}
			if (allCarriersMap.size() > 0) {
				for (String temp : allCarriersMap.keySet()) {
					InputCarrier input = new InputCarrier();
					input.inputId = temp;
					input.inputName = allCarriersMap.get(temp);
					input.outputs = new ArrayList<OutputCarrier>();
					groups.add(input);
				}
			}
		}
		
		private void getInOutCarrierByTag() throws BaseException {
			String api = "getCarrierAttributes(carrierId='" + global.getCarrierID() + "', attributes='status,location,receiptDate,lotNumber,carrierName,carrierType,carrierLayer,carrierGroupId,cassetteOrMagazine')";
			DataCollection queryResult = apiExecutorQuery.query("InOutCarrierAssign", "getInOutCarrierByTag", api);
			alotNumber = "";
			global.setCarrierID("");
			if (!CommonUtility.isEmpty(queryResult)) {
				if (!CommonUtility.isEmpty(queryResult.get(0).get(3)) && !queryResult.get(0).get(3).equals("None")) {
					alotNumber = queryResult.get(0).get(3);
				}
			}
		}

		private void setInOutCarrierAssignment() throws BaseException {
			String API = "setInOutCarrierAssignment(lotNumber='" + alotNumber + "', inputCarrierId='" + selectedInput + "', outputCarrierList=['" + global.getCarrierID()
					+ "'], status='ASSIGN', userId='" + global.getUser().getUserID() + "')";
			apiExecutorUpdate.transact("InOutCarrierAssign", "setInOutCarrierAssignment", API);
			global.setCarrierID("");
		}

		private void setInOutCarrierDeassignment(String inputId, String outputId) throws BaseException {
			String API = "setInOutCarrierAssignment(lotNumber='" + alotNumber + "', inputCarrierId='" + inputId + "', outputCarrierList=['" + outputId + "',], status='DEASSIGN', userId='" + global.getUser().getUserID()
					+ "')";
			apiExecutorUpdate.transact("InOutCarrierAssign", "setInOutCarrierDeassignment", API);
		}

		@SuppressLint("InflateParams")
		private void loadPageAfter() {
			for (InputCarrier input : groups) {
				LinearLayout l1 = (LinearLayout) getLayoutInflater().inflate(R.layout.in_out_carrier_group, null);
				input.inputNameView = (TextView) l1.findViewById(R.id.in_out_carrier_input_name);
				input.inputNameView.setText(input.inputName);
				input.assignButton = (Button) l1.findViewById(R.id.in_out_carrier_input_assign);
				input.assignButton.setOnClickListener(new AssignButtonOnClickListener(input.inputId));
				if (selectedInput.equals(input.inputId)) {
					input.assignButton.setText(getResources().getString(R.string.button_done));
				} else {
					input.assignButton.setText(getResources().getString(R.string.button_assign));
				}
				input.outputsLinear = (LinearLayout) l1.findViewById(R.id.in_out_assign_outputs);
				for (OutputCarrier output : input.outputs) {
					if (!output.outputStatus.equals("DEASSIGN")) {
						LinearLayout l2 = (LinearLayout) getLayoutInflater().inflate(R.layout.in_out_carrier_out, null);
						output.outputNameView = (TextView) l2.findViewById(R.id.in_out_carrier_out_name);
						output.outputNameView.setText(output.outputName);
						output.deassignButton = (Button) l2.findViewById(R.id.in_out_carrier_out_deassign);
						output.deassignButton.setOnClickListener(new DeassignButtonOnClickListener(input.inputId, output.outputId));
						if (output.outputStatus.equals("ASSIGN")) {
							output.deassignButton.setVisibility(View.VISIBLE);
						} else if (output.outputStatus.equals("COMPLETE")) {
							output.deassignButton.setVisibility(View.INVISIBLE);
						}
						input.outputsLinear.addView(l2);
					}
				}
				groupLinear.addView(l1);
			}
		}

	}

	@SuppressLint("HandlerLeak")
	class MyBaseHandler extends BaseHandler {

		MyBaseHandler(EditText alotNumberInput) {
			super(alotNumberInput);
		}

		@Override
		public void handleMessage(Message msg) {
			if (null != parseMessage((byte[]) msg.obj, msg.arg1) && null != alotNumberInput) {
				alotNumber = global.getAoLot().getAlotNumber();
				global.setAoLot(null);
				alotNumberInput.setText(alotNumber);
				if (null == qTask) {
					groups.clear();
					groupLinear.removeAllViews();
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("getInOutCarrierByLot");
				}
			}
		}
	};

	class InputCarrier {
		String inputId;
		String inputName;
		List<OutputCarrier> outputs = new ArrayList<OutputCarrier>();
		TextView inputNameView;
		Button assignButton;
		LinearLayout outputsLinear;
	}

	class OutputCarrier {
		String outputId;
		String outputName;
		String outputStatus;
		TextView outputNameView;
		TextView outputStatusView;
		Button deassignButton;
	}

	class AssignButtonOnClickListener implements View.OnClickListener {
		String inputId;

		AssignButtonOnClickListener(String inputId) {
			this.inputId = inputId;
		}

		@Override
		public void onClick(View v) {
			// store inputId to somewhere, once scan new tag, assign
			Button button = (Button) v;
			if (button.getText().toString().equals(getResources().getString(R.string.button_assign))) {
				selectedInput = inputId;
				button.setText(getResources().getString(R.string.button_done));
				global.setScanTarget(Constants.SCAN_TARGET_IN_OUT_ASSIGN_OUT);
			} else if (button.getText().equals(getResources().getString(R.string.button_done))) {
				selectedInput = "";
				global.setScanTarget(Constants.SCAN_TARGET_IN_OUT_ASSIGN_IN);
				button.setText(getResources().getString(R.string.button_assign));
			}
		}
	}

	class DeassignButtonOnClickListener implements View.OnClickListener {
		String inputId;
		String outputId;

		DeassignButtonOnClickListener(String inputId, String outputId) {
			this.inputId = inputId;
			this.outputId = outputId;
		}

		@Override
		public void onClick(View v) {
			// de-assign by lot, input, output
			log(inputId + " " + outputId);
			if (null == qTask) {
				groups.clear();
				groupLinear.removeAllViews();
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("setInOutCarrierDeassignment", inputId, outputId);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (global.getScanTarget().equals(Constants.SCAN_TARGET_IN_OUT_ASSIGN_IN)) {
			if (!CommonUtility.isEmpty(global.getCarrierID())) {
				if (qTask == null) {
					groups.clear();
					groupLinear.removeAllViews();
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("getInOutCarrierByTag");
				}
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_IN_OUT_ASSIGN_OUT)) {
			if (!CommonUtility.isEmpty(global.getCarrierID()) && !CommonUtility.isEmpty(selectedInput)) {
				if (qTask == null) {
					groups.clear();
					groupLinear.removeAllViews();
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("setInOutCarrierAssignment");
				}
			}
		}
	}

	@Override
	public void onBackPressed() {
		if (null != qTask) {
			qTask.cancel(true);
		}
		super.onBackPressed();
	}
	
	@Override
	protected void onPause() {
		if (null != qTask) {
			qTask.cancel(true);
		}
		super.onPause();
	}
}
