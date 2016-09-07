package com.fsl.cimei.rfid;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.freescale.api.ConvertData;
import com.freescale.api.DateFormatter;
import com.fsl.cimei.rfid.entity.Step;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class SPVMemoAcknowledgeActivity extends BaseActivity {

	private QueryTask qTask;
	private TextView spvGeneralInstr;
	private String spvGeneralInstrStr = "";
	private DataCollection spvInstrStep;
	private DataCollection spvParams;
	private ListView spvInstrStepListView;
	private List<HashMap<String, Object>> spvInstrStepListItem;
	private ListView spvParamListView;
	private List<HashMap<String, Object>> spvParamListItem;
	private static final String SPV_STEP_INSTR = "SPV_STEP_INSTR";
	private static final String SPV_STEP_INSTR_BY = "SPV_STEP_INSTR_BY";
	private static final String SPV_STEP_INSTR_TIME = "SPV_STEP_INSTR_TIME";
	private static final String PARAM_NAME = "PARAM_NAME";
	private static final String PARAM_VALUE = "PARAM_VALUE";
	private static final String SPV = "SPV";
	private Button acknowlegeButton;
	private Button cancelButton;
	private DataCollection engineeringMemo;
	private ListView engineeringMemoListView;
	private List<HashMap<String, Object>> engineeringMemoListItem;
	private static final String MEMO_TYPE_SET_FOR = "MEMO_TYPE_SET_FOR";
	private static final String MEMO_TEXT = "MEMO_TEXT";
	private static final String MEMO_BY_SET_TIME = "MEMO_BY_SET_TIME";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_spv_memo_acknowledge);
		mFormView = findViewById(R.id.spv_memo_form);
		mStatusView = findViewById(R.id.spv_memo_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		spvGeneralInstr = (TextView) findViewById(R.id.spv_general_instruction);
		spvInstrStep = new DataCollection();
		spvInstrStepListView = (ListView) findViewById(R.id.start_lot_spv_step_instr_list);
		spvInstrStepListItem = new ArrayList<HashMap<String, Object>>();
		spvParams = new DataCollection();
		spvParamListView = (ListView) findViewById(R.id.start_lot_spv_param_list);
		spvParamListItem = new ArrayList<HashMap<String, Object>>();
		engineeringMemo = new DataCollection();
		engineeringMemoListView = (ListView) findViewById(R.id.start_lot_engineering_memo_list);
		engineeringMemoListItem = new ArrayList<HashMap<String, Object>>();
		acknowlegeButton = (Button) findViewById(R.id.button_start_spv_memo_acknowledge);
		cancelButton = (Button) findViewById(R.id.button_start_spv_memo_cancel);
		if (null != global.getAoLot() && null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("combineSPVMemoInfo");
		} else {
			finish();
		}

		acknowlegeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SPVMemoAcknowledgeActivity.this, LotStartActivity.class);
				startActivity(intent);
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
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
				if (cmdName.equals("combineSPVMemoInfo")) {
					combineSPVMemoInfo();
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
				if (cmdName.equals("combineSPVMemoInfo")) {
					combineSPVMemoInfoAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(SPVMemoAcknowledgeActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(SPVMemoAcknowledgeActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void combineSPVMemoInfo() throws BaseException {
			String spvID = null;
			String API = "getCurrentStepContext(attributes='stepName,startTime,stepSeq,procName,prodLine,devcNumber',lotNumber='" + global.getAoLot().getAlotNumber()
					+ "')";
			DataCollection currentStepContext = apiExecutorQuery.query("SPVMemoAcknowledge", "combineSPVMemoInfo", API);
			if (currentStepContext.size() == 1) {
				String stepName = currentStepContext.get(0).get(0);
				String startTime = currentStepContext.get(0).get(1);
				String stepSeq = currentStepContext.get(0).get(2);
				String procName = currentStepContext.get(0).get(3);
				String prodLine = currentStepContext.get(0).get(4);
				String devcNumber = currentStepContext.get(0).get(5);
				Step currentStep = new Step();
				currentStep.setStepName(stepName);
				currentStep.setStepSeq(stepSeq);
				currentStep.setProcName(procName);
				currentStep.setProdLine(prodLine);
				currentStep.setDevcNumber(devcNumber);
				global.getAoLot().setCurrentStep(currentStep);
				if (!CommonUtility.isEmpty(startTime) && !startTime.trim().equals("")) {
					API = "getLotAttributes(attributes='spvAssyId',lotNumber='" + global.getAoLot().getAlotNumber() + "')";
					DataCollection spvIdDC = apiExecutorQuery.query("SPVMemoAcknowledge", "combineSPVMemoInfo", API);
					if (spvIdDC.size() == 1) {
						spvID = spvIdDC.get(0).get(0);
					}
				} else {
					throw new RfidException("Lot [" + global.getAoLot().getAlotNumber() + "] has already started in step [" + stepName + "]", "SPVMemoAcknowledge",
							"combineSPVMemoInfo", API);
				}
			}

			if (spvID != null && !spvID.equals("None")) {
				API = "getSPVAttributes(attributes='spvGeneralInstructions',lotNumber='" + global.getAoLot().getAlotNumber() + "',spvAOLotId = '"
						+ global.getAoLot().getSpvID() + "')";
				DataCollection dataSPVInstr = apiExecutorQuery.query("SPVMemoAcknowledge", "combineSPVMemoInfo", API);
				try {
					spvGeneralInstrStr = ConvertData.decode(dataSPVInstr.get(0).get(0));
				} catch (UnsupportedEncodingException e) {
					throw new RfidException(e.toString(), "SPVMemoAcknowledge", "combineSPVMemoInfo", API);
				}

				// get spv instructions
				API = "getSPVStepInstructions(attributes='stepName,instructions,setUserId,setDate',spvId='" + global.getAoLot().getSpvID() + "',stepName='"
						+ global.getAoLot().getCurrentStep().getStepName() + "',stepSeq = '" + global.getAoLot().getCurrentStep().getStepSeq() + "',flowId = '"
						+ global.getAoLot().getCurrentStep().getProcName() + "')";
				spvInstrStep = apiExecutorQuery.query("SPVMemoAcknowledge", "combineSPVMemoInfo", API);

				// get eSPV parameters
				API = "getSPVLotProcessingParmValues(attributes = 'parmName,parmValue,spvFlag',spvFlag = 'Y', lotNumber = '" + global.getAoLot().getAlotNumber()
						+ "', stepName = '" + global.getAoLot().getCurrentStep().getStepName() + "', flowId =  '" + global.getAoLot().getCurrentStep().getProcName()
						+ "', stepSeq = '" + global.getAoLot().getCurrentStep().getStepSeq() + "')";
				spvParams = apiExecutorQuery.query("SPVMemoAcknowledge", "combineSPVMemoInfo", API);
			}

			API = "getUIAllMemos(lotNumber = '" + global.getAoLot().getAlotNumber() + "', stepName = '" + global.getAoLot().getCurrentStep().getStepName() + "')";
			engineeringMemo = apiExecutorQuery.query("SPVMemoAcknowledge", "combineSPVMemoInfo", API);
			String ackMemoMaxSetTime = "";
			if (engineeringMemo.size() > 0) {
				for (int i = 0; i < engineeringMemo.size(); i++) {
					if (!CommonUtility.isEmpty(engineeringMemo.get(i).get(2))) {
						String memoIntr;
						try {
							memoIntr = ConvertData.decode(engineeringMemo.get(i).get(2));
						} catch (UnsupportedEncodingException e) {
							throw new RfidException(e.toString(), "SPVMemoAcknowledge", "combineSPVMemoInfo", API);
						}
						engineeringMemo.get(i).set(2, memoIntr);
						if (!CommonUtility.isEmpty(ackMemoMaxSetTime)) {
							try {
								Date d1 = DateFormatter.getSimpleDateToDate(ackMemoMaxSetTime);
								Date d2 = DateFormatter.getSimpleDateToDate(engineeringMemo.get(i).get(3));
								ackMemoMaxSetTime = d1.compareTo(d2) > 0 ? ackMemoMaxSetTime : engineeringMemo.get(i).get(3);
							} catch (ParseException e) {
								throw new RfidException(e.toString(), "SPVMemoAcknowledge", "combineSPVMemoInfo", API);
							}
						} else {
							ackMemoMaxSetTime = engineeringMemo.get(i).get(3);
						}
					}
					String setTime = engineeringMemo.get(i).get(3);
					String instrBy = engineeringMemo.get(i).get(5) + " " + engineeringMemo.get(i).get(6) + "(" + engineeringMemo.get(i).get(4) + ")";
					engineeringMemo.get(i).set(3, instrBy);
					engineeringMemo.get(i).set(4, setTime);
					for (int j = engineeringMemo.get(i).size() - 1; j > 4; j--) {
						engineeringMemo.get(i).remove(j);
					}
				}
			}
		}

		private void combineSPVMemoInfoAfter() {

			if (CommonUtility.isEmpty(spvGeneralInstrStr) && CommonUtility.isEmpty(spvInstrStep) && CommonUtility.isEmpty(spvParams)) {
				Intent intent = new Intent(SPVMemoAcknowledgeActivity.this, LotStartActivity.class);
				startActivity(intent);
			} else {
				spvGeneralInstr.setText(spvGeneralInstrStr);
				for (ArrayList<String> step : spvInstrStep) {
					HashMap<String, Object> m = new HashMap<String, Object>();
					try {
						m.put(SPV_STEP_INSTR, ConvertData.decode(step.get(1)));
					} catch (UnsupportedEncodingException e) {
					}
					m.put(SPV_STEP_INSTR_BY, step.get(2));
					m.put(SPV_STEP_INSTR_TIME, step.get(3));
					spvInstrStepListItem.add(m);
				}
				SimpleAdapter listItemAdapter = new SimpleAdapter(SPVMemoAcknowledgeActivity.this, spvInstrStepListItem, R.layout.lot_start_spv_step_instr_list_item,
						new String[] { SPV_STEP_INSTR, SPV_STEP_INSTR_BY, SPV_STEP_INSTR_TIME }, new int[] { R.id.title_start_spv_step_instr1,
								R.id.title_start_spv_step_instr2, R.id.title_start_spv_step_instr3 });
				spvInstrStepListView.setAdapter(listItemAdapter);

				for (ArrayList<String> param : spvParams) {
					// Parameter Name,Parameter Value,SPV
					HashMap<String, Object> m = new HashMap<String, Object>();
					m.put(PARAM_NAME, param.get(0));
					m.put(PARAM_VALUE, param.get(1));
					m.put(SPV, param.get(2));
					spvInstrStepListItem.add(m);
				}
				SimpleAdapter listItemAdapter2 = new SimpleAdapter(SPVMemoAcknowledgeActivity.this, spvParamListItem, R.layout.lot_start_spv_step_instr_list_item,
						new String[] { PARAM_NAME, PARAM_VALUE, SPV }, new int[] { R.id.title_start_spv_step_instr1, R.id.title_start_spv_step_instr2,
								R.id.title_start_spv_step_instr3 });
				spvParamListView.setAdapter(listItemAdapter2);

				for (ArrayList<String> memo : engineeringMemo) {
					// Memo Type,Memo Set For,Memo Text,Instruction By,Set Time
					HashMap<String, Object> m = new HashMap<String, Object>();
					m.put(MEMO_TYPE_SET_FOR, memo.get(0) + " " + memo.get(1));
					m.put(MEMO_TEXT, memo.get(2));
					m.put(MEMO_BY_SET_TIME, memo.get(3) + " " + memo.get(4));
					engineeringMemoListItem.add(m);
				}
				SimpleAdapter listItemAdapter3 = new SimpleAdapter(SPVMemoAcknowledgeActivity.this, engineeringMemoListItem, R.layout.lot_start_spv_step_instr_list_item,
						new String[] { MEMO_TYPE_SET_FOR, MEMO_TEXT, MEMO_BY_SET_TIME }, new int[] { R.id.title_start_spv_step_instr1, R.id.title_start_spv_step_instr2,
								R.id.title_start_spv_step_instr3 });
				engineeringMemoListView.setAdapter(listItemAdapter3);
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
