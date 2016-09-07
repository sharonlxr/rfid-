package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.Mach;
import com.fsl.cimei.rfid.exception.BaseException;

public class LotStartSelectMachActivity extends BaseActivity {

	private QueryTask qTask;

	private static final String MACH_NAME = "MACH_NAME";
	private static final String MACH_MODEL = "MACH_MODEL";
	private static final String MACH_TYPE = "MACH_TYPE";
	private Spinner machTypeSpinner;
	private List<String> machTypeList;
	private ArrayAdapter<String> machTypeArrayAdapter;
	private Spinner machModelSpinner;
	private List<String> machModelList;
	private ArrayAdapter<String> machModelArrayAdapter;
	// private Spinner machNameSpinner;
	private String[] machNameArr;
	private ListView machNameListView;
	// private ArrayAdapter<String> machNameArrayAdapter;
	private ListView selectedMachListView;
	private List<HashMap<String, Object>> selectedMachListItem;
	private SimpleAdapter selectedMachAdapter;

	private Button doneButton;
	private Button cancelButton;

	private String machType = "";
	private String machModel = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_start_select_mach_activity);
		mFormView = findViewById(R.id.start_mach_form);
		mStatusView = findViewById(R.id.start_mach_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		machTypeSpinner = (Spinner) findViewById(R.id.lot_start_mach_type_spinner);
		machModelSpinner = (Spinner) findViewById(R.id.lot_start_mach_model_spinner);
		// machNameSpinner = (Spinner) findViewById(R.id.lot_start_mach_name_spinner);
		selectedMachListView = (ListView) findViewById(R.id.lot_start_selected_mach_list);

		if (null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("getMachList");
		}

		machTypeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				machModelSpinner.setSelection(0);
				// machNameSpinner.setSelection(0);
				machType = machTypeSpinner.getSelectedItem().toString();
				if (!CommonUtility.isEmpty(machType) && qTask == null) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute(new String[] { "getMachModelByMachType" });
				}
				arg0.setVisibility(View.VISIBLE);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				arg0.setVisibility(View.VISIBLE);
			}
		});
		machModelSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				// machNameSpinner.setSelection(0);
				machModel = machModelSpinner.getSelectedItem().toString();
				if (!CommonUtility.isEmpty(machModel) && qTask == null) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute(new String[] { "getMachNameByMachModel" });
				}
				arg0.setVisibility(View.VISIBLE);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				arg0.setVisibility(View.VISIBLE);
			}
		});

		selectedMachListItem = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> m = new HashMap<String, Object>();
		m.put(MACH_NAME, getResources().getString(R.string.mach_name));
		m.put(MACH_MODEL, getResources().getString(R.string.mach_model));
		m.put(MACH_TYPE, getResources().getString(R.string.mach_type));
		selectedMachListItem.add(m);

		selectedMachAdapter = new SimpleAdapter(LotStartSelectMachActivity.this, selectedMachListItem, R.layout.lot_start_mach_list_item, new String[] { MACH_NAME,
				MACH_MODEL, MACH_TYPE }, new int[] { R.id.item1, R.id.item2, R.id.item3 });
		selectedMachListView.setAdapter(selectedMachAdapter);
		selectedMachListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if (position != 0) {
					selectedMachListItem.remove(position);
					global.getSelectedMachList().remove(position - 1);
					selectedMachAdapter.notifyDataSetChanged();
				}
				return false;
			}
		});

		doneButton = (Button) findViewById(R.id.lot_start_mach_ok);
		cancelButton = (Button) findViewById(R.id.lot_start_mach_cancel);

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				global.setSelectedMachList(null);
				Intent intent = new Intent(LotStartSelectMachActivity.this, LotStartActivity.class);
				setResult(0, intent);
				finish();
			}
		});
		doneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(LotStartSelectMachActivity.this, LotStartActivity.class);
				setResult(0, intent);
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
				if (cmdName.equals("getMachList")) {
					getMachList();
				} else if (cmdName.equals("getMachModelByMachType")) {
					getMachModelByMachType();
				} else if (cmdName.equals("getMachNameByMachModel")) {
					getMachNameByMachModel();
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
				if (cmdName.equals("getMachList")) {
					getMachListAfter();
				} else if (cmdName.equals("getMachModelByMachType")) {
					getMachModelByMachTypeAfter();
				} else if (cmdName.equals("getMachNameByMachModel")) {
					getMachNameByMachModelAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotStartSelectMachActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotStartSelectMachActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void getMachList() throws BaseException {
			machTypeList.clear();
			machTypeList.add("");
			// String API = "getUserAttributes(attributes='operation',userId='" + global.getUser().getUID() + "')";
			// DataCollection operation = apiExecutor.query(API);
			// if (!CommonUtility.isEmpty(apiExecutor.getMessage())) {
			// errorMsg = apiExecutor.getMessage();
			// }
			// if (CommonUtility.isEmpty(errorMsg) && operation.size() > 0) {
			// String operationResult = operation.get(0).get(0).trim();
			// global.getUser().setOperation(operationResult);
			// if (CommonUtility.isEmpty(global.getUserOperationList())) {
			// API = "getMESParmValues(attributes='parmValue',parmOwnerType='OPER',parmName='otherValidOper',parmOwner='" + global.getUser().getOperation() + "')";
			// DataCollection department = apiExecutor.query(API);
			// if (!CommonUtility.isEmpty(apiExecutor.getMessage())) {
			// errorMsg = apiExecutor.getMessage();
			// } else {
			// List<String> departmentResult = new ArrayList<String>();
			// for (int i = 0; i < department.size(); i++) {
			// departmentResult.add("'" + department.get(i).get(0).replace(",", "','") + "'");
			// }
			// departmentResult.add("'" + global.getUser().getOperation() + "'");
			// global.setUserOperationList(departmentResult.toString());
			// }
			// }
			CommonTrans commonTrans = new CommonTrans();
			commonTrans.checkUserInfo(apiExecutorQuery, global);
			String API = "getMachineAttributes(attributes='machType',stepName='" + global.getAoLot().getCurrentStep().getStepName() + "',operationList = "
					+ global.getUserOperationList() + ",activeMachinesOnly='Y')";
			DataCollection queryResult = apiExecutorQuery.query("LotStartSelectMach", "getMachList", API);
			if (!CommonUtility.isEmpty(queryResult)) {
				for (ArrayList<String> temp : queryResult) {
					machTypeList.add(temp.get(0));
				}
			}
			// }
		}

		private void getMachListAfter() {
			machTypeSpinner.setSelection(0);
			machTypeArrayAdapter = new ArrayAdapter<String>(LotStartSelectMachActivity.this, android.R.layout.simple_spinner_item, machTypeList);
			machTypeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			machTypeSpinner.setAdapter(machTypeArrayAdapter);
		}

		private void getMachModelByMachType() throws BaseException {
			machModelList.clear();
			machModelList.add("");
			String API = "getMachineAttributes(attributes='machModel',machType='" + machType + "',stepName='" + global.getAoLot().getCurrentStep().getStepName()
					+ "',operationList = " + global.getUserOperationList() + ",activeMachinesOnly='Y')";
			DataCollection queryResult = apiExecutorQuery.query("LotStartSelectMach", "getMachModelByMachType", API);
			if (!CommonUtility.isEmpty(queryResult)) {
				for (ArrayList<String> temp : queryResult) {
					machModelList.add(temp.get(0));
				}
			}
		}

		private void getMachModelByMachTypeAfter() {
			machModelSpinner.setSelection(0);
			machModelArrayAdapter = new ArrayAdapter<String>(LotStartSelectMachActivity.this, android.R.layout.simple_spinner_item, machModelList);
			machModelArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			machModelSpinner.setAdapter(machModelArrayAdapter);
		}

		private void getMachNameByMachModel() throws BaseException {
			List<String> machNameList = new ArrayList<String>();
			String API = "getMachineAttributes(attributes='machId',machModel='" + machModel + "',stepName='" + global.getAoLot().getCurrentStep().getStepName()
					+ "',machType='" + machType + "',operationList = " + global.getUserOperationList() + ",activeMachinesOnly='Y')";
			DataCollection queryResult = apiExecutorQuery.query("LotStartSelectMach", "getMachNameByMachModel", API);
			if (!CommonUtility.isEmpty(queryResult)) {
				for (ArrayList<String> temp : queryResult) {
					machNameList.add(temp.get(0));
				}
			}
			API = "getStepDevcMachAttr(attributes='machName,machId',machModel='" + machModel + "',stepName='" + global.getAoLot().getCurrentStep().getStepName()
					+ "',machType='" + machType + "',devcNumber='" + global.getAoLot().getCurrentStep().getDevcNumber() + "',validFlag='Y',operationList = "
					+ global.getUserOperationList() + "," + "activeMachinesOnly='Y')";
			queryResult = apiExecutorQuery.query("LotStartSelectMach", "getMachNameByMachModel", API);
			if (!CommonUtility.isEmpty(queryResult)) {
				for (ArrayList<String> temp : queryResult) {
					machNameList.add(temp.get(0));
				}
			}
			machNameArr = (String[]) machNameList.toArray();
		}

		private void getMachNameByMachModelAfter() {
			// machNameSpinner.setSelection(0);
			// machNameArrayAdapter = new
			// ArrayAdapter<String>(LotStartSelectMachActivity.this,
			// android.R.layout.simple_spinner_item, machNameList);
			// machNameArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			// machNameSpinner.setAdapter(machNameArrayAdapter);
			AlertDialog ad = new AlertDialog.Builder(LotStartSelectMachActivity.this).setTitle("选择机台")
					.setMultiChoiceItems(machNameArr, new boolean[machNameArr.length], new OnMultiChoiceClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						}
					}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							for (int i = 0; i < machNameArr.length; i++) {
								if (machNameListView.getCheckedItemPositions().get(i)) {
									String machID = "" + machNameListView.getAdapter().getItem(i);
									Mach m = new Mach();
									m.setMachType(machType);
									m.setMachModel(machModel);
									m.setMachID(machID);
									global.getSelectedMachList().add(m);
									HashMap<String, Object> map = new HashMap<String, Object>();
									map.put(MACH_NAME, machID);
									map.put(MACH_MODEL, machModel);
									map.put(MACH_TYPE, machType);
									selectedMachListItem.add(map);
								} else {
									machNameListView.getCheckedItemPositions().get(i, false);
								}
							}
							selectedMachAdapter.notifyDataSetChanged();
							dialog.dismiss();
						}
					}).setNegativeButton(getResources().getString(R.string.cancel), null).create();
			machNameListView = ad.getListView();
			ad.show();
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
