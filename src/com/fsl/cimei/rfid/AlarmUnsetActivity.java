package com.fsl.cimei.rfid;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;

public class AlarmUnsetActivity extends BaseActivity {

	private QueryTask qTask = null;
	private final String classname = "AlarmUnset";
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
	private String selectedMachID = "";
	private Button startMachButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alarm_unset);
		mFormView = findViewById(R.id.alarm_unset_form);
		mStatusView = findViewById(R.id.alarm_unset_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		showProgress(false);
		machTypeSelect = (TextView) findViewById(R.id.alarm_unset_func_mach_type);
		machTypeLine = (LinearLayout) findViewById(R.id.alarm_unset_func_ll1);
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

		locCodeSelect = (TextView) findViewById(R.id.alarm_unset_func_loc_code);
		locCodeLine = (LinearLayout) findViewById(R.id.alarm_unset_func_ll2);
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

		machIdSelect = (TextView) findViewById(R.id.alarm_unset_func_mach_ID);
		machIdLine = (LinearLayout) findViewById(R.id.alarm_unset_func_ll3);
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

		startMachButton = (Button) findViewById(R.id.alarm_unset_submit_button);
		startMachButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (qTask == null && !CommonUtility.isEmpty(selectedMachID)) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("startMach", selectedMachID);
				}
			}
		});

	}

	public class QueryTask extends AsyncTask<String, Void, BaseException> {
		DataCollection queryResult = null;
		String cmdName = "";
		String mach = "";
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
				} else if (cmdName.equals("startMach")) {
					mach = params[1];
					// sendPicEvent($machId, "EVENT_ID=RFID_EVENT TYPE=ALARM_UNSET replyFormat=python machId=$machId")
					String cmd = "EVENT_ID=RFID_EVENT TYPE=ALARM_UNSET replyFormat=python machId=" + mach;
					String api = "sendPicEvent('" + mach + "','" + cmd + "')";
					logf(classname + " " + global.getUser().getUserID() + " " + api);
					apiExecutorUpdate.transact(classname, "startMach", api);
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
				} else if (cmdName.equals("startMach")) {
					toastMsg(mach + " 开机成功");
				}
			} else {
				logf(exception.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(AlarmUnsetActivity.this, exception.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(AlarmUnsetActivity.this, exception.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void getMachTypeAfter() {
			machTypeAlertDialog = new AlertDialog.Builder(AlarmUnsetActivity.this).setTitle("选择机台类型")
					.setItems(machTypeArray, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							machType = "" + machTypeArray[which];
							machTypeSelect.setText(machType);

							if (!oldMachType.equals(machType)) {
								oldLocCode = "";
								locCodeSelect.setText(getResources().getString(R.string.pls_select));
								selectedMachID = "";
								machIdSelect.setText(getResources().getString(R.string.pls_select));
							}
						}
					}).setNegativeButton("取消", null).create();
			machTypeAlertDialog.show();
		}

		private void getLocCodeByMachTypeAfter() {
			locCodeAlertDialog = new AlertDialog.Builder(AlarmUnsetActivity.this).setTitle("选择机台位置").setItems(locCodeArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					locCode = "" + locCodeArray[which];
					locCodeSelect.setText(locCode);
					if (!oldLocCode.equals(locCode)) {
						selectedMachID = "";
						machIdSelect.setText(getResources().getString(R.string.pls_select));
					}
				}
			}).setNegativeButton("取消", null).create();
			locCodeAlertDialog.show();
		}

		private void getMachIDByLocCodeAfter() {
			machIdAlertDialog = new AlertDialog.Builder(AlarmUnsetActivity.this).setTitle("选择机台")
					.setSingleChoiceItems(machIdArray, -1, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							selectedMachID = machIdArray[which];
						}
					}).setPositiveButton(getResources().getString(R.string.button_done), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							machIdSelect.setText(selectedMachID);
						}
					}).setNegativeButton(getResources().getString(R.string.button_cancel), null).create();
			machIdAlertDialog.show();
		}

		private void getMachIDByLocCode() throws BaseException {
			String api = "getMachineAttributes(attributes='machId',locCode='" + locCode + "',machType='" + machType + "')";
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

	}
	
	@Override
	protected void onPause() {
		if (null != qTask) {
			qTask.cancel(true);
		}
		super.onPause();
	}
	
}
