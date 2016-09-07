package com.fsl.cimei.rfid;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.AOLot;
import com.fsl.cimei.rfid.entity.Step;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class RejActivity extends BaseActivity {

	private String alotNumber = "";
	private String tagUUID = "";
	private QueryTask qTask = null;
	// private ArrayList<String> machineIDs = null;
	private DataCollection defectList;
	private DataCollection defectCounts;
	private DataCollection machineIDs;
	private LinearLayout rejMachinesLinearLayout;
	private Map<String, Integer> rejTotalByMach = new HashMap<String, Integer>();
	// private ListView rejDefectListView;
	private LinearLayout rejDefectListLinearLayout;
	private List<Map<String, Object>> defectData = new ArrayList<Map<String, Object>>();
	// private RejLineAdapter adapter = null;
	private List<ViewHolder> viewHolderList = new ArrayList<ViewHolder>();
	private String currmachname;
	private Button submitAllButton;
	private Map<String, String> batchSubmitDefects = new HashMap<String, String>();
	// private EditText tagOrBarcodeInput;
	// private Button n7ScanBarcode;
	// private TextView alotNumberView;
	private Spinner stepSpinner;
	private List<String> stepList = new ArrayList<String>();
	private ArrayAdapter<String> stepArrayAdapter;
	private boolean previousStepRej = false;
	private TextView stepNameTitle;
	private String lastStepNameSelected = "";
	private int totalQtyChange = 0;
	private final String classname = "Rej";
	private TextView startQtyView;
	private TextView rejQtyView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rej);
		mFormView = findViewById(R.id.rej_form);
		mStatusView = findViewById(R.id.rej_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.rej_tb_fragment);
		super.initTagBarcodeInput();
		startQtyView = (TextView) findViewById(R.id.rej_start_qty);
		rejQtyView = (TextView) findViewById(R.id.rej_rej_qty);
		startQtyView.setText("开批数量");rejQtyView.setText("拒料数量");
		rejMachinesLinearLayout = (LinearLayout) findViewById(R.id.rej_machines);
		submitAllButton = (Button) findViewById(R.id.rej_submit_all);
		currmachname = getResources().getString(R.string.non_machine);
		// rejDefectListView = (ListView)
		// findViewById(R.id.rej_defect_list_view);
		rejDefectListLinearLayout = (LinearLayout) findViewById(R.id.rej_defect_list_view2);
		stepSpinner = (Spinner) findViewById(R.id.rej_step_spinner);
		stepNameTitle = (TextView) findViewById(R.id.rej_step_name_title);
		stepArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, stepList);
		stepArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		stepSpinner.setAdapter(stepArrayAdapter);
		stepSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				final String selected = stepArrayAdapter.getItem(position);
				if (!lastStepNameSelected.equals(selected) && qTask == null) {
					checkUnsavedData();
					if (!batchSubmitDefects.isEmpty()) {
						String machID = currmachname;
						AlertDialog.Builder builder = new AlertDialog.Builder(RejActivity.this);
						builder.setTitle("有未提交的修改，需要提交吗？");
						String result = formDefectStr();
						builder.setMessage(machID + ": " + result);
						builder.setPositiveButton(getResources().getString(R.string.button_submit), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								showProgress(true);
								qTask = new QueryTask();
								qTask.execute("batchLogDefects");
							}
						});
						builder.setNegativeButton(getResources().getString(R.string.ignore), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								lastStepNameSelected = selected;
								if (selected.equals(global.getAoLot().getCurrentStep().getStepName())) {
									previousStepRej = false;
									stepNameTitle.setText("当前站扔缺陷");
									qTask = new QueryTask();
									qTask.execute("getMainDetailByALotNumber");
								} else {
									previousStepRej = true;
									stepNameTitle.setText("前站扔缺陷");
									qTask = new QueryTask();
									qTask.execute("changeStep", selected);
								}
							}
						});
						builder.show();
					} else {
						lastStepNameSelected = selected;
						if (selected.equals(global.getAoLot().getCurrentStep().getStepName())) {
							previousStepRej = false;
							stepNameTitle.setText("当前站扔缺陷");
							qTask = new QueryTask();
							qTask.execute("getMainDetailByALotNumber");
						} else {
							previousStepRej = true;
							stepNameTitle.setText("前站扔缺陷");
							qTask = new QueryTask();
							qTask.execute("changeStep", selected);
						}
					}
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
//		stepSpinner.setVisibility(View.GONE);
//		stepNameTitle.setVisibility(View.GONE);

		submitAllButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
