package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;

public class OpShiftMachAssignActivity extends BaseActivity {

	private QueryTask qTask = null;

	private TextView machTypeSelect;
	private LinearLayout machTypeLine;
	private AlertDialog machTypeAlertDialog = null;
	private String[] machTypeArray;
	private String machType = "";
	private String oldMachType = "";

	private TextView locCodeSelect;
	private LinearLayout locCodeLine;
	private AlertDialog locCodeAlertDialog = null;
	private String[] locCodeArray;
	private String locCode = "";
	private String oldLocCode = "";

	private TextView machIdSelect;
	private LinearLayout machIdLine;
	private AlertDialog machIdAlertDialog = null;
	private String[] machIdArray;
	// private String machID = "";
	private List<String> selectedMachID = new ArrayList<String>();
	private boolean[] machIdCheckedArray;

	private Button assignButton;
	private Button histButton; // mach_assign_hist_button;

	// private DataCollection assignedMachDC;
	private ListView machOptionListView;
	// private List<HashMap<String, Object>> assignedMachListItem;
	private List<String> machOptionList = new ArrayList<String>();
	private List<String> assignedMachList = new ArrayList<String>();
	private List<String> histMachList = new ArrayList<String>();
	private ArrayAdapter<String> machOptionAdapter;

	// private static final String MACH_NAME = "MACH_NAME";
	// private static final String MACH_MODEL = "MACH_MODEL";
	// private static final String MACH_TYPE = "MACH_TYPE";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_op_shift_mach_assign);
		mFormView = findViewById(R.id.mach_assign_form);
		mStatusView = findViewById(R.id.mach_assign_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		machTypeSelect = (TextView) findViewById(R.id.mach_assign_func_mach_type);
		machTypeLine = (LinearLayout) findViewById(R.id.mach_assign_func_ll1);
		machTypeLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null == qTask && null == machTypeAlertDialog) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("getMachType");
				} else {
					machTypeAlertDialog.show();
				}
			}
		});

		locCodeSelect = (TextView) findViewById(R.id.mach_assign_func_loc_code);
		locCodeLine = (LinearLayout) findViewById(R.id.mach_assign_func_ll2);
		locCodeLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null == qTask && !CommonUtility.isEmpty(machType) && (CommonUtility.isEmpty(oldMachType) || !machType.equals(oldMachType))) {
					oldMachType = machType;
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("getLocCodeByMachType");
				} else {
					if (null != locCodeAlertDialog) {
						locCodeAlertDialog.show();
					}
				}
			}
		});

		machIdSelect = (TextView) findViewById(R.id.mach_assign_func_mach_ID);
		machIdLine = (LinearLayout) findViewById(R.id.mach_assign_func_ll3);
		machIdLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null == qTask && !CommonUtility.isEmpty(locCode) && (CommonUtility.isEmpty(oldLocCode) || !locCode.equals(oldLocCode))) {
					oldLocCode = locCode;
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("getMachIDByLocCode");
				} else {
					if (null != machIdAlertDialog) {
						machIdAlertDialog.show();
					}
				}
			}
		});

		assignButton = (Button) findViewById(R.id.mach_assign_submit_button);
		assignButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (qTask == null) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("assignMach");
				}
			}
		});

		histButton = (Button) findViewById(R.id.mach_assign_hist_button);
		histButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (qTask == null) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("getAssignHistory");
				}
			}
		});

		// assignedMachDC = new DataCollection();
		machOptionListView = (ListView) findViewById(R.id.mach_assign_assigned_mach_list);
		// assignedMachListItem = new ArrayList<HashMap<String, Object>>();
		// HashMap<String, Object> m = new HashMap<String, Object>();
		// m.put(MACH_NAME, getResources().getString(R.string.mach_name));
		// m.put(MACH_MODEL, getResources().getString(R.string.mach_model));
		// m.put(MACH_TYPE, getResources().getString(R.string.mach_type));
		// assignedMachListItem.add(m);
		machOptionAdapter = new ArrayAdapter<String>(OpShiftMachAssignActivity.this, android.R.layout.simple_expandable_list_item_1, machOptionList);
		// assignedMachListSimpleAdapter = new SimpleAdapter(OpShiftMachAssignActivity.this, assignedMachListItem, R.layout.lot_start_mach_list_item, new String[] {
		// MACH_NAME, MACH_MODEL, MACH_TYPE }, new int[] { R.id.item1, R.id.item2, R.id.item3 });
		machOptionListView.setAdapter(machOptionAdapter);
		machOptionListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Toast.makeText(OpShiftMachAssignActivity.this, machOptionList.get(position), Toast.LENGTH_SHORT).show();
				machOptionList.remove(position);
				machOptionAdapter.notifyDataSetChanged();
				return false;
			}
		});
		showProgress(true);
		qTask = new QueryTask();
		qTask.execute("getAssignedMach");
	}

	public class QueryTask extends AsyncTask<String, Void, BaseException> {
		DataCollection queryResult = null;
		String cmdName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("getMachType")) {
					getMachType();
				} else if (cmdName.equals("getLocCodeByMachType")) {
					getLocCodeByMachType();
				} else if (cmdName.equals("getMachIDByLocCode")) {
					getMachIDByLocCode();
				} else if (cmdName.equals("assignMach")) {
					assignMach();
				} else if (cmdName.equals("getAssignedMach")) {
					getAssignedMach();
				} else if (cmdName.equals("getAssignHistory")) {
					getAssignHistory();
				}
			} catch (BaseException e) {
				return e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(final BaseException exception) {
			qTask = null;
			showProgress(false);
			if (exception == null) {
				if (cmdName.equals("getMachType")) {
					getMachTypeAfter();
				} else if (cmdName.equals("getLocCodeByMachType")) {
					getLocCodeByMachTypeAfter();
				} else if (cmdName.equals("getMachIDByLocCode")) {
					getMachIDByLocCodeAfter();
				} else if (cmdName.equals("assignMach")) {
					assignMachAfter();
				} else if (cmdName.equals("getAssignedMach")) {
					getAssignedMachAfter();
				} else if (cmdName.equals("getAssignHistory")) {
					getAssignHistoryAfter();
				}
			} else {
				logf(exception.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(OpShiftMachAssignActivity.this, exception.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(OpShiftMachAssignActivity.this, exception.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void getMachTypeAfter() {
			machTypeAlertDialog = new AlertDialog.Builder(OpShiftMachAssignActivity.this).setTitle("选择机台类型")
					.setItems(machTypeArray, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							machType = "" + machTypeArray[which];
							machTypeSelect.setText(machType);

							if (!oldMachType.equals(machType)) {
								oldLocCode = "";
								locCodeSelect.setText(getResources().getString(R.string.pls_select));
								selectedMachID.clear();
								machIdSelect.setText(getResources().getString(R.string.pls_select));
							}
						}
					}).setNegativeButton("取消", null).create();
			machTypeAlertDialog.show();
		}

		private void getLocCodeByMachTypeAfter() {
			locCodeAlertDialog = new AlertDialog.Builder(OpShiftMachAssignActivity.this).setTitle("选择机台位置").setItems(locCodeArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					locCode = "" + locCodeArray[which];
					locCodeSelect.setText(locCode);
					if (!oldLocCode.equals(locCode)) {
						selectedMachID.clear();
						machIdSelect.setText(getResources().getString(R.string.pls_select));
					}
				}
			}).setNegativeButton("取消", null).create();
			locCodeAlertDialog.show();
		}

		private void getMachIDByLocCodeAfter() {
			machIdCheckedArray = new boolean[machIdArray.length];
			machIdAlertDialog = new AlertDialog.Builder(OpShiftMachAssignActivity.this).setTitle("选择机台")
					.setMultiChoiceItems(machIdArray, machIdCheckedArray, new OnMultiChoiceClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which, boolean isChecked) {
							machIdCheckedArray[which] = isChecked;
						}
						// @Override
						// public void onClick(DialogInterface dialog, int which) {
						// machID = "" + machIdArray[which];
						// machIdSelect.setText(machID);
						// }
					}).setPositiveButton(getResources().getString(R.string.button_done), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							StringBuilder sb = new StringBuilder();
							for (int i = 0; i < machIdArray.length; i++) {
								if (machIdCheckedArray[i]) {
									selectedMachID.add(machIdArray[i]);
									sb.append(",").append(machIdArray[i]);
									if (!machOptionList.contains(machIdArray[i])) {
										machOptionList.add(machIdArray[i]);
									}
								}
							}
							String machID = sb.toString();
							if (!CommonUtility.isEmpty(machID) && machID.startsWith(",")) {
								machID = machID.substring(1);
							}
							machIdSelect.setText(machID);
							machOptionAdapter.notifyDataSetChanged();
						}
					}).setNegativeButton(getResources().getString(R.string.button_cancel), null).create();
			machIdAlertDialog.show();
		}

		private void getAssignedMachAfter() {
			// HashMap<String, Object> m = new HashMap<String, Object>();
			// m.put(MACH_NAME, getResources().getString(R.string.mach_name));
			// m.put(MACH_MODEL, getResources().getString(R.string.mach_model));
			// m.put(MACH_TYPE, getResources().getString(R.string.mach_type));
			// assignedMachListItem.add(m);
			// for (int i = 0; i < assignedMachDC.size(); i++) {
			// ArrayList<String> mach = assignedMachDC.get(i);
			// HashMap<String, Object> map = new HashMap<String, Object>();
			// map.put(MACH_NAME, mach.get(0));
			// map.put(MACH_MODEL, mach.get(1));
			// map.put(MACH_TYPE, mach.get(2));
			// assignedMachListItem.add(map);
			// }
			machOptionAdapter.notifyDataSetChanged();
		}

		private void assignMachAfter() {
			SharedPreferences shared = OpShiftMachAssignActivity.this.getSharedPreferences("RFID-data", MODE_PRIVATE);
			Editor e = shared.edit();
			Set<String> assignedMach = new HashSet<String>();
			for (String mach : assignedMachList) {
				assignedMach.add(mach);
			}
			e.putStringSet("assignedMach", assignedMach);
			e.commit();
			Toast.makeText(OpShiftMachAssignActivity.this, "操作员加载机台成功！", Toast.LENGTH_SHORT).show();
		}

		private void getAssignHistoryAfter() {
			getAssignedMachAfter();
		}

		private void getMachIDByLocCode() throws BaseException {
			String api = "getMachineAttributes(attributes='machId',locCode='" + locCode + "',machStatus='ACTIVE',validMachinesOnly='Y')"; // ,machType='" + machType + "'
			queryResult = apiExecutorQuery.query("OpShiftMachAssign", "getMachIDByLocCode", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				machIdArray = new String[queryResult.size()];
				for (int i = 0; i < queryResult.size(); i++) {
					ArrayList<String> temp = queryResult.get(i);
					machIdArray[i] = temp.get(0);
				}
			}
		}

		private void getLocCodeByMachType() throws BaseException {
			String api = "getMachineAttributes(attributes='locCode',machType='" + machType + "',operation='" + global.getUser().getOperation() + "')";
			queryResult = apiExecutorQuery.query("OpShiftMachAssign", "getLocCodeByMachType", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				locCodeArray = new String[queryResult.size()];
				for (int i = 0; i < queryResult.size(); i++) {
					ArrayList<String> temp = queryResult.get(i);
					locCodeArray[i] = temp.get(0);
				}
			}
		}

		private void getMachType() throws BaseException {
			String operation = null;
			String api = "getUserAttributes(attributes='department,operation',userId='" + global.getUser().getUserID() + "')";
			queryResult = apiExecutorQuery.query("OpShiftMachAssign", "getMachType", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				operation = queryResult.get(0).get(1);
				global.getUser().setOperation(operation);
				api = "getMachineAttributes(attributes='machType',operation='" + global.getUser().getOperation() + "')";
				queryResult = apiExecutorQuery.query("OpShiftMachAssign", "getMachType", api);
				if (!CommonUtility.isEmpty(queryResult)) {
					machTypeArray = new String[queryResult.size()];
					for (int i = 0; i < queryResult.size(); i++) {
						ArrayList<String> temp = queryResult.get(i);
						machTypeArray[i] = temp.get(0);
					}
				}
			}
		}

		private void assignMach() throws BaseException {
			for (String machID : assignedMachList) {
				if (!machOptionList.contains(machID)) {
					String api = "registerOperatorOnMachine(transUserId='RFID',machId='" + machID + "')";
					apiExecutorUpdate.transact("OpShiftMachAssign", "assignMach", api);
				}
			}
			assignedMachList = new ArrayList<String>();
			for (String machID : machOptionList) {
				assignedMachList.add(machID);
				String api = "registerOperatorOnMachine(transUserId='" + global.getUser().getUserID() + "', machId='" + machID + "', hostName='android-"
						+ global.getAndroidSecureID() + ".ap.freescale.net') ";
				apiExecutorUpdate.transact("OpShiftMachAssign", "assignMach", api);
			}
		}

		private void getAssignedMach() throws BaseException {
			CommonTrans commonTrans = new CommonTrans();
			queryResult = commonTrans.getAssignedMachDC(apiExecutorQuery, global);
			assignedMachList = new ArrayList<String>();
			for (int i = 0; i < queryResult.size(); i++) {
				ArrayList<String> mach = queryResult.get(i);
				String machId = mach.get(0);
				assignedMachList.add(machId);
				if (!machOptionList.contains(machId)) {
					machOptionList.add(machId);
				}
			}
		}

		private void getAssignHistory() throws BaseException {
			String api = "getLastAssignedMachinesForUser(userId='" + global.getUser().getUserID() + "')";
			queryResult = apiExecutorQuery.query("OpShiftMachAssign", "getAssignHistory", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				for (ArrayList<String> temp : queryResult) {
					String machId = temp.get(0);
					histMachList.add(machId);
					if (!machOptionList.contains(machId)) {
						machOptionList.add(machId);
					}
				}
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
