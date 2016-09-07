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
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.entity.DataCollection;
import app.utils.CommonUtilities;

import com.freescale.api.Constants;
import com.freescale.api.ConvertData;
import com.freescale.api.DateFormatter;
import com.fsl.cimei.rfid.entity.AOLot;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class EndLotActivity extends BaseActivity {

	private QueryTask qTask;
	private TextView machListView;
	private TextView carrierListView;
	private LinearLayout ppAssignmentListView;

	// private Spinner actualCarrierNumSpinner;

	private Button endButt;
	private Button addPPButt;
	private Button rejectButt;
	private Button cancelButt;

	private static final String MACH_ID = "MACH_ID";
	private static final String MACH_NAME = "MACH_NAME";
	private static final String MACH_MODEL = "MACH_MODEL";
	private static final String MACH_TYPE = "MACH_TYPE";
	private static final String CARRIER_ID = "CARRIER_ID";
	private static final String CARRIER_NAME = "CARRIER_NAME";
	private static final String COLUMNS_1ST = "COLUMNS_1ST";
	private static final String COLUMNS_2ND = "COLUMNS_2ND";
	private static final String COLUMNS_3RD = "COLUMNS_3RD";
	private static final String PP_PART_NUMBER = "PP_PART_NUMBER";
	private static final String PP_PART_LOT = "PP_PART_LOT";
	private static final String PP_MATERIAL_TYPE = "PP_MATERIAL_TYPE";

	private List<HashMap<String, Object>> machListItem;
	private List<HashMap<String, Object>> carrierListItem;
	private List<HashMap<String, Object>> ppAssignmentListItem;
	// private SimpleAdapter machAdapter;
	// private SimpleAdapter carrierAdapter;
	// private SimpleAdapter ppAssignmentAdapter;
	// private ArrayAdapter<String> carrierNumAdapter;

	private String currLotNum;
	private String currStepName;
	private String procName;
	private String procSeq;
	private String spvId;
	private ArrayList<String> machIdList = new ArrayList<String>();
	ArrayList<String> carrierList = new ArrayList<String>();

	private String currAckMemoMaxSetTime;
	private int memoCount;

	private String returnData = "";

	// String missingCarrierNum = "";
	int carrierNum;

	private EditText stripNumberInput;
	private TextView currentQtyView;
	private int currentQty = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_end_lot);
		mFormView = findViewById(R.id.end_lot_form);
		mStatusView = findViewById(R.id.end_lot_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.end_lot_tb_fragment);
		super.initTagBarcodeInput();

		currentQtyView = (TextView) findViewById(R.id.end_lot_current_qty);
		currentQtyView.setText("当前数量");
		machListView = (TextView) findViewById(R.id.end_lot_mach_list);
		carrierListView = (TextView) findViewById(R.id.end_lot_carrier_list);
		ppAssignmentListView = (LinearLayout) findViewById(R.id.end_lot_pp_assignment_list);

		// actualCarrierNumSpinner = (Spinner) findViewById(R.id.end_lot_missing_carrier_num_spinner);
		stripNumberInput = (EditText) findViewById(R.id.end_lot_strip_number_input);
		endButt = (Button) findViewById(R.id.end_lot_end_butt);
		addPPButt = (Button) findViewById(R.id.end_lot_add_pp_butt);
		rejectButt = (Button) findViewById(R.id.end_lot_reject_butt);
		cancelButt = (Button) findViewById(R.id.end_lot_cancel_butt);
		endButt.setOnClickListener(getButtOnClickListener(endButt.getId()));
		addPPButt.setOnClickListener(getButtOnClickListener(addPPButt.getId()));
		rejectButt.setOnClickListener(getButtOnClickListener(rejectButt.getId()));
		cancelButt.setOnClickListener(getButtOnClickListener(cancelButt.getId()));

	}

	/**
	 * get machine info by lot number
	 * 
	 * @author B45234
	 * @deprecated
	 * */
	private List<HashMap<String, Object>> getMachListByLot(String lotNumber) {
		List<HashMap<String, Object>> machListItem = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> machTableHeader = new HashMap<String, Object>();
		machTableHeader.put(MACH_ID, getResources().getString(R.string.mach_ID));
		machTableHeader.put(MACH_NAME, getResources().getString(R.string.mach_name));
		machTableHeader.put(MACH_TYPE, getResources().getString(R.string.mach_type));
		machListItem.add(machTableHeader);
		String API = "getCurrentMachineContext(attributes='machId, machName, machType',lotNumber='" + lotNumber + "')";
		Log.i("API:", API);
		DataCollection machListDC;
		try {
			machListDC = apiExecutorQuery.query("EndLot", "getMachListByLot", API);
			for (ArrayList<String> mach : machListDC) {
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(MACH_ID, mach.get(0));
				m.put(MACH_NAME, mach.get(1));
				m.put(MACH_TYPE, mach.get(2));
				machIdList.add(mach.get(0));
				machListItem.add(m);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return machListItem;
	}

	private List<HashMap<String, Object>> convertArrayListToListItem(ArrayList<String> arrayList, int cols) {
		List<HashMap<String, Object>> ListItem = new ArrayList<HashMap<String, Object>>();
		if (!CommonUtilities.isEmpty(arrayList) && arrayList.size() > 0) {
			if (cols == 2) {
				for (int i = 0; i < arrayList.size(); i = i + 2) {
					HashMap<String, Object> m = new HashMap<String, Object>();
					m.put(COLUMNS_1ST, arrayList.get(i));
					if (i + 1 < arrayList.size()) {
						m.put(COLUMNS_2ND, arrayList.get(i + 1));
					} else {
						m.put(COLUMNS_2ND, "");
					}
					ListItem.add(m);
				}
			} else if (cols == 3) {
				for (int i = 0; i < arrayList.size(); i = i + 3) {
					HashMap<String, Object> m = new HashMap<String, Object>();
					m.put(COLUMNS_1ST, arrayList.get(i));
					if (i + 1 < machIdList.size()) {
						m.put(COLUMNS_2ND, arrayList.get(i + 1));
					} else {
						m.put(COLUMNS_2ND, "");
					}
					if (i + 2 < machIdList.size()) {
						m.put(COLUMNS_3RD, arrayList.get(i + 2));
					} else {
						m.put(COLUMNS_3RD, "");
					}
					ListItem.add(m);
				}
			}

		}
		return ListItem;
	}

	/**
	 * get machine info by lot number
	 * 
	 * @author B45234
	 * @param cols
	 *            the amount of columns in list view.
	 * */
	private List<HashMap<String, Object>> getMachListByLot(String lotNumber, int cols) throws BaseException {
		machIdList = new ArrayList<String>();
		List<HashMap<String, Object>> machListItem = new ArrayList<HashMap<String, Object>>();
		String API = "getCurrentMachineContext(attributes='machId',lotNumber='" + lotNumber + "')";
		DataCollection machListDC = apiExecutorQuery.query("EndLot", "getMachListByLot", API);
		if (machListDC.size() > 0) {
			for (ArrayList<String> mach : machListDC) {
				machIdList.add(mach.get(0));
			}
		}
		machListItem = convertArrayListToListItem(machIdList, 2);
		return machListItem;
	}

	/**
	 * get Carrier info by lot number
	 * 
	 * @author B45234
	 * @deprecated
	 * */
	private List<HashMap<String, Object>> getCarrierByLot(String lotNumber) {
		List<HashMap<String, Object>> carrierListItem = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> carrierTableHeader = new HashMap<String, Object>();
		carrierTableHeader.put(CARRIER_ID, getResources().getString(R.string.carrier_id));
		carrierTableHeader.put(CARRIER_NAME, getResources().getString(R.string.carrier_name));
		carrierListItem.add(carrierTableHeader);
		String API = "getCarrierAttributes(attributes='carrierId, carrierName', lotNumber='" + lotNumber + "')";
		Log.i("API:", API);
		DataCollection carrierListDC;
		try {
			carrierListDC = apiExecutorQuery.query("EndLot", "getCarrierByLot", API);
			for (ArrayList<String> carrier : carrierListDC) {
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(CARRIER_ID, carrier.get(0));
				m.put(CARRIER_NAME, carrier.get(1));
				carrierListItem.add(m);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return carrierListItem;
	}

	/**
	 * get Carrier info by lot number
	 * 
	 * @author B45234
	 * @param cols
	 *            the amount of columns in list view.
	 * */
	private List<HashMap<String, Object>> getCarrierByLot(String lotNumber, int cols) throws BaseException {
		carrierList = new ArrayList<String>();
		List<HashMap<String, Object>> carrierListItem = new ArrayList<HashMap<String, Object>>();
		String API = "getCarrierAttributes(attributes='carrierName', lotNumber='" + lotNumber + "')";
		DataCollection carrierListDC = apiExecutorQuery.query("EndLot", "getCarrierByLot", API);
		if (CommonUtilities.isEmpty(apiExecutorQuery.getMessage()) && carrierListDC.size() > 0) {
			for (ArrayList<String> carrier : carrierListDC) {
				carrierList.add(carrier.get(0));
			}
		}
		carrierNum = carrierList.size();
		carrierListItem = convertArrayListToListItem(carrierList, 2);
		return carrierListItem;
	}

	/**
	 * check whether configure the requirement of Piece part.
	 * 
	 * @author B45234
	 * */
	private boolean requirePiecePart(String stepName, String devcName, String pkgCode) {
		boolean require = false;
		DataCollection dataContainer;
		ArrayList<String> finalControls = new ArrayList<String>();
		String API = "getMESParmValues(attributes='parmName,parmValue',parmOwner='endLotAtStep:" + stepName + ":" + devcName + "',parmOwnerType='TRAN:STEP:DNAM')";
		Log.i("API:", API);
		try {
			dataContainer = apiExecutorQuery.query("EndLot", "requirePiecePart", API);
			if (CommonUtilities.isEmpty(apiExecutorQuery.getMessage()) && dataContainer.size() > 0) {
				for (int i = 0; i < dataContainer.size(); i++) {
					String parmName = dataContainer.get(i).get(0);
					String parmValue = dataContainer.get(i).get(1);
					if (!CommonUtilities.isEmpty(parmValue) && parmValue.equals("1")) {
						finalControls.add(parmName);
					}
				}
			}
			API = "getMESParmValues(attributes='parmName,parmValue',parmOwner='endLotAtStep:" + stepName + ":" + pkgCode + "',parmOwnerType='TRAN:STEP:PCKG')";
			Log.i("API:", API);
			dataContainer = apiExecutorQuery.query("EndLot", "requirePiecePart", API);
			if (CommonUtilities.isEmpty(apiExecutorQuery.getMessage()) && dataContainer.size() > 0) {
				for (int i = 0; i < dataContainer.size(); i++) {
					String parmName = dataContainer.get(i).get(0);
					String parmValue = dataContainer.get(i).get(1);
					if (!CommonUtilities.isEmpty(parmValue) && parmValue.equals("1")) {
						finalControls.add(parmName);
					}
				}
			}
			API = "getMESParmValues(attributes='parmName,parmValue',parmOwner='endLotAtStep:" + stepName + "',parmOwnerType='TRAN:STEP')";
			Log.i("API:", API);
			dataContainer = apiExecutorQuery.query("EndLot", "requirePiecePart", API);
			if (CommonUtilities.isEmpty(apiExecutorQuery.getMessage()) && dataContainer.size() > 0) {
				for (int i = 0; i < dataContainer.size(); i++) {
					String parmName = dataContainer.get(i).get(0);
					String parmValue = dataContainer.get(i).get(1);
					if (!CommonUtilities.isEmpty(parmValue) && parmValue.equals("1")) {
						finalControls.add(parmName);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (finalControls.contains("requirePiecePart")) {
			require = true;
		}
		return require;

	}

	/**
	 * get Piece Part info by lot number
	 * 
	 * @author B45234
	 * */
	private List<HashMap<String, Object>> getPpAssignment(String lotNumber) throws BaseException {
		List<HashMap<String, Object>> ppAssignmentListItem = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> ppAssignmentTableHeader = new HashMap<String, Object>();
		ppAssignmentTableHeader.put(PP_PART_NUMBER, getResources().getString(R.string.pp_part_number));
		ppAssignmentTableHeader.put(PP_PART_LOT, getResources().getString(R.string.pp_part_lot));
		ppAssignmentTableHeader.put(PP_MATERIAL_TYPE, getResources().getString(R.string.pp_material_type));
		ppAssignmentListItem.add(ppAssignmentTableHeader);
		String stepName = "";
		String devcName = "";
		String pkgCode = "";
		String pkgKit = "";
		DataCollection dataContainer;
		String API = "getCurrentStepContext(attributes='stepName,procName,stepSeq', lotNumber='" + lotNumber + "')";
		dataContainer = apiExecutorQuery.query("EndLot", "getPpAssignment", API);
		if (dataContainer.size() > 0) {
			stepName = dataContainer.get(0).get(0);
			currStepName = stepName;
			procName = dataContainer.get(0).get(1);
			procSeq = dataContainer.get(0).get(2);
		}
		API = "getLotAttributes(attributes='devcName, pkgCode, pkgKit, spvId',lotNumber='" + lotNumber + "')";
		dataContainer = apiExecutorQuery.query("EndLot", "getPpAssignment", API);
		if (dataContainer.size() > 0) {
			devcName = dataContainer.get(0).get(0);
			pkgCode = dataContainer.get(0).get(1);
			pkgKit = dataContainer.get(0).get(2);
			spvId = dataContainer.get(0).get(3);
		}
		if (!CommonUtilities.isEmpty(stepName) && !CommonUtilities.isEmpty(devcName) && !CommonUtilities.isEmpty(pkgCode)
				&& requirePiecePart(stepName, devcName, pkgCode)) {
			API = "getConsumedPiecepartContext(lotNumber='" + lotNumber + "',stepName='" + stepName
					+ "',attributes='ppDevcNumber,ppLotNumber,consumedQty,supplierName,trakMatlOwner,trakCustSrc')";
			dataContainer = apiExecutorQuery.query("EndLot", "getPpAssignment", API);
			if (dataContainer.size() > 0) {
				for (int i = 0; i < dataContainer.size(); i++) {
					String ppDevcNumber = dataContainer.get(i).get(0);
					String ppLotNumber = dataContainer.get(i).get(1);
					String matlType = "";
					if (!CommonUtilities.isEmpty(ppDevcNumber) && !ppDevcNumber.equals("None") && !CommonUtilities.isEmpty(pkgKit) && !pkgKit.equals("None")) {
						API = "getBomAttributes(pkgKit='" + pkgKit + "', stepName='" + stepName + "', devcNumber='" + ppDevcNumber + "',attributes='matlType')";
						DataCollection dc = apiExecutorQuery.query("EndLot", "getPpAssignment", API);
						if (dc.size() > 0) {
							matlType = dc.get(0).get(0);
						}
					}
					HashMap<String, Object> row = new HashMap<String, Object>();
					row.put(PP_PART_NUMBER, ppDevcNumber);
					row.put(PP_PART_LOT, ppLotNumber);
					row.put(PP_MATERIAL_TYPE, matlType);
					ppAssignmentListItem.add(row);
				}
			} else {
				API = "getCurrentAssignedPiecePartLotUsage(lotNumber='" + lotNumber + "',stepName='" + stepName + "',attributes='ppDevcNumber,ppLotNumber,mtrlType')";
				dataContainer = apiExecutorQuery.query("EndLot", "getPpAssignment", API);
				if (dataContainer.size() > 0) {
					for (int i = 0; i < dataContainer.size(); i++) {
						String ppDevcNumber = dataContainer.get(i).get(0);
						String ppLotNumber = dataContainer.get(i).get(1);
						String mtrlType = dataContainer.get(i).get(2);
						HashMap<String, Object> row = new HashMap<String, Object>();
						row.put(PP_PART_NUMBER, ppDevcNumber);
						row.put(PP_PART_LOT, ppLotNumber);
						row.put(PP_MATERIAL_TYPE, mtrlType);
						ppAssignmentListItem.add(row);
					}
				}
			}

		}
		return ppAssignmentListItem;
	}

	/**
	 * get Memo info (memoCount, currAckMemoMaxSetTime)
	 * 
	 * @author B45234
	 * */
	private void getMemoInfo(String lotNumber, String stepName) throws BaseException {
		String ackMemoMaxSetTime = "";
		String API = "getUIAllMemos(lotNumber = '" + lotNumber + "', stepName = '" + stepName + "')";
		DataCollection dataEngineeringMemo = apiExecutorQuery.query("EndLot", "getMemoInfo", API);
		if (CommonUtilities.isEmpty(apiExecutorQuery.getMessage()) && dataEngineeringMemo.size() > 0) {
			for (int i = 0; i < dataEngineeringMemo.size(); i++) {
				if (!CommonUtilities.isEmpty(dataEngineeringMemo.get(i).get(2))) {
					String memoIntr;
					try {
						memoIntr = ConvertData.decode(dataEngineeringMemo.get(i).get(2));
					} catch (UnsupportedEncodingException e) {
						throw new RfidException(e.toString(), "EndLot", "getMemoInfo", API);
					}
					dataEngineeringMemo.get(i).set(2, memoIntr);
					if (!CommonUtilities.isEmpty(ackMemoMaxSetTime)) {
						try {
							Date d1 = DateFormatter.getSimpleDateToDate(ackMemoMaxSetTime);
							Date d2 = DateFormatter.getSimpleDateToDate(dataEngineeringMemo.get(i).get(3));
							ackMemoMaxSetTime = d1.compareTo(d2) > 0 ? ackMemoMaxSetTime : dataEngineeringMemo.get(i).get(3);
						} catch (ParseException e) {
							throw new RfidException(e.toString(), "EndLot", "getMemoInfo", API);
						}

					} else {
						ackMemoMaxSetTime = dataEngineeringMemo.get(i).get(3);
					}
				}
				String setTime = dataEngineeringMemo.get(i).get(3);
				String instrBy = dataEngineeringMemo.get(i).get(5) + " " + dataEngineeringMemo.get(i).get(6) + "(" + dataEngineeringMemo.get(i).get(4) + ")";
				dataEngineeringMemo.get(i).set(3, instrBy);
				dataEngineeringMemo.get(i).set(4, setTime);
				for (int j = dataEngineeringMemo.get(i).size() - 1; j > 4; j--) {
					dataEngineeringMemo.get(i).remove(j);
				}
			}
		}
		memoCount = dataEngineeringMemo.size();
		currAckMemoMaxSetTime = ackMemoMaxSetTime;
	}

	private String getSPVStepInstructionDate(String spvId, String stepName, String stepSeq, String flowId) throws BaseException {
		String spvInstructionDate = "";
		if (!CommonUtility.isEmpty(spvId) && !CommonUtility.isEmpty(stepName) && !CommonUtility.isEmpty(stepSeq) && !CommonUtility.isEmpty(flowId)) {
			String API = "getSPVStepInstructions(attributes='stepName,instructions, setDate, setUserId ',spvId='" + spvId + "',stepName='" + stepName + "',stepSeq = '"
					+ stepSeq + "',flowId = '" + flowId + "')";
			DataCollection dataContainer = apiExecutorQuery.query("EndLot", "getSPVStepInstructionDate", API);
			if (CommonUtilities.isEmpty(apiExecutorQuery.getMessage()) && dataContainer.size() > 0) {
				spvInstructionDate = dataContainer.get(0).get(2);
			}
		}
		return spvInstructionDate;
	}

	public static String convertDateToAPITuple(String date) {
		String tupleString = "";
		String convertedDate = "";
		String[] tempDate = date.replaceAll("/", ",").replaceAll(":", ",").replaceAll(" ", ",").split(",");
		for (String a : tempDate) {
			convertedDate = convertedDate + Integer.parseInt(a) + ",";
		}
		tupleString = "(" + convertedDate.substring(0, convertedDate.length() - 1) + ")";
		return tupleString;
	}

	/**
	 * return button onClick listener by button id
	 * 
	 * @author B45234
	 * */
	private View.OnClickListener getButtOnClickListener(int buttId) {
		switch (buttId) {
		case R.id.end_lot_end_butt:
			return new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (qTask == null) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("endLot");
					}
				}

			};
		case R.id.end_lot_add_pp_butt:
			return new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					// Intent i = new Intent(EndLotActivity.this, PiecePartLoadActivity.class);
					// startActivity(i);
				}

			};
		case R.id.end_lot_reject_butt:
			return new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					global.setAoLot(new AOLot(currLotNum));
					Intent i = new Intent(EndLotActivity.this, RejActivity.class);
					startActivity(i);
				}

			};
		case R.id.end_lot_cancel_butt:
			return new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					finish();
				}

			};
		default:
			// throw error message in here.
			return null;
		}
	}

	/**
	 * setup Adapter and Listeners for Spinner by Spinner id.
	 * 
	 * @author B45234
	 * */
	// private void setupAdapterAndListenerforSpinner(Spinner spinner) {
	// if (spinner != null) {
	// switch (spinner.getId()) {
	// case R.id.end_lot_missing_carrier_num_spinner:
	// List<String> carrierNumList = new ArrayList<String>();
	// carrierNumList.add("");
	// for (int i = 0; i <= 10; i++) {
	// carrierNumList.add(new Integer(i).toString());
	// }
	// carrierNumAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, carrierNumList);
	// carrierNumAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	// spinner.setAdapter(carrierNumAdapter);
	// spinner.setSelection(1);
	// spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
	//
	// @Override
	// public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
	// missingCarrierNum = carrierNumAdapter.getItem(position);
	// parent.setVisibility(View.VISIBLE);
	// }
	//
	// @Override
	// public void onNothingSelected(AdapterView<?> parent) {
	//
	// }
	// });
	// spinner.setOnTouchListener(new Spinner.OnTouchListener() {
	//
	// @Override
	// public boolean onTouch(View v, MotionEvent event) {
	// // v.setVisibility(View.INVISIBLE);
	// return false;
	// }
	// });
	// spinner.setOnFocusChangeListener(new Spinner.OnFocusChangeListener() {
	//
	// @Override
	// public void onFocusChange(View v, boolean hasFocus) {
	// v.setVisibility(View.VISIBLE);
	// }
	// });
	// default:
	// // throw error message in here.
	// }
	// }
	// }

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("loadPage")) {
					loadPage();
				} else if (cmdName.equals("endLot")) {
					insertLotComment();
					endLot();
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
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
				if (cmdName.equals("loadPage")) {
					loadPageAfter();
				} else if (cmdName.equals("endLot")) {
					endLotAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(EndLotActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(EndLotActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void loadPage() throws BaseException {
			// machListItem = getMachListByLot(currLotNum);
			// carrierListItem = getCarrierByLot(currLotNum);
			String api = "getCurrentStepContext(attributes='procName,stepSeq,stepName,startQty,startTime,startUserId,lotStatus,devcNumber,pkgCode,rejQty', lotNumber='"
					+ currLotNum + "')";
			DataCollection queryResult = apiExecutorQuery.query("EndLot", "loadPage", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				String startQty = queryResult.get(0).get(3);
				String rejQty = queryResult.get(0).get(9);
				int startQtyValue = -1;
				int rejQtyValue = -1;
				try {
					startQtyValue = Integer.parseInt(startQty);
					rejQtyValue = Integer.parseInt(rejQty);
					if (startQtyValue != -1 && rejQtyValue != -1) {
						currentQty = startQtyValue - rejQtyValue;
					}
				}catch(NumberFormatException e) {
				}
			}
			machListItem = getMachListByLot(currLotNum, 2);
			carrierListItem = getCarrierByLot(currLotNum, 2);
			ppAssignmentListItem = getPpAssignment(currLotNum);
		}

		private void loadPageAfter() {

			// machAdapter = new SimpleAdapter(EndLotActivity.this, machListItem, R.layout.end_lot_mach_list_item,
			// new String[]{MACH_ID, MACH_NAME, MACH_TYPE},
			// new int[]{R.id.end_lot_item_mach_id, R.id.end_lot_item_mach_name, R.id.end_lot_item_mach_type } );
			// carrierAdapter = new SimpleAdapter(EndLotActivity.this, carrierListItem, R.layout.end_lot_carrier_list_item,
			// new String[]{CARRIER_ID, CARRIER_NAME},
			// new int[]{R.id.end_lot_item_carrier_id, R.id.end_lot_item_carrier_name} );

			// machAdapter = new SimpleAdapter(EndLotActivity.this, machListItem, R.layout.end_lot_2columns_list_item, new String[] { COLUMNS_1ST, COLUMNS_2ND }, new int[] {
			// R.id.end_lot_item_2cols_1st_col, R.id.end_lot_item_2cols_2nd_col });
			// carrierAdapter = new SimpleAdapter(EndLotActivity.this, carrierListItem, R.layout.end_lot_2columns_list_item, new String[] { COLUMNS_1ST, COLUMNS_2ND },
			// new int[] { R.id.end_lot_item_2cols_1st_col, R.id.end_lot_item_2cols_2nd_col });
			// ppAssignmentAdapter = new SimpleAdapter(EndLotActivity.this, ppAssignmentListItem, R.layout.end_lot_pp_assignment_list_item, new String[] { PP_PART_NUMBER,
			// PP_PART_LOT, PP_MATERIAL_TYPE }, new int[] { R.id.end_lot_item_pp_devc_num, R.id.end_lot_item_pp_lot_num, R.id.end_lot_item_pp_matrl_type });
			// machListView.setAdapter(machAdapter);
			// carrierListView.setAdapter(carrierAdapter);
			// ppAssignmentListView.setAdapter(ppAssignmentAdapter);
			machListView.setText(machIdList.toString());
			carrierListView.setText(carrierList.toString());
			currentQtyView.setText("当前数量：" + currentQty);
			for (HashMap<String, Object> row : ppAssignmentListItem) {
				LinearLayout line = (LinearLayout) EndLotActivity.this.getLayoutInflater().inflate(R.layout.end_lot_pp_assignment_list_item, null);
				TextView ppDevc = (TextView) line.findViewById(R.id.end_lot_item_pp_devc_num);
				ppDevc.setText(row.get(PP_PART_NUMBER).toString());
				TextView ppLot = (TextView) line.findViewById(R.id.end_lot_item_pp_lot_num);
				ppLot.setText(row.get(PP_PART_LOT).toString());
				TextView ppMatlType = (TextView) line.findViewById(R.id.end_lot_item_pp_matrl_type);
				ppMatlType.setText(row.get(PP_MATERIAL_TYPE).toString());
				ppAssignmentListView.addView(line);
			}

			// setupAdapterAndListenerforSpinner(actualCarrierNumSpinner);
		}

		/**
		 * insert comments into ALOT_COMMENTS table.
		 * 
		 * @author B45234
		 * */
		private void insertLotComment() throws BaseException {
			// String transUserId = "";
			// if (CommonUtilities.isEmpty(missingCarrierNum) || "".equals(missingCarrierNum) || !CommonUtilities.isNumeric(missingCarrierNum)) {
			// throw new RfidException("Please select a valid number!", "EndLot", "insertLotComment", "");
			// }
			// String comment = "LOT END FAILED: CODE(CACNT). Assigned: (" + carrierNum + "), Input: ( ), Missing: (" + missingCarrierNum + ").";
			// if (CommonUtilities.isEmpty(global.getUser())) {
			// throw new RfidException("Please input a valid user account!", "EndLot", "insertLotComment", "");
			// } else {
			// transUserId = global.getUser().getUserID();
			// }
			// String API = "insertLotComment(transUserId='" + transUserId + "', lotNumber='" + currLotNum + "', procName='" + procName + "', stepSeq=" + procSeq
			// + ", stepName='" + currStepName + "', userId='" + transUserId + "', comments='" + comment + "')";
			// apiExecutorUpdate.transact("EndLot", "insertLotComment", API);
			String text = stripNumberInput.getText().toString();
			if (!CommonUtility.isEmpty(text)) {
				if (!CommonUtility.isValidNumber(text)) {
					throw new RfidException("条数输入有误", "EndLot", "insertLotComment", "");
				} else {
					String API = "updateLHSTAttributes(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + currLotNum + "',stepName='" + currStepName
							+ "',hostname='" + global.getAndroidSecureID() + "',STRIP_NUMBER=" + text + ")";
					apiExecutorUpdate.transact("EndLot", "insertLotComment", API);
				}
			}
		}

		private void endLot() throws BaseException {
			String transUserId = "";
			String lotNumber = currLotNum;
			String stepName = currStepName;
			String nextStepName = "";
			String trakShift = "1";
			String hostName = "";
			String machIds = "";
			String assyOrTest = "ASSEMBLY";
			String spvInstructionDate = "";

			if (CommonUtilities.isEmpty(global.getUser())) {
				throw new RfidException("Please input a valid user account!", "EndLot", "insertLotComment", "");
			} else {
				transUserId = global.getUser().getUserID();
			}
			if (!CommonUtilities.isEmpty(global.getAndroidSecureID())) {
				hostName = global.getAndroidSecureID();
				if (hostName.length() > 16) {// the max length of TRANLOG.LOGICAL_TERM is 16
					hostName = hostName.substring(0, 16);
				}
			}
			String API = "getProcessFlowAttributes(attributes = 'stepSeq, stepName, trakOper', masterProcess = '" + procName + "')";
			DataCollection dataContainer = apiExecutorQuery.query("EndLot", "endLot", API);
			for (int x = 0; x < dataContainer.size(); x++) {
				if (dataContainer.get(x).get(0).equals(procSeq)) {
					if ((x + 1) < dataContainer.size()) {
						nextStepName = dataContainer.get(x + 1).get(1);
						break;
					}
				}
			}
			for (String machId : machIdList) {
				if (CommonUtilities.isEmpty(machIds)) {
					machIds = "'" + machId + "'";
				} else {
					machIds = machIds + ", '" + machId + "'";
				}
			}
			getMemoInfo(lotNumber, stepName);
			spvInstructionDate = getSPVStepInstructionDate(spvId, stepName, procSeq, procName);
			API = "validateUIRulesEndLotAtStep(transUserId='" + transUserId + "', lotNumber='" + lotNumber + "', stepName='" + stepName + "', nextStepName='"
					+ nextStepName + "', trakShift='" + trakShift + "', memoCount=" + memoCount + ", assyOrTest='" + assyOrTest + "'";
			if (!CommonUtilities.isEmpty(hostName) && !hostName.trim().equalsIgnoreCase("None")) {
				API = API + ", hostName='" + hostName + "'";
			}
			if (!CommonUtilities.isEmpty(machIds)) {
				API = API + ", machIdList=[" + machIds + "]";
			}
			if (!CommonUtilities.isEmpty(spvInstructionDate)) {
				API = API + ",spvInstructionDate=" + convertDateToAPITuple(spvInstructionDate) + "";
			}
			if (!CommonUtilities.isEmpty(currAckMemoMaxSetTime)) {
				API = API + ",ackMemoMaxSetTime=" + convertDateToAPITuple(currAckMemoMaxSetTime) + "";
			}
			if (!CommonUtilities.isEmpty(spvId) && !spvId.trim().equalsIgnoreCase("None")) {
				API = API + ", spvId='" + spvId + "'";
			}
			API = API + ")";
			returnData = apiExecutorUpdate.transact("EndLot", "endLot", API);
		}

		private void endLotAfter() {
			if (CommonUtilities.isEmpty(returnData) || returnData.trim().equalsIgnoreCase("None")) {
				showSuccessDialog(EndLotActivity.this, "Successfully end lot!");
			} else {
				showSuccessDialog(EndLotActivity.this, "Successfully end lot!\r\n" + returnData);
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

	@Override
	protected void onResume() {
		if (null != global.getAoLot() && null == qTask) {
			currLotNum = global.getAoLot().getAlotNumber();
			alotNumberTextView.setText(currLotNum);
			ppAssignmentListView.removeAllViews();
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("loadPage");
		}
		super.onResume();
	}

	public void setBarcodeInput(String alotNumber) {
		log("EndLot setLotNumber");
		currLotNum = alotNumber;
		alotNumberTextView.setText(currLotNum);
		ppAssignmentListView.removeAllViews();
		if (null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("loadPage");
		}
	}

	public void startScanBarcode() {
		log("EndLot startScanBarcode");
		if (qTask == null) {
			tagBarcodeInput.setText("");
			qTask = new QueryTask();
			qTask.execute("scanBarcode");
		}
	}

}
