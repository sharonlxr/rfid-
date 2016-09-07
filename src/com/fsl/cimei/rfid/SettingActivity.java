package com.fsl.cimei.rfid;

import interfacemgr.genesis.entity.InterfaceMgrSocketConfig;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.freescale.api.BaseApiExecutor;
import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;

public class SettingActivity extends BaseActivity {

	private QueryTask qTask;
	private ToggleButton envToggle;
	private ToggleButton carrierAssignInOutToggle;
	private ToggleButton manualDeassignToggle;
	private ArrayAdapter<String> buadAdapter;
	private Spinner buadSpinner;
	private ToggleButton msgFilterToggle;
	private ToggleButton carrierAssignLocToggle;
	private ToggleButton alarmUnsetMenuToggle;
	private ToggleButton autoLogoutToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
		envToggle = (ToggleButton) findViewById(R.id.setting_env);
		envToggle.setTextOn("Prod");
		envToggle.setTextOff("UAT");
		envToggle.setChecked(Constants.configFileName.equals(Constants.CONFIG_PROD));
		envToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					Constants.configFileName = Constants.CONFIG_PROD;
				} else {
					Constants.configFileName = Constants.CONFIG_TEST;
				}
				if (Constants.configFileName.equals(Constants.CONFIG_PROD)) {
					InterfaceMgrSocketConfig mInterfaceMgrSocketConfigUpdate = new InterfaceMgrSocketConfig(global.getConfigProd().get("hostname_u"), Integer
							.parseInt(global.getConfigProd().get("port_u")), global.getConfigProd().get("matlmgr_u"), global.getConfigProd().get("secureId_u"), Integer
							.parseInt(global.getConfigProd().get("timeout_u")));
					global.setInterfaceMgrSocketConfigUpdate(mInterfaceMgrSocketConfigUpdate);
					InterfaceMgrSocketConfig mInterfaceMgrSocketConfigQuery = new InterfaceMgrSocketConfig(global.getConfigProd().get("hostname_q"), Integer
							.parseInt(global.getConfigProd().get("port_q")), global.getConfigProd().get("matlmgr_q"), global.getConfigProd().get("secureId_q"), Integer
							.parseInt(global.getConfigProd().get("timeout_q")));
					global.setInterfaceMgrSocketConfigQuery(mInterfaceMgrSocketConfigQuery);
				} else {
					InterfaceMgrSocketConfig mInterfaceMgrSocketConfigUpdate = new InterfaceMgrSocketConfig(global.getConfigTest().get("hostname_u"), Integer
							.parseInt(global.getConfigTest().get("port_u")), global.getConfigTest().get("matlmgr_u"), global.getConfigTest().get("secureId_u"), Integer
							.parseInt(global.getConfigTest().get("timeout_u")));
					global.setInterfaceMgrSocketConfigUpdate(mInterfaceMgrSocketConfigUpdate);
					InterfaceMgrSocketConfig mInterfaceMgrSocketConfigQuery = new InterfaceMgrSocketConfig(global.getConfigTest().get("hostname_q"), Integer
							.parseInt(global.getConfigTest().get("port_q")), global.getConfigTest().get("matlmgr_q"), global.getConfigTest().get("secureId_q"), Integer
							.parseInt(global.getConfigTest().get("timeout_q")));
					global.setInterfaceMgrSocketConfigQuery(mInterfaceMgrSocketConfigQuery);
				}
				apiExecutorQuery = new BaseApiExecutor(global.getInterfaceMgrSocketConfigQuery());
				apiExecutorUpdate = new BaseApiExecutor(global.getInterfaceMgrSocketConfigUpdate());
			}
		});
		
		carrierAssignInOutToggle = (ToggleButton) findViewById(R.id.setting_carrier_assign_in_out);
		carrierAssignInOutToggle.setTextOn("Y");
		carrierAssignInOutToggle.setTextOff("N");
		carrierAssignInOutToggle.setChecked(Constants.carrierAssignInputOutput);
		carrierAssignInOutToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					Constants.carrierAssignInputOutput = true;
				} else {
					Constants.carrierAssignInputOutput = false;
				}
			}
		});
		
		manualDeassignToggle = (ToggleButton) findViewById(R.id.setting_manual_deassign);
		manualDeassignToggle.setTextOn("Y");
		manualDeassignToggle.setTextOff("N");
		manualDeassignToggle.setChecked(Constants.manualDeassign);
		manualDeassignToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					Constants.manualDeassign = true;
				} else {
					Constants.manualDeassign = false;
				}
			}
		});	
		
		msgFilterToggle = (ToggleButton) findViewById(R.id.setting_msg_filter);
		msgFilterToggle.setTextOn("Y");
		msgFilterToggle.setTextOff("N");
		msgFilterToggle.setChecked(Constants.msgFilter);
		msgFilterToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					Constants.msgFilter = true;
				} else {
					Constants.msgFilter = false;
				}
			}
		});
		
		List<String> buadList = new ArrayList<String>();
		buadList.add("9600");
		buadList.add("115200");
		buadAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, buadList);
		buadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		buadSpinner = (Spinner) findViewById(R.id.setting_buad);
		buadSpinner.setAdapter(buadAdapter);
		if (choosed_buad == 9600) {
			buadSpinner.setSelection(0);
		} else if (choosed_buad == 115200) {
			buadSpinner.setSelection(1);
		}
		buadSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				String result = buadAdapter.getItem(position);
				choosed_buad = Integer.parseInt(result);
				if (readThread != null) {
					readThread.interrupt();
					serialPort = null;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
		
		carrierAssignLocToggle = (ToggleButton) findViewById(R.id.setting_carrier_assign_loc);
		carrierAssignLocToggle.setTextOn("Y");
		carrierAssignLocToggle.setTextOff("N");
		carrierAssignLocToggle.setChecked(Constants.carrierAssignLoc);
		carrierAssignLocToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					Constants.carrierAssignLoc = true;
				} else {
					Constants.carrierAssignLoc = false;
				}
			}
		});
		
		alarmUnsetMenuToggle = (ToggleButton) findViewById(R.id.setting_alarm_unset);
		alarmUnsetMenuToggle.setTextOn("Y");
		alarmUnsetMenuToggle.setTextOff("N");
		alarmUnsetMenuToggle.setChecked(Constants.alarmUnsetMenu);
		alarmUnsetMenuToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					Constants.alarmUnsetMenu = true;
				} else {
					Constants.alarmUnsetMenu = false;
				}
			}
		});
		
		autoLogoutToggle = (ToggleButton) findViewById(R.id.setting_auto_logout);
		autoLogoutToggle.setTextOn("Y");
		autoLogoutToggle.setTextOff("N");
		autoLogoutToggle.setChecked(Constants.autoLogout);
		autoLogoutToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					Constants.autoLogout = true;
				} else {
					Constants.autoLogout = false;
				}
			}
		});
		
		findViewById(R.id.setting_admin).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
