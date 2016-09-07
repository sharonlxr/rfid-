package com.fsl.cimei.rfid;

import interfacemgr.genesis.entity.InterfaceMgrSocketConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import app.entity.DataCollection;
import app.utils.login.LoginInfo;
import app.utils.login.genesis.GenesisGateway;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class MachRFIDFunctionActivity extends BaseActivity {

	private QueryTask qTask = null;

	private TextView machIdSelect;
	private LinearLayout machIdLine;
	private AlertDialog machIdAlertDialog = null;
	private String[] machIdArray;
	private String machID = "";

	private ToggleButton toggleButton;
	private boolean rfidEnabled = false;
	private boolean oldRfidEnabled = false;

	private String classname = "MachRFIDFunc";
	private String userID = "";
	private String password = "";
	private LinearLayout loginForm;
	private LinearLayout funcForm;
	private EditText userIDView;
	private EditText passwordView;
	private Button loginButton;
	// private SQLiteDatabase configDB;
	// private ConfigDBHelper configDB;
	public static final String CONFIG_OWNER_TYPE = "MACH_RFID_PRIVILEGE";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mach_rfid_function);
		mFormView = findViewById(R.id.mach_rfid_func_form);
		mStatusView = findViewById(R.id.mach_rfid_func_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		showProgress(false);
		machIdSelect = (TextView) findViewById(R.id.mach_rfid_func_mach_ID);
		machIdLine = (LinearLayout) findViewById(R.id.mach_rfid_func_ll3);
		machIdLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null != machIdAlertDialog) {
					machIdAlertDialog.show();
				} else {
					if (null == qTask) {
						mStatusMessageView.setText(getResources().getString(R.string.loading_data));
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("getAssignedMach");
					}
				}
			}
		});

		toggleButton = (ToggleButton) findViewById(R.id.mach_rfid_func_toggle);
		toggleButton.setEnabled(false);
		toggleButton.setTextOn("停用RFID");
		toggleButton.setTextOff("启用RFID");
		toggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				rfidEnabled = isChecked;
				if (qTask == null && rfidEnabled != oldRfidEnabled) {
					mStatusMessageView.setText(getResources().getString(R.string.submitting_data));
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("setRFIDFlag");
				}
			}
		});

		// configDB = SQLiteDatabase.openOrCreateDatabase(MachRFIDFunctionActivity.this.getFilesDir() + "/config.db", null);
		// configDB = new ConfigDBHelper(MachRFIDFunctionActivity.this);
		loginForm = (LinearLayout) findViewById(R.id.mach_rfid_login_form);
		funcForm = (LinearLayout) findViewById(R.id.mach_rfid_form);
		userIDView = (EditText) findViewById(R.id.mach_rfid_user_id);
		passwordView = (EditText) findViewById(R.id.mach_rfid_password);
		loginButton = (Button) findViewById(R.id.mach_rfid_login_btn);
		passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.mach_rfid_ime_login || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});
		loginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin();
			}
		});
	}

	protected void attemptLogin() {
		userID = userIDView.getText().toString();
		password = passwordView.getText().toString();
		boolean cancel = false;
		View focusView = null;
		if (TextUtils.isEmpty(password)) {
			passwordView.setError(getString(R.string.error_field_required));
			focusView = passwordView;
			cancel = true;
		}
		if (TextUtils.isEmpty(userID)) {
			userIDView.setError(getString(R.string.error_field_required));
			focusView = userIDView;
			cancel = true;
		}
		// String value = "";
		// Cursor c = configDB.query("config", new String[] { "_id", "type", "owner", "value" }, "type=? and owner=?", new String[] { CONFIG_OWNER_TYPE, userID }, null, null, null, null);
		// // Cursor c = configDB.query(new String[] { "_id", "type", "owner", "value" }, "type=? and owner=?", new String[]{CONFIG_OWNER_TYPE, userID}, null, null, null, null);
		// if (c.moveToNext()) {
		// value = c.getString(3);
		// }
		// if (!value.equals("Y")) {
		// userIDView.setError(getString(R.string.no_access_privilege));
		// focusView = userIDView;
		// cancel = true;
		// }
		if (cancel) {
			focusView.requestFocus();
		} else {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("login");
		}
	}

	public class QueryTask extends AsyncTask<String, Void, BaseException> {
		DataCollection queryResult = null;
		String cmdName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("getRFIDFlag")) {
					getRFIDFlag();
				} else if (cmdName.equals("setRFIDFlag")) {
					setRFIDFlag();
				} else if (cmdName.equals("getAssignedMach")) {
					getAssignedMach();
				} else if (cmdName.equals("login")) {
					login();
				}
			} catch (BaseException e) {
				return e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(final BaseException e) {
			qTask = null;
			showProgress(false);
			if (null == e) {
				if (cmdName.equals("getRFIDFlag")) {
					toggleButton.setChecked(rfidEnabled);
					oldRfidEnabled = rfidEnabled;
					toggleButton.setEnabled(true);
				} else if (cmdName.equals("setRFIDFlag")) {
					oldRfidEnabled = rfidEnabled;
				} else if (cmdName.equals("getAssignedMach")) {
					machIdAlertDialog = new AlertDialog.Builder(MachRFIDFunctionActivity.this).setTitle("选择机台")
							.setItems(machIdArray, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									machID = "" + machIdArray[which];
									machIdSelect.setText(machID);
									if (qTask == null) {
										mStatusMessageView.setText(getResources().getString(R.string.loading_data));
										showProgress(true);
										qTask = new QueryTask();
										qTask.execute("getRFIDFlag");
									}
								}
							}).setNegativeButton(getResources().getString(R.string.cancel), null).create();
					machIdAlertDialog.show();
				} else if (cmdName.equals("login")) {
					loginForm.setVisibility(View.GONE);
					funcForm.setVisibility(View.VISIBLE);
					Toast.makeText(MachRFIDFunctionActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
				}
			} else {
				if (cmdName.equals("setRFIDFlag")) {
					rfidEnabled = oldRfidEnabled;
					toggleButton.setChecked(rfidEnabled);
				}
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(MachRFIDFunctionActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(MachRFIDFunctionActivity.this, e.getErrorMsg());
				}
			}
		}

		private void setRFIDFlag() throws BaseException {
			if (!CommonUtility.isEmpty(userID)) {
				String flag = rfidEnabled ? "Y" : "N";
				String api = "setRFIDFlag(transUserId='" + userID + "',machId='" + machID + "',flag='" + flag + "')";
				apiExecutorUpdate.transact("MachRFIDFunction", "setRFIDFlag", api);
				logf(api);
			} else {
				throw new RfidException("Please login.", "MachRFIDFunction", "setRFIDFlag", "");
			}
		}

		private void getRFIDFlag() throws BaseException {
			String api = "getRFIDFlag(machId='" + machID + "')";
			// queryResult = apiExecutor.query(api);
			// if (!CommonUtility.isEmpty(apiExecutor.getMessage())) {
			// errorMsg = apiExecutor.getMessage();
			// } else {
			// if (!CommonUtility.isEmpty(queryResult)) {
			// String rfidFlag = queryResult.get(0).get(0);
			// if (rfidFlag.equalsIgnoreCase("Y")) {
			// rfidEnabled = true;
			// } else {
			// rfidEnabled = false;
			// }
			// }
			// }

			try {
				InterfaceMgrSocketConfig socketString = global.getInterfaceMgrSocketConfigQuery();
				StringBuilder s = new StringBuilder();
				s.append("submitRequest(\"");
				s.append(socketString.getUser());
				s.append("\",\"");
				s.append(socketString.getInstance());
				s.append("\",\"");
				s.append(api);
				s.append("\")");
				String command = s.toString();
				Socket socket = new Socket(socketString.getHost(), socketString.getPort().intValue());
				socket.setSoTimeout(socketString.getTimeout());
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out.write(command);
				out.flush();
				String response = in.readLine();
				if (socket.isConnected()) {
					out.close();
					in.close();
					socket.close();
				}
				response = response.substring(1, response.length() - 1).trim();
				int status = Integer.parseInt(response.substring(0, 1).trim());
				if (status != 0) {
					throw new RfidException(response.substring(3), "MachRFIDFunction", "getRFIDFlag", command);
				} else {
					String flag = response.substring(response.length() - 1, response.length());
					if (flag.equalsIgnoreCase("Y")) {
						rfidEnabled = true;
					} else {
						rfidEnabled = false;
					}
				}
			} catch (UnknownHostException ex) {
				throw new RfidException(ex.toString(), "MachRFIDFunction", "getRFIDFlag", "");
			} catch (IOException ex) {
				throw new RfidException(ex.toString(), "MachRFIDFunction", "getRFIDFlag", "");
			}
		}

		private void getAssignedMach() throws BaseException {
			CommonTrans commonTrans = new CommonTrans();
			machIdArray = commonTrans.getAssignedMachArray(apiExecutorQuery, global);
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void login() throws BaseException {
			String machineId = global.getAndroidSecureID();
			GenesisGateway GenesisGateway = new GenesisGateway(global.getInterfaceMgrSocketConfigQuery());
			try {
				LoginInfo loginInfo;
				try {
					loginInfo = GenesisGateway.login(userID, password, machineId);
				} catch (Exception e) {
					throw new RfidException("登录失败", classname, "Login", "");
				}
				if (loginInfo.isSuccess()) {
					String link = "servlet/LoginServlet?action=getMachRFIDPrivilege&userID=" + userID + "&deviceID=" + global.getAndroidSecureID();
					CommonTrans commonTrans = new CommonTrans();
					String output = commonTrans.queryFromServer(link);
					if (null != output && !output.isEmpty()) {
						if (!output.equalsIgnoreCase("Y")) {
							throw new RfidException(userID + " 没有权限", classname, "Login", link);
						}
					} else {
						throw new RfidException("连接服务器失败", classname, "Login", link);
					}
				} else {
					if (null == loginInfo.getReason()) {
						throw new RfidException("非法用户。", classname, "Login", "");
					} else {
						throw new RfidException(loginInfo.getReason(), classname, "Login", "");
					}
				}
			} catch (BaseException e) {
				throw e;
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
	protected void onDestroy() {
		// configDB.close();
		super.onDestroy();
	}
}