//				for (int position = 0; position < viewHolderList.size(); position++) {
//					String changeStr = "" + viewHolderList.get(position).qtyChange.getText();
//					if (!CommonUtility.isEmpty(changeStr) && !changeStr.equals("0") && CommonUtility.isValidNumber(changeStr)) {
//						String strrejtype = "" + viewHolderList.get(position).rejDesc.getText();
//						strrejtype = strrejtype.substring(strrejtype.indexOf("(") + 1, strrejtype.indexOf(")"));
//						batchSubmitDefects.put(strrejtype, changeStr);
//					}
//				}
				checkUnsavedData();
				if (null == qTask && !batchSubmitDefects.isEmpty()) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("batchLogDefects");
				}
			}
		});

	}

	public class QueryTask extends AsyncTask<String, Void, BaseException> {
		DataCollection queryResult = null;
		String cmdName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("getMainDetailByALotNumber") || cmdName.equals("getMainDetailByRFIDTag")) {
					getMainDetail();
				} else if (cmdName.equals("logDefects")) {
					String strrejtype = params[1];
					String changeStr = params[2];
					logDefects(strrejtype, changeStr);
				} else if (cmdName.equals("batchLogDefects")) {
					batchLogDefects();
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
				} else if (cmdName.equals("changeStep")) {
					String step = params[1];
					changeStep(step);
				}
			} catch (BaseException e) {
				return e;
			}
			return null;
		}

		private void batchLogDefects() throws BaseException {
			String machID = currmachname;
			if (currmachname.equals(getResources().getString(R.string.non_machine))) {
				machID = "";
			}
			String api = "getCurrentDefectSummary(attributes='procName, stepSeq, stepName, defectType, defectQty, machId, category, description',lotNumber='"
					+ global.getAoLot().getAlotNumber() + "',stepName='" + global.getAoLot().getCurrentStep().getStepName() + "')";

			queryResult = apiExecutorQuery.query(classname, "batchLogDefects", api);
			int inputQty = 0;
			for (String key : batchSubmitDefects.keySet()) {
				int temp = Integer.parseInt(batchSubmitDefects.get(key));
				inputQty += temp;
			}
			int totQty = 0;
			if (!CommonUtility.isEmpty(queryResult)) {
				totQty = Integer.parseInt(queryResult.get(0).get(4)) + inputQty;
			}
			int startQty = Integer.parseInt(global.getAoLot().getCurrentStep().getStartQty());
			if (totQty > startQty) {
				throw new RfidException("所输入的缺陷数量 " + inputQty + " 大于来料总数 " + startQty + "，请检查。", classname, "logDefects", api);
			}

			api = "getCurrentStepContext(attributes='procName,stepSeq,stepName,startQty,startTime,startUserId,lotStatus,devcNumber,pkgCode', lotNumber='"
					+ global.getAoLot().getAlotNumber() + "')";
			queryResult = apiExecutorQuery.query(classname, "batchLogDefects", api);
			String currentStep = queryResult.get(0).get(2);
			if (!global.getAoLot().getCurrentStep().getStepName().equals(currentStep)) {
				throw new RfidException("step [" + global.getAoLot().getAlotNumber() + "] 已结束，不能再扔缺陷。", classname, "logDefects", api);
			}
			String defectStr = formDefectStr();
			api = "logDefects(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + global.getAoLot().getAlotNumber() + "',stepName='"
					+ global.getAoLot().getCurrentStep().getStepName() + "',defectDict=" + defectStr + ",machId='" + machID
					+ "',magId='" + global.getCarrierID() + "',source='RFIDAPP'";
			if (previousStepRej) {
				api = api + ",stepDfctCombOvrdFlag='Y')";
			} else {
				api = api + ")";
			}
			logf(api);
			apiExecutorUpdate.transact(classname, "batchLogDefects", api);
			api = "getCurrentDefectSummary(attributes='machId,defectType,defectQty',lotNumber='" + alotNumber + "',stepName='"
					+ global.getAoLot().getCurrentStep().getStepName() + "')";
			defectCounts = apiExecutorQuery.query(classname, "batchLogDefects", api);
		}

		private void logDefects(String strrejtype, String changeStr) throws BaseException {
			String machID = currmachname;
			if (currmachname.equals(getResources().getString(R.string.non_machine))) {
				machID = "";
			}
			String api = "getCurrentDefectSummary(attributes='procName, stepSeq, stepName, defectType, defectQty, machId, category, description',lotNumber='"
					+ global.getAoLot().getAlotNumber() + "',stepName='" + global.getAoLot().getCurrentStep().getStepName() + "')";

			queryResult = apiExecutorQuery.query(classname, "logDefects", api);
			int inputQty = Integer.parseInt(changeStr);
			int totQty = 0;
			if (!CommonUtility.isEmpty(queryResult)) {
				totQty = Integer.parseInt(queryResult.get(0).get(4)) + inputQty;
			}
			int startQty = Integer.parseInt(global.getAoLot().getCurrentStep().getStartQty());
			if (totQty > startQty) {
				throw new RfidException("所输入的缺陷数量 " + inputQty + " 大于来料总数 " + startQty + "，请检查。", classname, "logDefects", api);
			}

			api = "getCurrentStepContext(attributes='procName,stepSeq,stepName,startQty,startTime,startUserId,lotStatus,devcNumber,pkgCode', lotNumber='"
					+ global.getAoLot().getAlotNumber() + "')";
			queryResult = apiExecutorQuery.query(classname, "logDefects", api);
			String currentStep = queryResult.get(0).get(2);
			if (!global.getAoLot().getCurrentStep().getStepName().equals(currentStep)) {
				throw new RfidException("step [" + global.getAoLot().getAlotNumber() + "] 已结束，不能再扔缺陷。", classname, "logDefects", api);
			}
			api = "logDefects(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + global.getAoLot().getAlotNumber() + "',stepName='"
					+ global.getAoLot().getCurrentStep().getStepName() + "',defectDict={'" + strrejtype + "':" + changeStr + "},machId='" + machID + "',magId='"
					+ global.getCarrierID() + "',source='RFIDAPP'";
			if (previousStepRej) {
				api = api + ",stepDfctCombOvrdFlag='Y')";
			} else {
				api = api + ")";
			}
			logf(api);
			apiExecutorUpdate.transact(classname, "logDefects", api);
			api = "getCurrentDefectSummary(attributes='machId,defectType,defectQty',lotNumber='" + alotNumber + "',stepName='"
					+ global.getAoLot().getCurrentStep().getStepName() + "')";
			defectCounts = apiExecutorQuery.query(classname, "logDefects", api);			
		}

		private void getMainDetail() throws BaseException {
			stepList.clear();
			String api;
			if (cmdName.equals("getMainDetailByRFIDTag")) {
				// getCarrierAttributes,Parameters: attributes
				// (pkgCode,carrierId,carrierName,carrierType,carrierLayer,receiptDate,status,lotNumber,carrierGroupId,location,cassetteLotNumber,cassetteOrMagazine),pkgCode,carrierId,status,
				// lotNumber,carrierGroupId,location,cassetteLotNumber,cassetteOrMagazine
				api = "getCarrierAttributes(carrierId='" + tagUUID
						+ "', attributes='status,pkgCode,receiptDate,lotNumber,carrierName,carrierType,carrierLayer,carrierGroupId')";
				queryResult = apiExecutorQuery.query(classname, "getMainDetail", api);
				String carrierName = "";
				if (!CommonUtility.isEmpty(queryResult)) {
					if (!CommonUtility.isEmpty(queryResult.get(0).get(3)) && !"None".equalsIgnoreCase(queryResult.get(0).get(3))) {
						alotNumber = queryResult.get(0).get(3);
					} else {
						carrierName = queryResult.get(0).get(4);
						throw new RfidException("该RFID标签[" + carrierName + "]未被绑定到lot", classname, "getMainDetail", api);
					}
				} else {
					throw new RfidException("该RFID标签[" + carrierName + "]不存在", classname, "getMainDetail", api);
				}
			}
			if (!CommonUtility.isEmpty(alotNumber)) {
				AOLot aoLot = new AOLot();
				aoLot.setAlotNumber(alotNumber);
				global.setAoLot(aoLot);
			} else {
				throw new RfidException("请输入lot号", classname, "getMainDetail", "");
			}

			// getCurrentStepContext,Parameters: attributes
			// (lotNumber,devcNumber,masterProcess,pkgKit,lotStatus,lotQty,devcName,devcSubset,palCode,prodLine,pkgCode,department,mpqFactor,pkgType,pkgName,aql,trakRouting,mooNumber,lotAge,
			// procName,stepSeq,stepName,queueTime,queueAge,startTime,startQty,effectiveStartQty,rejQty,adjQty,startUserId,trakOper,subGroup,testId,stepClass,displaySequence,operation,engineeringMemo,
			// probeDate,subArea,area,pkgName,devcDescription,sentRejQty
			// <secondary lot
			// attribute>,...),lotNumber,devcNumber,masterProcess,lotStatusList,devcName,devcSubset,palCode,prodLine,pkgCode,department,stepName,operation,operationList,mooNumber,subArea,area,pkgName,devcDescription
			// <secondary lot attribute filter>,...
			api = "getCurrentStepContext(attributes='procName,stepSeq,stepName,startQty,startTime,startUserId,lotStatus,devcNumber,pkgCode,rejQty', lotNumber='"
					+ alotNumber + "')";
			queryResult = apiExecutorQuery.query(classname, "getMainDetail", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				Step step = new Step();
				step.setProcName(queryResult.get(0).get(0));
				step.setStepSeq(queryResult.get(0).get(1));
				step.setStepName(queryResult.get(0).get(2));
				step.setStartQty(queryResult.get(0).get(3));
				step.setStartTime(queryResult.get(0).get(4));
				step.setStartUserId(queryResult.get(0).get(5));
				step.setLotStatus(queryResult.get(0).get(6));
				step.setDevcNumber(queryResult.get(0).get(7));
				step.setPkgCode(queryResult.get(0).get(8));
				step.setRejQty(queryResult.get(0).get(9));
				global.getAoLot().setCurrentStep(step);
			} else {
				throw new RfidException(alotNumber + "不在step", classname, "MainDetail", api);
			}

			if (null != global.getAoLot().getCurrentStep()) {
				// getLotAttributes,Parameters: attributes
				// (lotNumber,devcNumber,masterProcess,pkgKit,lotStatus,startQty,effectiveStartQty,lotQty,rejQty,endQty,startDate,endDate,
				// devcName,devcSubset,palCode,prodLine,pkgCode,department,
				// operation,mpqFactor,pkgType,pkgName,aql,mooNumber,lotAge,probeDate,priorTargetItem,holdDuration,accumRejQty,currBalanceDue,
				// orderAdjustQty,origOrderQty,testStartQty,siteLocation,
				// componentId,cqiId,combinedStartQty,urgencyCode,scheduleId,aqt,<secondary
				// lot
				// attribute>,...),lotNumber,devcNumber,masterProcess,lotStatusList,devcName,devcSubset,palCode,prodLine,pkgCode,department,
				// operationList,startFromDate,urgencyCode,scheduleId,<secondary
				// lot attribute filter>,...
				api = "getLotAttributes(attributes='devcNumber,pkgCode,waferLotNumber,traceCode',lotNumber='" + alotNumber + "')";
				queryResult = apiExecutorQuery.query(classname, "getMainDetail", api);
				if (!CommonUtility.isEmpty(queryResult)) {
					global.getAoLot().setDevcNumber(queryResult.get(0).get(0));
					global.getAoLot().setPkgCode(queryResult.get(0).get(1));
					global.getAoLot().setWaferLotNumber(queryResult.get(0).get(2));
					global.getAoLot().setTraceCode(queryResult.get(0).get(3));
				}
			}

			if (null != global.getAoLot().getCurrentStep()) {
				// getLotMachineHistory,Parameters: attributes
				// (lotNumber,procName,stepSeq,stepName,machId,machName,machType,startTime,endTime,machQty,rejQty,startUserId,endUserId,testerRecipe,
				// handlerSetup,temperature),lotNumber,stepName,machId,machName,machType,lotStatusList,startDate,endDate
				// getMachineIds
				// distinct mach_id from ALOT_MACH_HISTORIES
				api = "getLotMachineHistory(attributes='machId',lotNumber='" + alotNumber + "',stepName='" + global.getAoLot().getCurrentStep().getStepName()
						+ "')";
				machineIDs = apiExecutorQuery.query(classname, "getMainDetail", api);
			}

			if (null != global.getAoLot().getCurrentStep()) {
				/*
				 * MatlMgrAPI = "getStepDnamDefectAttr(attributes='category,defectType,description'," & _ "devcName='" & devcName & "',stepName='" & stepName & "')" reply =
				 * MatlMgrRequest_Query(MatlMgrAPI)
				 * 
				 * If glMatlMgrReply(1, 0) = 0 Then
				 * 
				 * MatlMgrAPI = "getStepDeptDefectAttr(attributes='category,defectType,description'," & _ "department='" & glDepartment & "',stepName='" & stepName & "')" reply =
				 * MatlMgrRequest_Query(MatlMgrAPI)
				 * 
				 * If glMatlMgrReply(1, 0) = 0 Then
				 * 
				 * MatlMgrAPI = "getDefectAttributes(attributes='category,defectType,description'," & _ "excDevcName='" & devcName & "',stepName='" & stepName & "')" reply =
				 * MatlMgrRequest_Query(MatlMgrAPI) End If End If
				 */

				api = "getStepDnamDefectAttr(attributes='description,defectType',devcName='" + global.getAoLot().getCurrentStep().getDevcNumber()
						+ "',stepName='" + global.getAoLot().getCurrentStep().getStepName() + "')";
				defectList = apiExecutorQuery.query(classname, "getMainDetail", api);
				if (CommonUtility.isEmpty(defectList)) {
					api = "getStepDeptDefectAttr(attributes='description,defectType',department='" + global.getUser().getDepartment() + "',stepName='"
							+ global.getAoLot().getCurrentStep().getStepName() + "')";
					defectList = apiExecutorQuery.query(classname, "getMainDetail", api);
					if (CommonUtility.isEmpty(defectList)) {
						api = "getDefectAttributes(attributes='description,defectType',excDevcName='" + global.getAoLot().getCurrentStep().getDevcNumber()
								+ "',stepName='" + global.getAoLot().getCurrentStep().getStepName() + "')";
						defectList = apiExecutorQuery.query(classname, "getMainDetail", api);
					}
				}

				api = "getCurrentDefectSummary(attributes='machId,defectType,defectQty',lotNumber='" + alotNumber + "',stepName='"
						+ global.getAoLot().getCurrentStep().getStepName() + "')";
				defectCounts = apiExecutorQuery.query(classname, "getMainDetail", api);

				lastStepNameSelected = global.getAoLot().getCurrentStep().getStepName();
				api = "getLotStepHistory(attributes='procName,stepName',lotNumber='" + alotNumber + "')";
				DataCollection histSteps = apiExecutorQuery.query(classname, "getMainDetail", api);
				if (!CommonUtility.isEmpty(histSteps)) {
					for (ArrayList<String> temp : histSteps) {
						if (temp.get(0).equals(global.getAoLot().getCurrentStep().getProcName())) {
							stepList.add(temp.get(1));
						}
					}
				}
			}

			// if (CommonUtility.isEmpty(errorMsg)) {
			// rejTotalByMach.put(getResources().getString(R.string.non_machine), 0);
			// if (!CommonUtility.isEmpty(machineIDs)) {
			// for (ArrayList<String> temp : machineIDs) {
			// rejTotalByMach.put(temp.get(0), 0);
			// }
			// }
			// for (int y = 0; y < defectCounts.size(); y++) {
			// String machId = defectCounts.get(y).get(0);
			// if (machId.equals("None")) {
			// machId = getResources().getString(R.string.non_machine);
			// }
			// rejTotalByMach.put(machId, rejTotalByMach.get(machId) + Integer.parseInt(defectCounts.get(y).get(2)));
			// }
			// }
		}

		private void changeStep(String step) throws BaseException {
			if (previousStepRej) {
				String api = "getDefectAttributes(attributes='description,defectType',stepName='" + step + "',excDevcName='" + global.getAoLot().getDevcNumber() + "')";
				defectList = apiExecutorQuery.query(classname, "changeStep", api);
			} else {
				String api = "getStepDnamDefectAttr(attributes='description,defectType',devcName='" + global.getAoLot().getCurrentStep().getDevcNumber()
						+ "',stepName='" + global.getAoLot().getCurrentStep().getStepName() + "')";
				defectList = apiExecutorQuery.query(classname, "getMainDetail", api);
				if (CommonUtility.isEmpty(defectList)) {
					api = "getStepDeptDefectAttr(attributes='description,defectType',department='" + global.getUser().getDepartment() + "',stepName='"
							+ global.getAoLot().getCurrentStep().getStepName() + "')";
					defectList = apiExecutorQuery.query(classname, "getMainDetail", api);
					if (CommonUtility.isEmpty(defectList)) {
						api = "getDefectAttributes(attributes='description,defectType',excDevcName='" + global.getAoLot().getCurrentStep().getDevcNumber()
								+ "',stepName='" + global.getAoLot().getCurrentStep().getStepName() + "')";
						defectList = apiExecutorQuery.query(classname, "getMainDetail", api);
					}
				}
			}
//			api = "getCurrentDefectSummary(attributes='machId,defectType,defectQty',lotNumber='" + alotNumber + "',stepName='" + global.getAoLot().getCurrentStep().getStepName() + "')";
//			defectCounts = apiExecutorQuery.query(classname, "changeStep", api);
		}

		@Override
		protected void onPostExecute(final BaseException e) {
			qTask = null;
			showProgress(false);
			tagBarcodeInput.requestFocus();
			if (null == e) {
				// query success
				if (cmdName.equals("getMainDetailByALotNumber") || cmdName.equals("getMainDetailByRFIDTag")) {
					alotNumberTextView.setText(alotNumber);
					startQtyView.setText("开批数量：" + global.getAoLot().getCurrentStep().getStartQty());
					rejQtyView.setText("拒料数量：" + global.getAoLot().getCurrentStep().getRejQty());
					refreshRejListView(true);
				} else if (cmdName.equals("logDefects")) {
					Toast.makeText(RejActivity.this, "Reject success", Toast.LENGTH_SHORT).show();
					refreshRejListView(false);
				} else if (cmdName.equals("batchLogDefects")) {
					Toast.makeText(RejActivity.this, "Reject success", Toast.LENGTH_SHORT).show();
					refreshRejListView(false);
				} else if (cmdName.equals("changeStep")) {
					refreshRejListView(false);
				}
			} else {
				// query fail
				logf(e.toString());
				if (cmdName.equals("getMainDetailByALotNumber") || cmdName.equals("getMainDetailByRFIDTag")) {
					rejDefectListLinearLayout.removeAllViews();
					rejMachinesLinearLayout.removeAllViews();
					defectData.clear();
					viewHolderList.clear();
					rejTotalByMach.clear();
					stepArrayAdapter.notifyDataSetChanged();
					batchSubmitDefects.clear();
				}
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(RejActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(RejActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}
	}

	@SuppressLint("InflateParams")
	private void refreshRejListView(boolean isInit) {
		rejDefectListLinearLayout.removeAllViews();
		rejMachinesLinearLayout.removeAllViews();
		defectData.clear();
		viewHolderList.clear();
		rejTotalByMach.clear();
		stepArrayAdapter.notifyDataSetChanged();
		batchSubmitDefects.clear();
		if (isInit) {
			stepNameTitle.setText("当前站扔缺陷");
			currmachname = getResources().getString(R.string.non_machine);
			stepSpinner.setSelection(stepList.indexOf(global.getAoLot().getCurrentStep().getStepName()));
		}
		for (int i = 0; i < defectList.size(); i++) {
			String currqty = "0";
			for (int y = 0; y < defectCounts.size(); y++) {
				String machId = defectCounts.get(y).get(0);
				if (machId.equals("None")) {
					machId = getResources().getString(R.string.non_machine);
				}
				if (machId.equals(currmachname)) {
					if (defectCounts.get(y).get(1).equals(defectList.get(i).get(1))) {
						currqty = defectCounts.get(y).get(2);
						break;
					}
				}
			}
			Map<String, Object> temp = new HashMap<String, Object>();
			try {
				temp.put("rejDesc", URLDecoder.decode(defectList.get(i).get(0).replace("\\x", "%"), "GB2312") + " (" + defectList.get(i).get(1) + ")");
			} catch (UnsupportedEncodingException e) {
			}
			temp.put("qtyTotal", "" + currqty);
//			temp.put("qtyChange", "");
			defectData.add(temp);
		}

		// adapter = new RejLineAdapter(RejActivity.this);
		// rejDefectListView.setAdapter(adapter);

		for (int i = 0; i < defectData.size(); i++) {
			Map<String, Object> temp = defectData.get(i);
			ViewHolder vh = new ViewHolder();
			LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.defect_list_items, null);
			vh.rejDesc = (TextView) l.findViewById(R.id.rejDescTextView);
			vh.rejDesc.setText((String) temp.get("rejDesc"));
			vh.plusBtn = (Button) l.findViewById(R.id.rejPlusButton);
			vh.plusBtn.setOnClickListener(new PlusButtonOnClickListener(i));
			vh.qtyChange = (EditText) l.findViewById(R.id.rejQtyChange);
			vh.qtyChange.addTextChangedListener(new QtyChangeTextWatcher());
//			vh.qtyChange.setText((String) temp.get("qtyChange"));
			vh.minusBtn = (Button) l.findViewById(R.id.rejMinusButton);
			vh.minusBtn.setOnClickListener(new MinusButtonOnClickListener(i));
			vh.submitBtn = (Button) l.findViewById(R.id.rejSubmitButton);
			vh.submitBtn.setOnClickListener(new SubmitButtonOnClickListener(i));
			vh.qtyTotal = (TextView) l.findViewById(R.id.rejQtyTotal);
			vh.qtyTotal.setText((String) temp.get("qtyTotal"));
			viewHolderList.add(vh);
			rejDefectListLinearLayout.addView(l);
		}

		rejTotalByMach.put(getResources().getString(R.string.non_machine), 0);
		if (!CommonUtility.isEmpty(machineIDs)) {
			for (ArrayList<String> temp : machineIDs) {
				rejTotalByMach.put(temp.get(0), 0);
			}
		}
		for (int y = 0; y < defectCounts.size(); y++) {
			String machId = defectCounts.get(y).get(0);
			if (machId.equals("None")) {
				machId = getResources().getString(R.string.non_machine);
			}
			rejTotalByMach.put(machId, rejTotalByMach.get(machId) + Integer.parseInt(defectCounts.get(y).get(2)));
		}

		for (String machId : rejTotalByMach.keySet()) {
			Button button = new Button(RejActivity.this);
			button.setText(machId + " [" + rejTotalByMach.get(machId) + "]");
			if (currmachname.equals(machId)) {
				button.setTextColor(getResources().getColor(R.color.text_highlight_color));
			} else {
				button.setTextColor(getResources().getColor(R.color.bg_black));
			}
			button.setOnClickListener(new MachButtonOnClickListener(machId));
			rejMachinesLinearLayout.addView(button);
		}
	}

	/*
	 * Fail to use listview private class RejLineAdapter extends BaseAdapter {
	 * 
	 * private LayoutInflater mInflater; private ViewHolder holder = null; private int index = -1;
	 * 
	 * public RejLineAdapter(Context context) { this.mInflater = LayoutInflater.from(context); }
	 * 
	 * @Override public int getCount() { return defectList.size(); }
	 * 
	 * @Override public Object getItem(int arg0) { return null; }
	 * 
	 * @Override public long getItemId(int arg0) { return 0; }
	 * 
	 * @Override public View getView(final int position, View convertView, ViewGroup parent) {
	 * 
	 * if (convertView == null) { holder = new ViewHolder(); convertView = mInflater.inflate(R.layout.defect_list_items, null); holder.rejDesc = (TextView) convertView
	 * .findViewById(R.id.rejDescTextView); holder.plusBtn = (Button) convertView .findViewById(R.id.rejPlusButton); holder.qtyChange = (EditText) convertView .findViewById(R.id.rejQtyChange);
	 * holder.minusBtn = (Button) convertView .findViewById(R.id.rejMinusButton); holder.submitBtn = (Button) convertView .findViewById(R.id.rejSubmitButton); holder.qtyTotal = (TextView) convertView
	 * .findViewById(R.id.rejQtyTotal); convertView.setTag(holder); } else { holder = (ViewHolder) convertView.getTag(); }
	 * 
	 * holder.rejDesc.setText(position + " " + (String) mData.get(position).get("rejDesc")); holder.plusBtn.setOnClickListener(new OnClickListener() {
	 * 
	 * @Override public void onClick(View arg0) { int qtyChange = Integer.parseInt((String) mData.get( position).get("qtyChange")); qtyChange++; mData.get(position).put("qtyChange", "" + qtyChange);
	 * adapter.notifyDataSetChanged(); } }); String qtyChangeStr = (String) mData.get(position).get("qtyChange"); holder.qtyChange.setText(qtyChangeStr); if (!qtyChangeStr.equals("0")) {
	 * holder.qtyChange.setTextColor(getResources().getColor( R.color.text_highlight_color)); } else { holder.qtyChange.setTextColor(getResources().getColor( R.color.text_color)); }
	 * 
	 * holder.qtyChange.setOnTouchListener(new OnTouchListener() {
	 * 
	 * @Override public boolean onTouch(View view, MotionEvent event) { if (event.getAction() == MotionEvent.ACTION_UP) { index = position; } return false; } });
	 * 
	 * holder.qtyChange.addTextChangedListener(new TextWatcher() {
	 * 
	 * @Override public void afterTextChanged(Editable arg0) { }
	 * 
	 * @Override public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
	 * 
	 * @Override public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { mData.get(position).put("qtyChange", "" + arg0); } });
	 * 
	 * holder.qtyChange.clearFocus(); if (index != -1 && index == position) { holder.qtyChange.requestFocus(); }
	 * 
	 * holder.minusBtn.setOnClickListener(new View.OnClickListener() {
	 * 
	 * @Override public void onClick(View v) { int qtyChange = Integer.parseInt((String) mData.get( position).get("qtyChange")); qtyChange--; mData.get(position).put("qtyChange", "" + qtyChange);
	 * adapter.notifyDataSetChanged(); } }); holder.submitBtn.setOnClickListener(new View.OnClickListener() {
	 * 
	 * @Override public void onClick(View v) { } }); holder.qtyTotal.setText((String) mData.get(position) .get("qtyTotal"));
	 * 
	 * return convertView; }
	 * 
	 * }
	 */

	public String formDefectStr() {
		StringBuilder sbuilder = new StringBuilder();
		for (String key : batchSubmitDefects.keySet()) {
			sbuilder.append(",'").append(key).append("':").append(batchSubmitDefects.get(key));
		}
		String temp = sbuilder.toString();
		if (temp.length() > 1) {
			return "{" + temp.substring(1) + "}";
		} else {
			return "";
		}
	}

	private final class ViewHolder {
		public TextView rejDesc;
		public Button plusBtn;
		public EditText qtyChange;
		public Button minusBtn;
		public Button submitBtn;
		public TextView qtyTotal;
	}

	private class PlusButtonOnClickListener implements OnClickListener {
		int position;

		PlusButtonOnClickListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View arg0) {
			String s = viewHolderList.get(position).qtyChange.getText().toString();
			int change = 0;
			if (CommonUtility.isEmpty(s)) {
				change = 1;
			} else {
				change = Integer.parseInt("" + s);
				change++;
			}
			viewHolderList.get(position).qtyChange.setText("" + change);
		}
	}

	private class MinusButtonOnClickListener implements OnClickListener {
		int position;

		MinusButtonOnClickListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View arg0) {
			String s = viewHolderList.get(position).qtyChange.getText().toString();
			int change = 0;
			if (CommonUtility.isEmpty(s)) {
				change = -1;
			} else {
				change = Integer.parseInt("" + s);
				change--;
			}
			viewHolderList.get(position).qtyChange.setText("" + change);
		}
	}

	private class SubmitButtonOnClickListener implements OnClickListener {
		int position;

		SubmitButtonOnClickListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View arg0) {
			String strrejtype = "" + viewHolderList.get(position).rejDesc.getText();
			strrejtype = strrejtype.substring(strrejtype.indexOf("(") + 1, strrejtype.indexOf(")"));
			String changeStr = "" + viewHolderList.get(position).qtyChange.getText();
			if (CommonUtility.isValidNumber(changeStr)) {
				if (qTask == null) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute(new String[] { "logDefects", strrejtype, changeStr });
				}
			}
		}
	}

	private class MachButtonOnClickListener implements OnClickListener {
		String rejDesc;

		MachButtonOnClickListener(String rejDesc) {
			this.rejDesc = rejDesc;
		}

		@Override
		public void onClick(View arg0) {
			RejActivity.this.currmachname = this.rejDesc;
			refreshRejListView(false);
		}
	}
	
	private class QtyChangeTextWatcher implements TextWatcher {

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}

		@Override
		public void afterTextChanged(Editable s) {
			totalQtyChange = 0;
			for (ViewHolder vh : viewHolderList) {
				String changeStr = vh.qtyChange.getText().toString();
				if (!CommonUtility.isEmpty(changeStr) && CommonUtility.isValidNumber(changeStr)) {
					totalQtyChange += Integer.parseInt(changeStr);
				}
			}
			submitAllButton.setText(RejActivity.this.getResources().getString(R.string.submit_all) + " [" + totalQtyChange + "]");
		}
		
	}

	@Override
	public void onBackPressed() {
		checkUnsavedData();
		if (!batchSubmitDefects.isEmpty()) {
			String machID = currmachname;
			AlertDialog.Builder builder = new AlertDialog.Builder(RejActivity.this);
			builder.setTitle("有未提交的修改，需要提交吗？");
			String result = formDefectStr();
			builder.setMessage(machID + ": " + result);
			builder.setPositiveButton(getResources().getString(R.string.button_submit), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("batchLogDefects");
				}
			});
			builder.setNegativeButton(getResources().getString(R.string.ignore), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//RejActivity.super.onBackPressed();
					finish();
				}
			});
			builder.show();
		} else {
			//super.onBackPressed();
			finish();
		}
	}
	
	private void checkUnsavedData() {
		batchSubmitDefects.clear();
		for (int position = 0; position < viewHolderList.size(); position++) {
			String changeStr = "" + viewHolderList.get(position).qtyChange.getText();
			if (!CommonUtility.isEmpty(changeStr) && !changeStr.equals("0") && CommonUtility.isValidNumber(changeStr)) {
				String strrejtype = "" + viewHolderList.get(position).rejDesc.getText();
				strrejtype = strrejtype.substring(strrejtype.indexOf("(") + 1, strrejtype.indexOf(")"));
				batchSubmitDefects.put(strrejtype, changeStr);
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
		if (!CommonUtility.isEmpty(global.getCarrierID())) {
			setTagId(global.getCarrierID());
		}
		if (null != global.getAoLot()) {
			setBarcodeInput(global.getAoLot().getAlotNumber());
		}
		super.onResume();
		tagBarcodeInput.requestFocus();
	}

	public void setBarcodeInput(String alotNumber) {
		this.alotNumber = alotNumber;
		if (null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("getMainDetailByALotNumber");
		}
	}

	public void startScanBarcode() {
		log("Rej startScanBarcode");
		if (qTask == null) {
			tagBarcodeInput.setText("");
			qTask = new QueryTask();
			qTask.execute("scanBarcode");
		}
	}

	public void setTagId(String tagId) {
		log("Rej setTagId");
		tagUUID = tagId;
		if (null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("getMainDetailByRFIDTag");
		}
	}

}
