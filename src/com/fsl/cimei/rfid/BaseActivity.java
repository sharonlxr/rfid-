package com.fsl.cimei.rfid;

import interfacemgr.genesis.entity.InterfaceMgrSocketConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import app.utils.login.genesis.GenesisUser;
import cn.pda.scan.ScanThread;

import com.freescale.api.BaseApiExecutor;
import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.AOLot;
import com.zkc.pc700.helper.ScanGpio;
import com.zkc.pc700.helper.SerialPort;

public class BaseActivity extends Activity {
	protected GlobalVariable global = null;
	protected BaseApiExecutor apiExecutorQuery = null;
	protected BaseApiExecutor apiExecutorUpdate = null;
	protected View mFormView;
	protected View mStatusView;
	protected TextView mStatusMessageView;

	private byte[] packedData = new byte[] { 0x07, (byte) 0xC6, 0x04, 0x08, 0x00, (byte) 0xEB, 0x07, (byte) 0xFE, 0x35 };// 读取数据格式
	protected static SerialPort serialPort;
	private InputStream mInputStream;// 读取信息流
	private String choosed_serial = "/dev/ttyMT0";// 串口号
	protected static int choosed_buad = 9600;// 波特率   115200
	protected ReadThread readThread;// 读取信息监听线程
	private static byte[] getbuffer = new byte[1024];// 存条码信息
	private static int getsize = 0;// 条码信息长度
	protected static ScanGpio scanGpio;// = new ScanGpio();
	protected BaseHandler alotNumberInputHandler;
	public TagBarcodeInputFragment tagBarcodeInputFragment = null;
	private ScanThread chiliBarcodeScanThread;
	private Handler chiliBarcodeHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		global = (GlobalVariable) getApplication();
		String errorMsg = null;
		if (CommonUtility.isEmpty(Constants.configFileName)) {
			log("Set up setting file");
			setupSettingFile();
		}
		if (!new File(this.getFilesDir().getAbsolutePath() + "/" + Constants.CONFIG_PROD).exists()) {
			log("Copying " + Constants.CONFIG_PROD);
			errorMsg = CommonUtility.copyConfigFile(this, Constants.CONFIG_PROD);
		}
		if (!new File(this.getFilesDir().getAbsolutePath() + "/" + Constants.CONFIG_TEST).exists()) {
			log("Copying " + Constants.CONFIG_TEST);
			errorMsg = CommonUtility.copyConfigFile(this, Constants.CONFIG_TEST);
		}
		if (null == errorMsg && (null == global.getConfigProd() || global.getConfigProd().isEmpty())) {
			global.setConfigProd(CommonUtility.readConfig(this, Constants.CONFIG_PROD));
		}
		if (null == errorMsg && (null == global.getConfigTest() || global.getConfigTest().isEmpty())) {
			global.setConfigTest(CommonUtility.readConfig(this, Constants.CONFIG_TEST));
		}
		if (null == global.getInterfaceMgrSocketConfigUpdate() || null == global.getInterfaceMgrSocketConfigQuery()) {
			// log("null == global.getInterfaceMgrSocketConfig()");
			if (Constants.configFileName.equals(Constants.CONFIG_PROD)) {
				InterfaceMgrSocketConfig mInterfaceMgrSocketConfigUpdate = new InterfaceMgrSocketConfig(global.getConfigProd().get("hostname_u"), Integer.parseInt(global
						.getConfigProd().get("port_u")), global.getConfigProd().get("matlmgr_u"), global.getConfigProd().get("secureId_u"), Integer.parseInt(global
								.getConfigProd().get("timeout_u")));
				global.setInterfaceMgrSocketConfigUpdate(mInterfaceMgrSocketConfigUpdate);
				InterfaceMgrSocketConfig mInterfaceMgrSocketConfigQuery = new InterfaceMgrSocketConfig(global.getConfigProd().get("hostname_q"), Integer.parseInt(global
						.getConfigProd().get("port_q")), global.getConfigProd().get("matlmgr_q"), global.getConfigProd().get("secureId_q"), Integer.parseInt(global
								.getConfigProd().get("timeout_q")));
				global.setInterfaceMgrSocketConfigQuery(mInterfaceMgrSocketConfigQuery);
			} else {
				InterfaceMgrSocketConfig mInterfaceMgrSocketConfigUpdate = new InterfaceMgrSocketConfig(global.getConfigTest().get("hostname_u"), Integer.parseInt(global
						.getConfigTest().get("port_u")), global.getConfigTest().get("matlmgr_u"), global.getConfigTest().get("secureId_u"), Integer.parseInt(global
								.getConfigTest().get("timeout_u")));
				global.setInterfaceMgrSocketConfigUpdate(mInterfaceMgrSocketConfigUpdate);
				InterfaceMgrSocketConfig mInterfaceMgrSocketConfigQuery = new InterfaceMgrSocketConfig(global.getConfigTest().get("hostname_q"), Integer.parseInt(global
						.getConfigTest().get("port_q")), global.getConfigTest().get("matlmgr_q"), global.getConfigTest().get("secureId_q"), Integer.parseInt(global
								.getConfigTest().get("timeout_q")));
				global.setInterfaceMgrSocketConfigQuery(mInterfaceMgrSocketConfigQuery);
			}
		}
		if (global.getUser() == null) {
			// log("null == global.getUser()");
			SharedPreferences data = BaseActivity.this.getSharedPreferences("RFID-data", MODE_PRIVATE);
			if (data != null && !CommonUtility.isEmpty(data.getString(Constants.USER_ID, ""))) {
				GenesisUser user = new GenesisUser();
				user.setUserID(data.getString(Constants.USER_ID, ""));
				user.setFirstName(data.getString(Constants.FIRST_NAME, ""));
				user.setLastName(data.getString(Constants.LAST_NAME, ""));
				user.setDepartment(data.getString(Constants.DEPARTMENT, ""));
				user.setLastSuccessLogin(data.getString(Constants.SERVER_CURRENT_DATE, ""));
				global.setUser(user);
			}
		}
		apiExecutorQuery = new BaseApiExecutor(global.getInterfaceMgrSocketConfigQuery());
		apiExecutorUpdate = new BaseApiExecutor(global.getInterfaceMgrSocketConfigUpdate());
		setupActionBar();
	}

	private void setupSettingFile() {
		File file = Environment.getExternalStorageDirectory();
		File settingFolder = new File(file.getAbsolutePath() + "/rfid-config/");
		File settingFile = new File(file.getAbsolutePath() + "/rfid-config/setting.conf");
		boolean success = true;
		if (settingFile.exists()) {
			Properties result = new Properties();
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(settingFile);
				result.load(fis);
				if (result.containsKey("env")) {
					if (result.get("env").equals("prod")) {
						Constants.configFileName = Constants.CONFIG_PROD;
					} else if (result.get("env").equals("test")) {
						Constants.configFileName = Constants.CONFIG_TEST;
					} else {
						success = false;
					}
				} else {
					success = false;
				}
				if (result.containsKey("carrier_assign_in_out")) {
					if (result.get("carrier_assign_in_out").equals("Y")) {
						Constants.carrierAssignInputOutput = true;
					} else if (result.get("carrier_assign_in_out").equals("N")) {
						Constants.carrierAssignInputOutput = false;
					} else {
						success = false;
					}
				} else {
					success = false;
				}
				if (result.containsKey("carrier_assign_loc")) {
					if (result.get("carrier_assign_loc").equals("Y")) {
						Constants.carrierAssignLoc = true;
					} else if (result.get("carrier_assign_loc").equals("N")) {
						Constants.carrierAssignLoc = false;
					} else {
						success = false;
					}
				} else {
					success = false;
				}
				if (result.containsKey("alarm_unset_menu")) {
					if (result.get("alarm_unset_menu").equals("Y")) {
						Constants.alarmUnsetMenu = true;
					} else if (result.get("alarm_unset_menu").equals("N")) {
						Constants.alarmUnsetMenu = false;
					} else {
						success = false;
					}
				} else {
					success = false;
				}
				if (result.containsKey("auto_logout")) {
					if (result.get("auto_logout").equals("Y")) {
						Constants.autoLogout = true;
					} else if (result.get("auto_logout").equals("N")) {
						Constants.autoLogout = false;
					} else {
						success = false;
					}
				} else {
					success = false;
				}
				if (result.containsKey("manual_deassign")) {
					if (result.get("manual_deassign").equals("Y")) {
						Constants.manualDeassign = true;
					} else if (result.get("manual_deassign").equals("N")) {
						Constants.manualDeassign = false;
					} else {
						success = false;
					}
				} else {
					success = false;
				}
				if (result.containsKey("message_filter")) {
					if (result.get("message_filter").equals("Y")) {
						Constants.msgFilter = true;
					} else if (result.get("message_filter").equals("N")) {
						Constants.msgFilter = false;
					} else {
						success = false;
					}
				} else {
					success = false;
				}
				if (result.containsKey("baud")) {
					if (result.get("baud").equals("9600")) {
						choosed_buad = 9600;
					} else if (result.get("baud").equals("115200")) {
						choosed_buad = 115200;
					} else {
						success = false;
					}
				} else {
					success = false;
				}
			} catch (Exception e) {
				log("CommonUtility readConfig " + e.toString());
			} finally {
				if (null != fis) {
					try {
						fis.close();
					} catch (IOException e) {
						log("CommonUtility readConfig " + e.toString());
					}
				}
			}
		} else {
			success = false;
		}
		if (!success) {
			Constants.configFileName = Constants.CONFIG_PROD;
			success = true; // init value
			if (!settingFolder.exists()) {
				success = settingFolder.mkdir();
			}
			if (success) {
				if (settingFile.exists()) {
					success = settingFile.delete();
				}
			} else {
				// fail to create setting file's folder
				logf("Fail to create setting file's folder");
			}
			if (success) {
				try {
					success = settingFile.createNewFile();
				} catch (IOException e) {
					logf("Fail to create new setting file " + e.toString());
					success = false;
				}
			} else {
				// fail to to delete old file
				logf("Fail to delete old setting file");
			}
			if (success) {
				InputStream is = null;
				FileOutputStream fos = null;
				try {
					is = this.getResources().getAssets().open("setting.conf");
					fos = new FileOutputStream(settingFile);
					byte[] buffer = new byte[1024];
					int count = 0;
					while ((count = is.read(buffer)) > 0) {
						fos.write(buffer, 0, count);
					}
					success = true;
				} catch (Exception e) {
					logf("CommonUtility copy: " + e.toString());
					success = false;
				} finally {
					if (null != fos) {
						try {
							fos.close();
						} catch (IOException e) {
							logf("CommonUtility copy: " + e.toString());
						}
					}
					if (null != is) {
						try {
							is.close();
						} catch (IOException e) {
							logf("CommonUtility copy: " + e.toString());
						}
					}
				}
			} else {
				logf("Fail to create new setting file");
			}
		}
		if (!success) {
			// fail anyway
			Constants.configFileName = Constants.CONFIG_PROD;
			Constants.carrierAssignInputOutput = false;
			Constants.manualDeassign = false;
			Constants.msgFilter = true;
			choosed_buad = 9600;
		}
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	protected void showProgress(final boolean show) {
		if (null == mStatusView || null == mFormView) {
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
			mStatusView.setVisibility(View.VISIBLE);
			mStatusView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
				}
			});
			mFormView.setVisibility(View.VISIBLE);
			mFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mFormView.setVisibility(show ? View.GONE : View.VISIBLE);
				}
			});
		} else {
			mStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	protected void showError(Context context, String errorMsg) {
		CommonUtility.logError(errorMsg, Constants.LOG_FILE_ERR);
		new AlertDialog.Builder(context).setTitle(getResources().getString(R.string.title_error)).setIcon(android.R.drawable.ic_dialog_info).setMessage(errorMsg)
				.setNegativeButton(getResources().getString(R.string.close), null).show();
	}
	
	protected void showMsg(Context context, String msg) {
		new AlertDialog.Builder(context).setTitle(getResources().getString(R.string.title_message)).setIcon(android.R.drawable.ic_dialog_info).setMessage(msg)
		.setNegativeButton(getResources().getString(R.string.close), null).show();
	}

	/**
	 * display a alert dialog after successfully submit.
	 * 
	 * @author B45234
	 * */
	protected void showSuccessDialog(Context context, String successMsg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(getResources().getString(R.string.title_success));
		builder.setIcon(R.drawable.success);
		builder.setMessage(successMsg);
		builder.setPositiveButton(getResources().getString(R.string.close), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		builder.show();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
//		boolean result = redirect(itemId);
//		if (result) {
//			return result;
//		} else {
//			return super.onOptionsItemSelected(item);
//		}
		Intent intent;
		switch (itemId) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.action_logout:
			this.logout();
			return true;
		case R.id.action_lot_inquiry:
			intent = new Intent(this, LotInquiryActivity.class);
			startActivity(intent);
			return true;
//		case R.id.action_emt_log:
//			global.setAoLot(null);
//			global.setCarrierID("");
//			global.setScanTarget(Constants.SCAN_TARGET_EMT_LOG);
//			intent = new Intent(this, EmtLogActivity.class);
//			startActivity(intent);
//			return true;
		case R.id.action_mach_rfid_function:
			intent = new Intent(this, MachRFIDFunctionActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_op_shift_mach_assign:
			intent = new Intent(this, OpShiftMachAssignActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_rej:
			global.setScanTarget(Constants.SCAN_TARGET_REJ);
			intent = new Intent(this, RejActivity.class);
			startActivity(intent);
			return true;
//		case R.id.action_in_out_carrier_assign:
//			global.setAoLot(null);
//			global.setCarrierID("");
//			global.setScanTarget(Constants.SCAN_TARGET_IN_OUT_ASSIGN_IN);
//			intent = new Intent(this, InOutCarrierAssignmentActivity.class);
//			startActivity(intent);
//			return true;
		case R.id.action_end_lot_on_mach:
			intent = new Intent(this, EndLotOnMachActivity.class);
			startActivity(intent);
			return true;
//		case R.id.action_test:
//			intent = new Intent(this, TestActivity.class);
//			startActivity(intent);
//			return true;
		/*
		 * 
		 * case R.id.action_piece_part_load: 
		 * intent = new Intent(this, PiecePartLoadActivity.class); 
		 * startActivity(intent); 
		 * return true; 
		 * 
		 * case R.id.action_piece_part_unload: 
		 * intent = new Intent(this, PiecePartUnloadActivity.class); 
		 * startActivity(intent); 
		 * return true; 
		 * 
		 * case R.id.action_lot_start: 
		 * intent = new Intent(this, LotStartActivity.class); 
		 * startActivity(intent); 
		 * return true;
		 */
		case R.id.action_rack_mgmt: 
			global.setAoLot(null); 
			global.setCarrierID("");
			global.setRackName("");
			global.setSlotName("");
			global.setScanTarget(Constants.SCAN_TARGET_RACK_INIT);
			intent = new Intent(this, RackMgmtActivity.class); 
			startActivity(intent);
			return true; 
		case R.id.action_pass_window: 
			global.setAoLot(null); 
			global.setCarrierID("");
			global.setRackName("");
			global.setSlotName("");
			global.setScanTarget(Constants.SCAN_TARGET_PASS_WINDOW_INIT);
			intent = new Intent(this, PassWindowActivity.class); 
			startActivity(intent);
			return true; 
		case R.id.action_lot_pass_window: 
			global.setAoLot(null); 
			global.setCarrierID("");
			global.setRackName("");
			global.setSlotName("");
			global.setScanTarget(Constants.SCAN_TARGET_LOT_PASS_WINDOW);
			intent = new Intent(this, LotPassWindowActivity.class); 
			startActivity(intent);
			return true; 
		case R.id.action_about_app:
			intent = new Intent(this, AppInfoActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_carrier_assign:
			global.setScanTarget(Constants.SCAN_TARGET_INIT);
			global.setAoLot(null);
			intent = new Intent(this, CarrierAssignActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_step_hist:
			intent = new Intent(this, LotInquiryStepHistActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_mach_info:
			intent = new Intent(this, LotInquiryMachInfoActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_wafer_info:
			intent = new Intent(this, LotInquiryWaferInfoActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_die_relief:
			intent = new Intent(this, LotInquiryDieReliefActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_wafer_id_usage:
			intent = new Intent(this, LotInquiryWaferIDUsageActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_memo:
			intent = new Intent(this, LotInquiryMemoActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_memo_history:
			intent = new Intent(this, LotInquiryMemoHistoryActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_reason_history:
			intent = new Intent(this, LotInquiryReasonHistActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_future_hold_setting:
			intent = new Intent(this, LotInquiryFutureHoldSettingActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_container_id_usage:
			intent = new Intent(this, LotInquiryContainerIDUsageActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_spv_info:
			intent = new Intent(this, LotInquirySPVInfoActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_cpk_data:
			intent = new Intent(this, LotInquiryCPKDataActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_carrier_usage:
			intent = new Intent(this, LotInquiryCarrierUsageActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_exit:
			exit();
			break;
		case R.id.action_end_lot:
			global.setScanTarget(Constants.SCAN_TARGET_END_LOT);
			global.setAoLot(null);
			intent = new Intent(this, EndLotActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_check_magazine:
			global.setScanTarget(Constants.SCAN_TARGET_CHECK_MAGAZINE_INIT);
			intent = new Intent(this, CheckMagazineActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_cassette:
			global.setScanTarget(Constants.SCAN_TARGET_CASSETTE);
			intent = new Intent(this, CassetteActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_lot_carrier_tracking:
			global.setScanTarget(Constants.SCAN_TARGET_LOT_CARRIER_TRACKING);
			intent = new Intent(this, LotCarrierTrackingActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_setting:
			goToSetting();
			return true;
		case R.id.action_bonding_diagram:
			getBondingDiagram();
			return true;
		case R.id.action_agile:
			getAgile();
			return true;
//		case R.id.action_onepage:
//			getOnepage();
//			return true;
		case R.id.action_rej_mag:
			global.setScanTarget(Constants.SCAN_TARGET_REJ_MAG);
			intent = new Intent(this, RejMagActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_si:
			global.setScanTarget(Constants.SCAN_TARGET_SI_INIT);
			intent = new Intent(this, StripInspectionActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_lot_carrier_hist:
			global.setScanTarget(Constants.SCAN_TARGET_LOT_CARRIER_HIST);
			intent = new Intent(this, LotCarrierHistActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_lot_carrier_report:
			intent = new Intent(this, LotCarrierReportActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_div_lot:
			global.setScanTarget(Constants.SCAN_TARGET_DIV_LOT_INIT);
			intent = new Intent(this, DivLotActivity.class);
			startActivity(intent);
			return true;
		case R.id.action_lot_div_progress:
			intent = new Intent(this, DivLotProgressActivity.class);
			ArrayList<String> progress = new ArrayList<String>();
			ArrayList<String> magScan = new ArrayList<String>();
			ArrayList<String> magNotScan = new ArrayList<String>();
			((DivLotActivity)this).getAlotNumbers(progress, magScan,magNotScan);
			intent.putExtra("progress", progress);	
			intent.putExtra("magScan",magScan);
			intent.putExtra("magNotScan",magNotScan);
			startActivity(intent);
			return true;
		case R.id.action_rack_lot_mgmt: 
			global.setAoLot(null); 
			global.setCarrierID("");
			global.setRackName("");
			global.setSlotName("");
			global.setScanTarget(Constants.SCAN_TARGET_LOT_RACK_INIT);
			intent = new Intent(this, LotRackActivity.class); 
			startActivity(intent);
			return true;
		case R.id.action_alarm_unset: 
			intent = new Intent(this, AlarmUnsetActivity.class); 
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void redirect(int itemId) {
		/*
			ControlActivity - loadMainMenuConfig()
		    MainMenuActivity.menuMap.put(1, getResources().getString(R.string.action_logout));
			MainMenuActivity.menuMap.put(2, getResources().getString(R.string.title_activity_lot_inquiry));
			MainMenuActivity.menuMap.put(3, getResources().getString(R.string.title_activity_carrier_assign));
			MainMenuActivity.menuMap.put(4, getResources().getString(R.string.title_activity_check_magazine));
			MainMenuActivity.menuMap.put(5, getResources().getString(R.string.title_activity_div_lot));
			MainMenuActivity.menuMap.put(6, getResources().getString(R.string.title_activity_rej));
			MainMenuActivity.menuMap.put(7, getResources().getString(R.string.title_activity_op_shift_mach_assign));
			MainMenuActivity.menuMap.put(8, getResources().getString(R.string.title_activity_mach_rfid_function));
			MainMenuActivity.menuMap.put(9, getResources().getString(R.string.title_activity_cassette));
			MainMenuActivity.menuMap.put(10, getResources().getString(R.string.title_activity_lot_carrier_hist));
			MainMenuActivity.menuMap.put(11, getResources().getString(R.string.title_activity_lot_carrier_report));
			MainMenuActivity.menuMap.put(12, getResources().getString(R.string.title_activity_app_info));
			MainMenuActivity.menuMap.put(13, getResources().getString(R.string.title_activity_setting));
			MainMenuActivity.menuMap.put(14, getResources().getString(R.string.exit));
			MainMenuActivity.menuMap.put(15, getResources().getString(R.string.title_activity_end_lot));
			MainMenuActivity.menuMap.put(16, getResources().getString(R.string.title_activity_end_lot_on_mach));
			MainMenuActivity.menuMap.put(17, getResources().getString(R.string.title_activity_rej_mag));
			MainMenuActivity.menuMap.put(18, getResources().getString(R.string.title_activity_strip_inspection));
			MainMenuActivity.menuMap.put(19, getResources().getString(R.string.title_activity_rack_mgmt));
			MainMenuActivity.menuMap.put(20, getResources().getString(R.string.title_activity_rack_lot_mgmt));
			MainMenuActivity.menuMap.put(21, getResources().getString(R.string.title_activity_pass_window));
			MainMenuActivity.menuMap.put(22, getResources().getString(R.string.title_activity_message_test));
			MainMenuActivity.menuMap.put(23, getResources().getString(R.string.title_activity_alarm_unset));
			MainMenuActivity.menuMap.put(24, getResources().getString(R.string.title_activity_lot_pass_window));
			MainMenuActivity.menuMap.put(25, getResources().getString(R.string.title_activity_cassette_assign));
		 */
		Intent intent;
		switch (itemId) {
		case 1:
			this.logout();
			return;
		case 2:
			intent = new Intent(this, LotInquiryActivity.class);
			startActivity(intent);
			return;
		case 8:
			intent = new Intent(this, MachRFIDFunctionActivity.class);
			startActivity(intent);
			return;
		case 7:
			intent = new Intent(this, OpShiftMachAssignActivity.class);
			startActivity(intent);
			return;
		case 6:
			global.setScanTarget(Constants.SCAN_TARGET_REJ);
			intent = new Intent(this, RejActivity.class);
			startActivity(intent);
			return;
		case 16:
			intent = new Intent(this, EndLotOnMachActivity.class);
			startActivity(intent);
			return;
		case 12:
			intent = new Intent(this, AppInfoActivity.class);
			startActivity(intent);
			return;
		case 3:
			global.setScanTarget(Constants.SCAN_TARGET_INIT);
			global.setAoLot(null);
			intent = new Intent(this, CarrierAssignActivity.class);
			startActivity(intent);
			return;
		case 14:
			exit();
			break;
		case 15:
			global.setScanTarget(Constants.SCAN_TARGET_END_LOT);
			global.setAoLot(null);
			intent = new Intent(this, EndLotActivity.class);
			startActivity(intent);
			return;
		case 4:
			global.setScanTarget(Constants.SCAN_TARGET_CHECK_MAGAZINE_INIT);
			intent = new Intent(this, CheckMagazineActivity.class);
			startActivity(intent);
			return;
		case 9:
			global.setScanTarget(Constants.SCAN_TARGET_CASSETTE);
			intent = new Intent(this, CassetteActivity.class);
			startActivity(intent);
			return;
		case 25:
			global.setScanTarget(Constants.SCAN_TARGET_LOT_CARRIER_TRACKING);
			intent = new Intent(this, LotCarrierTrackingActivity.class);
			startActivity(intent);
			return;
		case 13:
			goToSetting();
			return;
		case 17:
			global.setScanTarget(Constants.SCAN_TARGET_REJ_MAG);
			intent = new Intent(this, RejMagActivity.class);
			startActivity(intent);
			return;
		case 18:
			global.setScanTarget(Constants.SCAN_TARGET_SI_INIT);
			intent = new Intent(this, StripInspectionActivity.class);
			startActivity(intent);
			return;
		case 10:
			global.setScanTarget(Constants.SCAN_TARGET_LOT_CARRIER_HIST);
			intent = new Intent(this, LotCarrierHistActivity.class);
			startActivity(intent);
			return;
		case 11:
			intent = new Intent(this, LotCarrierReportActivity.class);
			startActivity(intent);
			return;
		case 5:
			global.setScanTarget(Constants.SCAN_TARGET_DIV_LOT_INIT);
			intent = new Intent(this, DivLotActivity.class);
			startActivity(intent);
			return;
		case 19:
			global.setAoLot(null); 
			global.setCarrierID("");
			global.setRackName("");
			global.setSlotName("");
			global.setScanTarget(Constants.SCAN_TARGET_RACK_INIT);
			intent = new Intent(this, RackMgmtActivity.class); 
			startActivity(intent);
			return;
		case 20:
			global.setAoLot(null); 
			global.setCarrierID("");
			global.setRackName("");
			global.setSlotName("");
			global.setScanTarget(Constants.SCAN_TARGET_LOT_RACK_INIT);
			intent = new Intent(this, LotRackActivity.class); 
			startActivity(intent);
			return;
		case 21:
			global.setAoLot(null); 
			global.setCarrierID("");
			global.setRackName("");
			global.setSlotName("");
			global.setScanTarget(Constants.SCAN_TARGET_PASS_WINDOW_INIT);
			intent = new Intent(this, PassWindowActivity.class); 
			startActivity(intent);
			return;
		case 22:
			intent = new Intent(this, MessageTestActivity.class); 
			startActivity(intent);
			return;
		case 23:
			intent = new Intent(this, AlarmUnsetActivity.class); 
			startActivity(intent);
			return;
		case 24:
			global.setAoLot(null); 
			global.setCarrierID("");
			global.setRackName("");
			global.setSlotName("");
			global.setScanTarget(Constants.SCAN_TARGET_LOT_PASS_WINDOW);
			intent = new Intent(this, LotPassWindowActivity.class); 
			startActivity(intent);
			return; 
		}
	}

	public void logout() {
	}
	public void getBondingDiagram() {
	}
	public void getAgile() {
	}
	public void getOnepage() {
	}

	class ReadThread extends Thread {
		Handler handler = null;

		ReadThread(Handler handler) {
			this.handler = handler;
		}

		@Override
		public void run() {
			while (!interrupted()) {
				try {
					int size;
					byte[] buffer = new byte[1024];
					if (mInputStream == null)
						return;
					// 读取返回信息
					size = mInputStream.read(buffer);
					log("size: " + size);
					// 保存读取数据信息
					for (int i = 0; i < size; i++) {
						getbuffer[getsize + i] = buffer[i];
					}
					// 保存数据信息长度
					getsize += size;
					handler.post(new Runnable() {
						@Override
						public void run() {
							Message m = new Message();
							m.arg1 = getsize;
							m.obj = getbuffer;
							handler.sendMessage(m);
						}
					});
				} catch (Exception e) {
					log(e.toString());
					return;
				}
			}
		}
	}

	protected String parseMessage(byte[] getdata, int sizs) {
		String getStringPort = "";
		byte[] setData = new byte[sizs];
		boolean isfull = true;// 是否取得完整数据
		boolean isLuanMa = false;// 首次是否出现乱码数据【4, -48, 0, 0, -1, 44】
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < sizs; i++) {
			sb.append(" ~ ").append(getdata[i]);
		}
		log(sb.toString());
		for (int i = 0; i < sizs; i++) {
			setData[i] = getdata[i];
			if (getdata[i] == 13) {
				isfull = true;
			}
			if (getdata[i] == -1) {
				isLuanMa = true;
			}
		}
		if (isfull) {
			// ControlActivity.isScanOpen = false;
			try {
				// 转换扫描信息为字符串，格式UTF-8
				if (isLuanMa) {
					setData = new byte[sizs];
					for (int j = 6; j < sizs; j++) {
						setData[j - 6] = getdata[j];
					}
					getStringPort = new String(setData, 0, sizs, "UTF-8");
				} else {
					getStringPort = new String(setData, 0, sizs, "UTF-8");
				}
				log(getStringPort);
			} catch (Exception e) {
				log(e.toString());
			}
			String alotNumber = getStringPort.trim();
			if (!CommonUtility.isEmpty(alotNumber)) {
				if (alotNumber.startsWith("1T")) {
					Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
					vibrator.vibrate(200L);
					alotNumber = alotNumber.substring(2);
					global.setAoLot(new AOLot(alotNumber));
					return alotNumber;
				} else {
					Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
					vibrator.vibrate(200L);
					global.setAoLot(new AOLot(alotNumber.trim()));
					return alotNumber;
				}
			}
		}
		return null;
	}

	public static void clerkMessage() {
		getsize = 0;// 扫描信息长度清0
		getbuffer = new byte[1024];// 清空扫描信息
	}

	protected void scanBarcode(Handler handler) {
		try {
			// 连接串口
			if (null == serialPort) {
				log("null == serialPort");
				serialPort = new SerialPort(choosed_serial, choosed_buad, 0);
				mInputStream = serialPort.getInputStream();
				// 打开电源
				scanGpio.openPower();
				// 设置接收格式
				serialPort.send_Instruct(packedData);
			}
			// 开启读取信息监听
			if (null == readThread || !readThread.isAlive()) {
				log("!readThread.isAlive()");
				readThread = new ReadThread(handler);
				readThread.start();
			}

			// if (ControlActivity.isStartOpen) {
			// ControlActivity.isStartOpen = false;
			// 清空文本框
			// if (ControlActivity.alotNumberInput != null) {
			// ControlActivity.alotNumberInput.setText("");
			// }
			// 清空数据，打开扫描
			clerkMessage();
			scanGpio.openScan();
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						log(e.toString());
					}
					// ControlActivity.isScanOpen = false;
				}
			}).start();
			// }
		} catch (Exception e1) {
			log(e1.toString());
		}
	}

	@SuppressLint("HandlerLeak")
	class BaseHandler extends Handler {

		EditText alotNumberInput = null;

		BaseHandler(EditText alotNumberInput) {
			this.alotNumberInput = alotNumberInput;
		}

		void setAlotNumberInput(EditText alotNumberInput) {
			this.alotNumberInput = alotNumberInput;
		}

		@Override
		public void handleMessage(Message msg) {
			if (null != parseMessage((byte[]) msg.obj, msg.arg1) && null != alotNumberInput) {
				alotNumberInput.setText(global.getAoLot().getAlotNumber());
			}
		}
	};

	@Override
	protected void onDestroy() {
		if (chiliBarcodeScanThread != null) {
			chiliBarcodeScanThread.interrupt();
			chiliBarcodeScanThread.close();
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		if (readThread != null) {
			readThread.interrupt();
			serialPort = null;
		}
		super.onPause();
	}

	protected void log(String msg) {
		Log.e(Constants.APP_NAME, msg);
	}

	protected void logf(String msg) {
		CommonUtility.logError(msg, Constants.LOG_FILE_ERR);
	}

	@Override
	public void onBackPressed() {
		NavUtils.navigateUpFromSameTask(this);
		// super.onBackPressed();
	}

	public void exit() {
	}
	public void goToSetting() {
	}

	TextView alotNumberTextView;
	EditText tagBarcodeInput;
	Button n7ScanBarcode;
	@SuppressLint("HandlerLeak")
	public void initTagBarcodeInput() {
		if (this.tagBarcodeInputFragment == null) {
			return;
		}
		if (this.mStatusView != null) {
			this.mStatusView.setVisibility(View.GONE);
		}
		alotNumberTextView = this.tagBarcodeInputFragment.getAlotTextView();
		tagBarcodeInput = this.tagBarcodeInputFragment.getTagBarcodeInput();
		n7ScanBarcode = this.tagBarcodeInputFragment.getN7ScanBarcode();
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		// for type 3
		tagBarcodeInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				// iData, input + "return"(char)13
				if (id == R.id.tb_fragment_search_t || id == EditorInfo.IME_NULL) {
					String tagId = tagBarcodeInput.getText().toString().trim().toUpperCase(Locale.CHINESE);
					if (tagId.length() == 14 && tagId.startsWith("1T")) {
						tagBarcodeInput.setText("");
						setBarcodeInput(tagId.substring(2).toUpperCase(Locale.CHINESE));
					} else if (tagId.length() == 16 && CommonUtility.isValidHexNumber(tagId)) {
						tagBarcodeInput.setText("");
						tagId = tagId.substring(0, tagId.length() / 2);
						setTagId(tagId);
					} else if (!CommonUtility.isEmpty(tagId)) {
						// maybe magazine's barcode
						tagBarcodeInput.setText("");
						setBarcodeInput(tagId.toUpperCase(Locale.CHINESE));
					}
					return true;
				}
				return false;
			}
		});
		// for type 2
		alotNumberInputHandler = new BaseHandler(tagBarcodeInput);
		tagBarcodeInput.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == 131 && Constants.type == 2) {
					startScanBarcode();
				} else if (keyCode == 131 && Constants.type == 5) {
					chiliBarcodeScanThread.scan();
				} else if ((keyCode == 134 || keyCode == 135 || keyCode == 132) && Constants.type == 5) {
					Intent intent = new Intent(BaseActivity.this, NewNFCTagActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
					startActivity(intent);
				}
				return false;
			}
		});
		
		tagBarcodeInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@SuppressLint("DefaultLocale")
			@Override
			public void afterTextChanged(Editable s) {
				String tagId = s.toString();
				tagId = tagId.trim();
				if (tagId.length() == 5 && CommonUtility.isValidNumber(tagId)) { // bonding diagram 5-digit number
					tagBarcodeInput.setText("");
					setBarcodeInput(tagId.toUpperCase(Locale.CHINESE));
				} else if (tagId.length() == 18 && tagId.endsWith(";")) {
					tagBarcodeInput.setText("");
					String a = tagId.substring(2, 14);
					setBarcodeInput(a.toUpperCase(Locale.CHINESE));
				} else if (tagId.endsWith(";") && tagId.contains(".")) {
					// maybe rack barcode
					tagBarcodeInput.setText("");
					setBarcodeInput(tagId.substring(0, tagId.length() - 1).toUpperCase(Locale.CHINESE));
				} else if (tagId.endsWith(";") && tagId.startsWith("1T")) {
					tagBarcodeInput.setText("");
					setBarcodeInput(tagId.toUpperCase(Locale.CHINESE).substring(2, tagId.length() - 1));
				} else if (tagId.endsWith(";") && tagId.startsWith("33T")) {
					tagBarcodeInput.setText("");
					setBarcodeInput(tagId.toUpperCase(Locale.CHINESE).substring(0, tagId.length() - 1));
				} else if (tagId.endsWith(";") && tagId.length() > 4) {
					// maybe magazine barcode
					tagBarcodeInput.setText("");
					setBarcodeInput(tagId.toUpperCase(Locale.CHINESE).substring(0, tagId.length() - 1));
				}
			}
		});
		// for type 1
		n7ScanBarcode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent("com.google.zxing.client.android.SCAN");
				BaseActivity.this.startActivityForResult(intent, 0);
			}
		});
		
		tagBarcodeInput.setVisibility(View.VISIBLE);
		tagBarcodeInput.setText("");
		tagBarcodeInput.requestFocus();
		if (Constants.type == 1) {
			n7ScanBarcode.setVisibility(View.VISIBLE);
		}
		if (Constants.type == 2) {
			scanGpio = new ScanGpio();
		}
		if (Constants.type == 5) {
			// NFC tag
			Intent nfcServiceIntent = new Intent(BaseActivity.this, NFCService.class);
			startService(nfcServiceIntent);
			// barcode
			chiliBarcodeHandler = new Handler() {
				public void handleMessage(android.os.Message msg) {
					if (msg.what == ScanThread.SCAN) {
						String data = msg.getData().getString("data").trim();
						if (data.startsWith("1T")) {
							tagBarcodeInput.setText(data.substring(2));
						} else if (!CommonUtility.isEmpty(data)) {
							// maybe magazine's barcode
							tagBarcodeInput.setText(data);
						}
					}
				}
			};
			try {
				chiliBarcodeScanThread = new ScanThread(chiliBarcodeHandler);
				chiliBarcodeScanThread.start();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setBarcodeInput(String input) {
	}
	
	public void setTagId(String tagId) {
		Intent intent = new Intent(BaseActivity.this, NewNFCTagActivity.class);
		intent.putExtra("carrierID", tagId);
		startActivity(intent);
	}
	
	public void startScanBarcode() {
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null) {
			switch (resultCode) {
			case -1:
				String result = data.getStringExtra("SCAN_RESULT");
				if (!CommonUtility.isEmpty(result)) {
//					if (result.trim().startsWith("1T")) {
//						tagBarcodeInput.setText(result.trim().substring(2) + ";");
//					} else {
						// maybe magazine's barcode
						tagBarcodeInput.setText(result.trim() + ";");
//					}
				}
				break;
			}
		}
	}
	
	@SuppressLint("InflateParams")
	public void toastMsg(String message) {
		Toast toastCustom = new Toast(getApplicationContext());
		LayoutInflater inflate = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflate.inflate(R.layout.message_toast, null);
		TextView msgText = (TextView) v.findViewById(R.id.toast_msg_text);
		msgText.setText(message);
		toastCustom.setView(v);
		toastCustom.setDuration(Toast.LENGTH_SHORT);
		toastCustom.show();
	}
}