//				SQLiteDatabase configDB = SQLiteDatabase.openOrCreateDatabase(SettingActivity.this.getFilesDir() + "/config.db", null);
//				Cursor c = configDB.query("config", new String[] { "_id", "type", "owner", "value" }, "type=?", new String[] { MachRFIDFunctionActivity.CONFIG_OWNER_TYPE }, null,
//						null, null, null);
//				StringBuilder builder = new StringBuilder();
//				while (c.moveToNext()) {
//					builder.append(c.getString(2)).append(" ");
//				}
//				configDB.close();
//				showMsg(SettingActivity.this, builder.toString());
				if (null == qTask) {
					qTask = new QueryTask();
					qTask.execute("getMachRFIDPrivilege");
				}
			}
		});
		
		findViewById(R.id.setting_msg_hist).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SettingActivity.this, MessageTestActivity.class);
				startActivity(intent);
			}
		});
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		CommonUtility.updateSettingFile();
		if (null != qTask) {
			qTask.cancel(true);
		}
	}
	
	public class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";
		String getMachRFIDPrivilegeResult = "";
		
		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("getMachRFIDPrivilege")) {
					String link = "servlet/LoginServlet?action=getMachRFIDPrivilege&deviceID=" + global.getAndroidSecureID();
					CommonTrans commonTrans = new CommonTrans();
					String output = commonTrans.queryFromServer(link);
					getMachRFIDPrivilegeResult = output.replaceAll("\\|", "  ");
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
				if (cmdName.equals("getMachRFIDPrivilege")) {
					showMsg(SettingActivity.this, getMachRFIDPrivilegeResult);
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(SettingActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(SettingActivity.this, e.getErrorMsg());
				}
			}
		}
		
		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}
	}
}
