package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import app.entity.DataCollection;
import app.utils.login.genesis.GenesisUser;

import com.example.nfc.util.Consts;
import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.AOLot;
import com.fsl.cimei.rfid.exception.ApiException;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;
import com.fsl.cimei.rfid.exception.RfidWifiException;

public class NewNFCTagActivity extends BaseActivity {

	private TextView msgView;
	private QueryTask qTask;
	private LinearLayout lotForm;
	private EditText alotNumberAlertInput;
	private Button assignButton;
	private Button assignInputButton;
	private Button assignOutputButton;
	private Button cancelButton;
	private Button n7ScanBarcode;
	private ChiliNfcBroadcastReceiver chiliNfcBroadcastReceiver;
	// private boolean startFlag = false;
	private int cmdCode = 0;
	private final String classname = "NewNFCTag";
	private List<String> machIdList = new ArrayList<String>();
	private ArrayAdapter<String> machArrayAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_nfctag);
		mFormView = findViewById(R.id.new_nfc_tag_form);
		mStatusView = findViewById(R.id.new_nfc_tag_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		msgView = (TextView) findViewById(R.id.new_nfc_tag_msg);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		if (Constants.type == 5) {
			chiliNfcBroadcastReceiver = new ChiliNfcBroadcastReceiver();
			IntentFilter filter = new IntentFilter();
			filter.addAction("com.fsl.cimei.rfid.hfdemo.NewNFCTagActivity");
			registerReceiver(chiliNfcBroadcastReceiver, filter);
			Intent cmdIntent = new Intent(NewNFCTagActivity.this, NFCService.class);
			cmdCode = Consts.Init_14443a;
			cmdIntent.putExtra("cmd", cmdCode);
			startService(cmdIntent);
		} else {
			resolveIntent(getIntent());
		}
	}

	@SuppressLint("DefaultLocale")
	private void resolveIntent(Intent intent) {
		String action = intent.getAction();
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
			Parcelable parcelable = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			Tag tag = (Tag) parcelable;
			byte[] id = tag.getId();
			String infoStr = getHex(id).toUpperCase();
			String carrierID = infoStr.substring(0, infoStr.length() / 2);
			// newNfcTag.setText(carrierID);
			Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(200L);
			checkScanTarget(carrierID);
		} else {
			String carrierID = intent.getStringExtra("carrierID");
			if (!CommonUtility.isEmpty(carrierID)) {
				checkScanTarget(carrierID);
			}
		}
	}

	private void checkScanTarget(String carrierID) {
		if (CommonUtility.isEmpty(global.getScanTarget())) {
			if (qTask == null) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("checkTag", carrierID);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_EMT_LOG)) {
			global.setCarrierID(carrierID);
			Intent i = new Intent(this, EmtLogActivity.class);
			startActivity(i);
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_INIT)) {
			if (qTask == null) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("getAlotByTag", carrierID);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_INIT)) {
			if (qTask == null) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("getAlotByTag", carrierID);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_ASSIGN)) {
			if (qTask == null && null != global.getAoLot() && !CommonUtility.isEmpty(carrierID)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("carrierAssign", global.getAoLot().getAlotNumber(), carrierID, "INTRANS", "");
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_ASSIGN)) {
			if (qTask == null && null != global.getAoLot() && !CommonUtility.isEmpty(carrierID)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("carrierAssign", global.getAoLot().getAlotNumber(), carrierID, "INTRANS", "");
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_ASSIGN_INPUT)) {
			if (qTask == null && null != global.getAoLot() && !CommonUtility.isEmpty(carrierID)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("carrierAssign", global.getAoLot().getAlotNumber(), carrierID, "IN", "INTRANS");
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_ASSIGN_OUTPUT)) {
			if (qTask == null && null != global.getAoLot() && !CommonUtility.isEmpty(carrierID)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("carrierAssign", global.getAoLot().getAlotNumber(), carrierID, "OUT", "INTRANS");
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_DEASSIGN)) {
			if (qTask == null && null != global.getAoLot() && !CommonUtility.isEmpty(carrierID)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("carrierDeassign", global.getAoLot().getAlotNumber(), carrierID, "INTRANS", "");
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_DEASSIGN)) {
			if (qTask == null && null != global.getAoLot() && !CommonUtility.isEmpty(carrierID)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("carrierDeassign", global.getAoLot().getAlotNumber(), carrierID, "INTRANS", "");
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_DEASSIGN_INPUT)) {
			if (qTask == null && null != global.getAoLot() && !CommonUtility.isEmpty(carrierID)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("carrierDeassign", global.getAoLot().getAlotNumber(), carrierID, "IN", "INTRANS");
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_DEASSIGN_OUTPUT)) {
			if (qTask == null && null != global.getAoLot() && !CommonUtility.isEmpty(carrierID)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("carrierDeassign", global.getAoLot().getAlotNumber(), carrierID, "OUT", "INTRANS");
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_IN_OUT_ASSIGN_IN)) {
			global.setCarrierID(carrierID);
			Intent i = new Intent(this, InOutCarrierAssignmentActivity.class);
			startActivity(i);
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_IN_OUT_ASSIGN_OUT)) {
			global.setCarrierID(carrierID);
			Intent i = new Intent(this, InOutCarrierAssignmentActivity.class);
			startActivity(i);
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_LOT_RACK_INIT)) {
			if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("checkCarrierForRackLot", carrierID);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_INIT)) {
			// if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
			// showProgress(true);
			// qTask = new QueryTask();
			// qTask.execute("getCarrierForRack", carrierID);
			// }
			if (!global.getCarrierID().equals(carrierID)) {
				global.setCarrierID(carrierID);
				Intent i = new Intent(this, RackMgmtActivity.class);
				startActivity(i);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_IN)) {
			// if (qTask == null && !CommonUtility.isEmpty(carrierID) && !CommonUtility.isEmpty(global.getRackName()) && !CommonUtility.isEmpty(global.getSlotName())) {
			// showProgress(true);
			// qTask = new QueryTask();
			// qTask.execute("putItemOnRack", carrierID);
			// }
			if (!global.getCarrierID().equals(carrierID)) {
				global.setCarrierID(carrierID);
				Intent i = new Intent(this, RackMgmtActivity.class);
				startActivity(i);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_OUT)) {
			// if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
			// // && !CommonUtility.isEmpty(global.getRackName()) && !CommonUtility.isEmpty(global.getSlotName())
			// showProgress(true);
			// qTask = new QueryTask();
			// qTask.execute("takeItemOffRack", carrierID);
			// }
			if (!global.getCarrierID().equals(carrierID)) {
				global.setCarrierID(carrierID);
				Intent i = new Intent(this, RackMgmtActivity.class);
				startActivity(i);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_INIT)) {
			// if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
			// showProgress(true);
			// qTask = new QueryTask();
			// qTask.execute("getCarrierForRack", carrierID);
			// }
			if (!global.getCarrierID().equals(carrierID)) {
				Intent intent = new Intent(this, PassWindowActivity.class);
				intent.putExtra("carrierID", carrierID);
				// global.setCarrierID(carrierID);
				startActivity(intent);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_IN)) {
			if (!global.getCarrierID().equals(carrierID)) {
				Intent intent = new Intent(this, PassWindowActivity.class);
				intent.putExtra("carrierID", carrierID);
				// global.setCarrierID(carrierID);
				startActivity(intent);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_PASS_WINDOW_OUT)) {
			if (!global.getCarrierID().equals(carrierID)) {
				Intent intent = new Intent(this, PassWindowActivity.class);
				intent.putExtra("carrierID", carrierID);
				// global.setCarrierID(carrierID);
				startActivity(intent);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_LOT_PASS_WINDOW)) {
			if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
				Intent intent = new Intent(NewNFCTagActivity.this, LotPassWindowActivity.class);
				intent.putExtra("carrierID", carrierID);
				startActivity(intent);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_LOGIN)) {
			if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("login", carrierID);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_LOT_INQUIRY)) {
			if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("lotInquiry", carrierID);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_END_LOT)) {
			if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("endLot", carrierID);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_CHECK_MAGAZINE_INIT)) {
			if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("checkMagInit", carrierID);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_CHECK_MAGAZINE)) {
			Intent i = new Intent(NewNFCTagActivity.this, CheckMagazineActivity.class);
			global.setCarrierID(carrierID);
			startActivity(i);
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_DIV_LOT_INIT)) {// add by B54521
			if (qTask == null) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("divLotInit", carrierID);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_REJ)) {
			Intent i = new Intent(NewNFCTagActivity.this, RejActivity.class);
			global.setCarrierID(carrierID);
			startActivity(i);
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_CASSETTE)) {
			Intent i = new Intent(NewNFCTagActivity.this, CassetteActivity.class);
			global.setCarrierID(carrierID);
			startActivity(i);
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_LOT_CARRIER_TRACKING)) {
			Intent i = new Intent(NewNFCTagActivity.this, LotCarrierTrackingActivity.class);
			i.putExtra("carrierID", carrierID);
			startActivity(i);
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_LOT_CARRIER_HIST)) {
			Intent i = new Intent(NewNFCTagActivity.this, LotCarrierHistActivity.class);
			global.setCarrierID(carrierID);
			startActivity(i);
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_REJ_MAG)) {
			Intent i = new Intent(NewNFCTagActivity.this, RejMagActivity.class);
			global.setCarrierID(carrierID);
			startActivity(i);
		}
	}

	private int tagScanCount = 0;

	@Override
	public void onNewIntent(Intent intent) {
		if (Constants.type == 5) {
			tagScanCount = 0;
		} else {
			setIntent(intent);
			resolveIntent(intent);
		}
	}

	private String getHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			int b = bytes[i] & 0xff;
			if (b < 0x10)
				sb.append('0');
			sb.append(Integer.toHexString(b));
		}
		return sb.toString();
	}

	@Override
	protected void setupActionBar() {
	}

	@Override
	public void onBackPressed() {
		finish();
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";
		String resultCode = "";
		String carrierID = "";
		String carrierName = "";
		String oldLotNumber = "";
		String newLotNumber = "";
		boolean showWifiConfig = false;
		private Spinner machSpinner;

		@Override
		protected BaseException doInBackground(String... params) {
			BaseException exception = null;
			cmdName = params[0];
			try {
				if (!cmdName.equals("scanBarcode")) {
					// CommonUtility.pingHost(global.getInterfaceMgrSocketConfigQuery().getHost());
				}
				if (cmdName.equals("checkTag")) {
					carrierID = params[1];
					checkTag(carrierID);
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
				} else if (cmdName.equals("assignByKeyinLotNumber")) {
					String alotNumberInput = params[1];
					String mCarrierID = params[2];
					String port = params[3];
					String comments = params[4];
					CommonTrans commonTrans = new CommonTrans();
					commonTrans.putLotIntoCarriers(apiExecutorQuery, apiExecutorUpdate, global, alotNumberInput, mCarrierID, port, comments, global.getMachID());
					global.setAoLot(new AOLot(alotNumberInput));
				} else if (cmdName.equals("getAlotByTag")) {
					carrierID = params[1];
					getAlotByTag(carrierID);
				} else if (cmdName.equals("carrierAssign")) {
					String alotNumber = params[1];
					String mCarrierID = params[2];
					String port = params[3];
					String comments = params[4];
					carrierAssign(alotNumber, mCarrierID, port, comments);
				} else if (cmdName.equals("carrierDeassign")) {
					String alotNumber = params[1];
					String mCarrierID = params[2];
					String port = params[3];
					String comments = params[4];
					carrierDeassign(alotNumber, mCarrierID, port, comments);
				} else if (cmdName.equals("getCarrierForRack")) {
					String mCarrierID = params[1];
					getCarrierForRack(mCarrierID);
				} else if (cmdName.equals("login")) {
					String mCarrierID = params[1];
					login(mCarrierID);
				} else if (cmdName.equals("lotInquiry")) {
					String mCarrierID = params[1];
					lotInquiry(mCarrierID);
				} else if (cmdName.equals("endLot")) {
					String mCarrierID = params[1];
					lotInquiry(mCarrierID);
				} else if (cmdName.equals("checkMagInit")) {
					String mCarrierID = params[1];
					lotInquiry(mCarrierID);
					carrierID = mCarrierID;
				} else if (cmdName.equals("divLotInit")) {// add by B54521
					String mCarrierID = params[1];
					lotInquiry(mCarrierID);
					carrierID = mCarrierID;
				} else if (cmdName.equals("deassignThenAssign")) {
					String oldLotNumber = params[1];
					String newLotNumber = params[2];
					String mCarrierID = params[3];
					String port = params[4];
					String comments = params[5];
					deassignThenAssign(mCarrierID, oldLotNumber, newLotNumber, port, comments);
				} else if (cmdName.equals("putItemOnRack")) {
					String mCarrierID = params[1];
					putItemOnRack(mCarrierID);
				} else if (cmdName.equals("takeItemOffRack")) {
					String mCarrierID = params[1];
					takeItemOffRack(mCarrierID);
				} else if (cmdName.equals("checkCarrierForRackLot")) {
					String mCarrierID = params[1];
					checkCarrierForRackLot(mCarrierID);
				}
			} catch (RfidWifiException e) {
				showWifiConfig = true;
				return e;
			} catch (BaseException e) {
				exception = e;
			}
			return exception;
		}

		private void checkCarrierForRackLot(String mCarrierID) throws ApiException, RfidException {
			String api = "getCarrierAttributes(carrierId='" + mCarrierID
					+ "', attributes='status,location,receiptDate,lotNumber,carrierName,carrierType,carrierLayer,carrierGroupId,cassetteOrMagazine,waferLotNumber')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "checkTag", api);
			if (CommonUtility.isEmpty(queryResult) || queryResult.size() > 1) {
				throw new RfidException("该RFID标签不存在或信息有误", classname, "checkCarrierForRackLot", api);
			}
			if (!CommonUtility.isEmpty(queryResult.get(0).get(5)) && queryResult.get(0).get(5).equals("RACK")) {
				String loc = queryResult.get(0).get(4);
				String[] temp = loc.split(":");
				global.setRackName(temp[0]);
				global.setSlotName(temp[1]);
				resultCode = "rack";
			} else if (!CommonUtility.isEmpty(queryResult.get(0).get(3)) && !queryResult.get(0).get(3).equalsIgnoreCase("None")) {
				String alotNumber = queryResult.get(0).get(3);
				AOLot aoLot = new AOLot();
				aoLot.setAlotNumber(alotNumber);
				global.setAoLot(aoLot);
				resultCode = "alotNumber";
			}
		}

		private void takeItemOffRack(String mCarrierID) throws BaseException {
			String api = "getCarrierAttributes(carrierId='" + mCarrierID + "', attributes='location,lotNumber,carrierName,carrierType,carrierLayer')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "takeItemOffRack", api);
			if (!CommonUtility.isEmpty(queryResult) && queryResult.size() == 1) {
				String alotNumber = queryResult.get(0).get(1);
				String carrierName = queryResult.get(0).get(2);
				// need to check location ?
				if (CommonUtility.isEmpty(alotNumber)) {
					throw new RfidException("该弹夹[" + carrierName + "]未对应物料", classname, "takeItemOffRack", api);
				}
				// if (global.getAoLot() == null) {
				// global.setAoLot(new AOLot(alotNumber)); // first magazine of this lot
				// } else {
				// if (!global.getAoLot().getAlotNumber().equals(alotNumber)) {
				// throw new RfidException("该弹夹[" + carrierName + "]属于另一批物料["+alotNumber+"]", classname, "takeItemOffRack", api);
				// }
				// }
				global.setAoLot(new AOLot(alotNumber));
				CommonTrans commonTrans = new CommonTrans();
				commonTrans.takeLotOffRack(apiExecutorQuery, apiExecutorUpdate, global, alotNumber);
			} else {
				throw new RfidException("该RFID标签不存在或信息有误", classname, "takeItemOffRack", api);
			}
		}

		private void putItemOnRack(String mCarrierID) throws BaseException {
			String api = "getCarrierAttributes(carrierId='" + mCarrierID + "', attributes='location,lotNumber,carrierName,carrierType,carrierLayer')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "putItemOnRack", api);
			if (!CommonUtility.isEmpty(queryResult) && queryResult.size() == 1) {
				String alotNumber = queryResult.get(0).get(1);
				String carrierName = queryResult.get(0).get(2);
				// need to check location ?
				if (CommonUtility.isEmpty(alotNumber)) {
					throw new RfidException("该弹夹[" + carrierName + "]未对应物料", classname, "putItemOnRack", api);
				}
				// if (global.getAoLot() == null) {
				// global.setAoLot(new AOLot(alotNumber)); // first magazine of this lot
				// } else {
				// if (!global.getAoLot().getAlotNumber().equals(alotNumber)) {
				// throw new RfidException("该弹夹[" + carrierName + "]属于另一批物料["+alotNumber+"]", classname, "putItemOnRack", api);
				// }
				// }
				global.setAoLot(new AOLot(alotNumber));
				CommonTrans commonTrans = new CommonTrans();
				commonTrans.putLotOnRack(apiExecutorQuery, apiExecutorUpdate, global, alotNumber);
			} else {
				throw new RfidException("该RFID标签不存在或信息有误", classname, "putItemOnRack", api);
			}
		}

		@Override
		protected void onPostExecute(BaseException exception) {
			qTask = null;
			showProgress(false);
			if (exception == null) {
				if (cmdName.equals("checkTag")) {
					checkTagAfter(carrierID);
				} else if (cmdName.equals("assignByKeyinLotNumber")) {
					if (CommonUtility.isEmpty(global.getScanTarget()) || global.getScanTarget().equals(Constants.SCAN_TARGET_INIT)) {
						global.setScanTarget(Constants.SCAN_TARGET_INIT);
						Intent intent = new Intent(NewNFCTagActivity.this, CarrierAssignActivity.class);
						startActivity(intent);
					} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_INIT)) {
						Intent intent = new Intent(NewNFCTagActivity.this, StripInspectionActivity.class);
						startActivity(intent);
					}
				} else if (cmdName.equals("getAlotByTag")) {
					getAlotByTagAfter(carrierID);
				} else if (cmdName.equals("carrierAssign")) {
					carrierAssignAfter();
				} else if (cmdName.equals("carrierDeassign")) {
					carrierDeassignAfter();
				} else if (cmdName.equals("login")) {
					checkTagAfter("");
				} else if (cmdName.equals("lotInquiry")) {
					checkTagAfter("");
				} else if (cmdName.equals("endLot")) {
					Intent intent = new Intent(NewNFCTagActivity.this, EndLotActivity.class);
					setResult(0, intent);
					finish();
				} else if (cmdName.equals("checkMagInit")) {
					global.setCarrierID(carrierID);
					Intent i = new Intent(NewNFCTagActivity.this, CheckMagazineActivity.class);
					startActivity(i);
				} else if (cmdName.equals("divLotInit")) {// add by B54521
					global.setCarrierID(carrierID);
					Intent i = new Intent(NewNFCTagActivity.this, DivLotActivity.class);
					startActivity(i);
				} else if (cmdName.equals("deassignThenAssign")) {
					deassignThenAssignAfter();
				} else if (cmdName.equals("getCarrierForRack") || cmdName.equals("putItemOnRack") || cmdName.equals("takeItemOffRack")) {
					Intent i = new Intent(NewNFCTagActivity.this, RackMgmtActivity.class);
					startActivity(i);
				} else if (cmdName.equals("checkCarrierForRackLot")) {
					Intent i = new Intent(NewNFCTagActivity.this, LotRackActivity.class);
					startActivity(i);
				}
			} else {
				logf(exception.toString());
				if (showWifiConfig) {
					WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
					if (wifiManager.isWifiEnabled()) {
						wifiManager.setWifiEnabled(false);
					} else {
						wifiManager.setWifiEnabled(true);
					}
				} else {
					// showError(exception.getErrorMsg());
					if (Constants.configFileName.equals("Testing.properties")) {
						msgView.setText(exception.toString());
					} else if (Constants.configFileName.equals("Production.properties")) {
						msgView.setText(exception.getErrorMsg());
					}
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void checkTag(String mCarrierID) throws BaseException {
			String api = "getCarrierAttributes(carrierId='" + mCarrierID
					+ "', attributes='status,location,receiptDate,lotNumber,carrierName,carrierType,carrierLayer,carrierGroupId,cassetteOrMagazine,waferLotNumber')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "checkTag", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				carrierName = queryResult.get(0).get(4);
				if (!CommonUtility.isEmpty(queryResult.get(0).get(5)) && queryResult.get(0).get(5).equalsIgnoreCase("RACK")) {
					String loc = queryResult.get(0).get(4);
					if (loc.contains(":")) {
						String[] temp = loc.split(":");
						global.setRackName(temp[0]);
						global.setSlotName(temp[1]);
						resultCode = "rack";
					}
				} else if (!CommonUtility.isEmpty(queryResult.get(0).get(5)) && queryResult.get(0).get(5).equalsIgnoreCase("USER")) {
					resultCode = "login";
					processGenesisLogin(carrierName);
				} else if (!CommonUtility.isEmpty(queryResult.get(0).get(3)) && !queryResult.get(0).get(3).equalsIgnoreCase("None")) {
					String alotNumber = queryResult.get(0).get(3);
					AOLot aoLot = new AOLot();
					aoLot.setAlotNumber(alotNumber);
					global.setAoLot(aoLot);
					resultCode = "alotNumber";
				} else if (!CommonUtility.isEmpty(queryResult.get(0).get(7)) && !queryResult.get(0).get(7).equalsIgnoreCase("None")) { // carrierGroupId
					resultCode = "cassette";
				} else if (!CommonUtility.isEmpty(queryResult.get(0).get(9)) && !queryResult.get(0).get(9).equalsIgnoreCase("None")) { // waferLotNumber
					resultCode = "cassette";
				} else if (!CommonUtility.isEmpty(queryResult.get(0).get(8)) && queryResult.get(0).get(8).equalsIgnoreCase("cassette")) { // cassetteOrMagazine
					resultCode = "cassette";
				} else {
					if (Constants.carrierAssignLoc) {
						api = "getCurrentUserMachAssignment(attributes='machId',userId='" + global.getUser().getUserID() + "',hostName='android-"
								+ global.getAndroidSecureID() + ".ap.freescale.net')";
						DataCollection result = apiExecutorQuery.query("Control", "getAssignedMach", api);
						machIdList.add("Location");
						for (ArrayList<String> temp : result) {
							machIdList.add(temp.get(0));
						}
					}
					resultCode = "keyIn";
				}
			} else {
				throw new RfidException("此RFID标签不存在。", classname, "checkTag", api);
			}
		}

		private void checkTagAfter(String mCarrierID) {
			if (CommonUtility.isEmpty(global.getScanTarget()) && null == global.getUser() && !resultCode.equals("login")) {
				msgView.setText("请先登录");
				return;
			}
			if (resultCode.equals("alotNumber")) {
				Intent intent = new Intent(NewNFCTagActivity.this, LotInquiryActivity.class);
				startActivity(intent);
			} else if (resultCode.equals("login")) {
				// Toast.makeText(getApplicationContext(), "登录成功", Toast.LENGTH_SHORT).show();
				// Intent intent = new Intent(NewNFCTagActivity.this, ControlActivity.class);
				// startActivity(intent);
				msgView.setText("登录成功 [" + global.getUser().getUserID() + " - " + global.getUser().getFirstName() + " " + global.getUser().getLastName() + "]");
			} else if (resultCode.equals("rack")) {
				global.setScanTarget(Constants.SCAN_TARGET_RACK_INIT);
				Intent i = new Intent(NewNFCTagActivity.this, RackMgmtActivity.class);
				startActivity(i);
			} else if (resultCode.equals("keyIn")) {
				formAlotNumberInputForm(mCarrierID);
			} else if (resultCode.equals("cassette")) {
				global.setCarrierID(carrierID);
				global.setScanTarget(Constants.SCAN_TARGET_CASSETTE);
				Intent intent = new Intent(NewNFCTagActivity.this, CassetteActivity.class);
				startActivity(intent);
			}
		}

		@SuppressLint("DefaultLocale")
		private void formAlotNumberInputForm(final String mCarrierID) {
			msgView.setText("此RFID标签[" + carrierName + "]未对应lot。点击F1扫描或输入lot号。");
			lotForm = (LinearLayout) findViewById(R.id.new_nfc_tag_lot_form);
			lotForm.setVisibility(View.VISIBLE);
			alotNumberAlertInput = (EditText) findViewById(R.id.new_nfc_tag_lot_input);
			alotNumberAlertInput.requestFocus();
			assignButton = (Button) findViewById(R.id.new_nfc_tag_assign);
			assignInputButton = (Button) findViewById(R.id.new_nfc_tag_assign_input);
			assignOutputButton = (Button) findViewById(R.id.new_nfc_tag_assign_output);
			machSpinner = (Spinner) findViewById(R.id.new_nfc_tag_location);
			if (Constants.carrierAssignLoc) {
				machSpinner.setVisibility(View.VISIBLE);
				machArrayAdapter = new ArrayAdapter<String>(NewNFCTagActivity.this, android.R.layout.simple_spinner_item, machIdList);
				machArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				machSpinner.setAdapter(machArrayAdapter);
				if (!CommonUtility.isEmpty(global.getMachID())) {
					int index = machIdList.indexOf(global.getMachID());
					if (index != -1) {
						machSpinner.setSelection(index);
					}
				} else {
					machSpinner.setSelection(0);
				}
				machSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						String location = machIdList.get(position);
						if (!CommonUtility.isEmpty(location) && !location.equalsIgnoreCase("location")) {
							global.setMachID(location);
						}
					}
					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
				});
			} else {
				machSpinner.setVisibility(View.GONE);
			}
			if (Constants.carrierAssignInputOutput) {
				assignButton.setVisibility(View.GONE);
				assignInputButton.setVisibility(View.VISIBLE);
				assignOutputButton.setVisibility(View.VISIBLE);
			} else {
				assignButton.setVisibility(View.VISIBLE);
				assignInputButton.setVisibility(View.GONE);
				assignOutputButton.setVisibility(View.GONE);
			}
			cancelButton = (Button) findViewById(R.id.new_nfc_tag_cancel);
			alotNumberInputHandler = new BaseHandler(alotNumberAlertInput);
			alotNumberAlertInput.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if (keyCode == 131 && qTask == null) {
						alotNumberAlertInput.setText("");
						qTask = new QueryTask();
						qTask.execute("scanBarcode");
					}
					return false;
				}
			});
			// for type 3
			alotNumberAlertInput.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					String tagId = s.toString();
					tagId = tagId.trim();
					if (tagId.length() == 14 && tagId.startsWith("1T")) {
						alotNumberAlertInput.setText(tagId.substring(2));
					}
				}
			});
			alotNumberAlertInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
					if (id == R.id.new_nfc_tag_search_t || id == EditorInfo.IME_NULL) {
						// String tagId = alotNumberAlertInput.getText().toString().trim();
						// if (tagId.length() == 14 && tagId.startsWith("1T")) {
						// alotNumberAlertInput.setText("");
						// log(tagId);
						// }
						return true;
					}
					return false;
				}
			});
			assignButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String alotNumberInput = alotNumberAlertInput.getText().toString().toUpperCase();
					if (!CommonUtility.isEmpty(alotNumberInput) && null == qTask) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("assignByKeyinLotNumber", alotNumberInput, mCarrierID, "INTRANS", "");
					}
				}
			});
			assignInputButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String alotNumberInput = alotNumberAlertInput.getText().toString().toUpperCase();
					if (!CommonUtility.isEmpty(alotNumberInput) && null == qTask) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("assignByKeyinLotNumber", alotNumberInput, mCarrierID, "IN", "INTRANS");
					}
				}
			});
			assignOutputButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String alotNumberInput = alotNumberAlertInput.getText().toString().toUpperCase();
					if (!CommonUtility.isEmpty(alotNumberInput) && null == qTask) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("assignByKeyinLotNumber", alotNumberInput, mCarrierID, "OUT", "INTRANS");
					}
				}
			});
			cancelButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					NewNFCTagActivity.this.finish();
				}
			});
			n7ScanBarcode = (Button) findViewById(R.id.new_nfc_tag_n7_scan_barcode);
			if (Constants.type == 1) {
				n7ScanBarcode.setVisibility(View.VISIBLE);
			}
			n7ScanBarcode.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent("com.google.zxing.client.android.SCAN");
					NewNFCTagActivity.this.startActivityForResult(intent, 0);
				}
			});
		}

		private void formDeassignToAssignForm(final String mCarrierID, final String mCarrierName, final String oldLotNumber, final String newLotNumber) {
			msgView.setText("此RFID标签[" + carrierName + "]已对应[" + oldLotNumber + "]，需要与该lot解绑并绑定到新lot[" + newLotNumber + "]吗？");
			lotForm = (LinearLayout) findViewById(R.id.new_nfc_tag_lot_form);
			lotForm.setVisibility(View.VISIBLE);
			alotNumberAlertInput = (EditText) findViewById(R.id.new_nfc_tag_lot_input);
			alotNumberAlertInput.setVisibility(View.GONE);
			assignButton = (Button) findViewById(R.id.new_nfc_tag_assign);
			assignButton.setText(R.string.button_yes);
			cancelButton = (Button) findViewById(R.id.new_nfc_tag_cancel);
			assignButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (null == qTask) {
						showProgress(true);
						qTask = new QueryTask();
						String port = "INTRANS";
						String comments = "";
						if (global.getScanTarget().equals(Constants.SCAN_TARGET_ASSIGN)) {
							port = "INTRANS";
							comments = "";
						} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_ASSIGN_INPUT)) {
							port = "IN";
							comments = "INTRANS";
						} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_ASSIGN_OUTPUT)) {
							port = "OUT";
							comments = "INTRANS";
						} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_ASSIGN)) {
							port = "INTRANS";
							comments = "";
						}
						qTask.execute("deassignThenAssign", oldLotNumber, newLotNumber, mCarrierID, port, comments);
					}
				}
			});
			cancelButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					NewNFCTagActivity.this.finish();
				}
			});
		}

		private void processGenesisLogin(String userid) throws BaseException {
			GenesisUser user = new GenesisUser();
			String API = "getMESParmValues(attributes='parmValue',parmOwnerType='APPL',parmOwner='SFC',parmName='genesisBaselineVersion')";
			ArrayList<ArrayList<String>> genesisVersion;
			genesisVersion = apiExecutorQuery.query(classname, "processGenesisLogin", API);
			String genesisVersionBaseLine = "";
			String sfcVersionString = "";
			if (genesisVersion.size() > 0) {
				genesisVersionBaseLine = genesisVersion.get(0).get(0);
				sfcVersionString = ",sfcVersion='" + genesisVersionBaseLine + "'";
			}

			API = "getUserAttributes(attributes='firstName,lastName,password,operation,department,failAttempt,failReason,lastSuccessLogin,supervisor', userId='" + userid
					+ "')";
			ArrayList<ArrayList<String>> users;
			users = apiExecutorQuery.query(classname, "processGenesisLogin", API);
			if (users.size() > 0) {
				user.setUserID(userid);
				user.setFirstName(users.get(0).get(0));
				user.setLastName(users.get(0).get(1));
				user.setPassword(users.get(0).get(2));
				user.setOperation(users.get(0).get(3));
				user.setDepartment(users.get(0).get(4));
				user.setFailAttempt(users.get(0).get(5));
				user.setFailReason(users.get(0).get(6));
				user.setLastSuccessLogin(users.get(0).get(7));
				user.setSupervisor(users.get(0).get(8));
			} else {
				throw new ApiException("非法SFC用户 ", classname, "processGenesisLogin", API);
			}

			if (CommonUtility.isEmpty(user.getPassword()) || user.getPassword().trim().equals("None")) {
				if (CommonUtility.isEmpty(user.getFailReason())) {
					user.setFailReason("");
				}
				if (CommonUtility.isEmpty(user.getFailAttempt())) {
					user.setFailAttempt("");
				}
				if (user.getFailAttempt().equals("0") && user.getFailReason().equals("SUSPENDED DUE TO TOO MANY LOGIN FAILURE ATTEMPTS")) {
					throw new ApiException("登录失败次数过多，账号被挂起 ", classname, "processGenesisLogin", API);
				} else {
					throw new ApiException("账号被挂起 ", classname, "processGenesisLogin", API);
				}
			}
			// String DBPasswordSecret = null;
			// password = XMLUtilities.unescapeXML(password);
			// String MatlMgrAPI = null;
			// String userPassword = user.getPassword().trim();
			// try {
			// DBPasswordSecret = SFCEncryptDecrypt.decrypt_API(userPassword);
			// } catch (Exception e) {
			// LoginInfo.setSuccess(false);
			// LoginInfo.setReason("Password is not encrypted!");
			// LoginInfo.setUser(user);
			// return LoginInfo;
			// }
			// String terminal = InterfaceMgrUtils.getHostName(machineHost);
			String terminal = global.getAndroidSecureID();
			// if (!password.equals(DBPasswordSecret)) {
			// MatlMgrAPI = "loginStatus(transUserId='" + userId + "', userId='" + userId + "',terminalId='" + terminal + "',loginSuccess='false'" + sfcVersionString + ")";
			// APIExecutor.transact(MatlMgrAPI);
			// if (!CommonUtilities.isEmpty(APIExecutor.getMessage())) {
			// LoginInfo.setSuccess(false);
			// LoginInfo.setReason(APIExecutor.getMessage());
			// LoginInfo.setUser(user);
			// return LoginInfo;
			// }
			// LoginInfo.setSuccess(false);
			// LoginInfo.setReason(LoginStatus.LOGIN_ERROR_FIELDS_INVALID);
			// LoginInfo.setUser(user);
			// return LoginInfo;
			// }

			String loginMessage = "User Id last login successful on " + user.getLastSuccessLogin() + " at " + terminal + ". ";

			if (Integer.parseInt(user.getFailAttempt().trim()) > 0) {
				// MatlMgrAPI = "execSql('select distinct uhist_failure_terminal,max(uhist_login_failure_date) from user_failure_login_hist where uhist_id=\\'" + userId.toUpperCase() +
				// "\\' group by uhist_failure_terminal order by max(uhist_login_failure_date)')";
				String MatlMgrAPI = "execSql(\\\"select distinct uhist_failure_terminal,max(uhist_login_failure_date) from user_failure_login_hist where uhist_id='"
						+ userid + "' group by uhist_failure_terminal order by max(uhist_login_failure_date)\\\")";
				ArrayList<ArrayList<String>> result;
				result = apiExecutorQuery.query(classname, "processGenesisLogin", MatlMgrAPI);
				if (!CommonUtility.isEmpty(result)) {
					ArrayList<String> col = new ArrayList<String>();
					for (ArrayList<String> row : result) {
						col = row;
					}
					loginMessage = "User Id last login failed on " + col.get(1).toString() + " at " + col.get(0).toString() + ". " + loginMessage;
				}
			}

			/* Check if user password expired and need to reset password */
			String matlMgrAPI = "execSql(\\\"select sysdate - user_last_pw_change from users where user_id='" + userid + "'\\\")";
			DataCollection result;
			result = apiExecutorQuery.query(classname, "processGenesisLogin", matlMgrAPI);
			String passwdChNumofDate = "";
			if (!CommonUtility.isEmpty(result)) {
				passwdChNumofDate = result.get(0).get(0);
				if (passwdChNumofDate.equals("None")) {
					throw new ApiException("密码被重置，请到SFC中修改密码 ", classname, "processGenesisLogin", matlMgrAPI);
				}
			}

			matlMgrAPI = "getMESParmValues(attributes='parmValue',parmOwnerType='USER',parmName='transRoles',parmOwner='" + userid + "')";
			result = apiExecutorQuery.query(classname, "processGenesisLogin", matlMgrAPI);
			ArrayList<String> expireValue = new ArrayList<String>();
			if (!CommonUtility.isEmpty(result)) {
				String[] roles = result.get(0).get(0).split(",");
				for (int i = 0; i < roles.length; i++) {
					matlMgrAPI = "getMESParmValues(attributes='parmValue',parmOwnerType='TROL',parmName='passwordExpiryCheck',parmOwner='" + roles[i].toString() + "')";
					DataCollection result1;
					result1 = apiExecutorQuery.query(classname, "processGenesisLogin", matlMgrAPI);
					if (!CommonUtility.isEmpty(result1)) {
						expireValue.add(result1.get(0).get(0));
					}
				}
			}
			int passedExpiredNumOfDate = 90;
			if (expireValue.size() > 0) {
				for (int i = 0; i < expireValue.size(); i++) {
					if (expireValue.get(i).equals("1")) {
						passedExpiredNumOfDate = 30;
						break;
					} else if (expireValue.get(i).equals("0")) {
						passedExpiredNumOfDate = 365;
					}
				}
			}

			if (passedExpiredNumOfDate - (Double.parseDouble(passwdChNumofDate)) <= 0) {
				throw new ApiException("密码过期，请到SFC中修改密码 ", classname, "processGenesisLogin", "");
			}

			/* Login successful */
			matlMgrAPI = "loginStatus(transUserId='" + userid + "', userId='" + userid + "',terminalId='" + terminal + "',loginSuccess='true'" + sfcVersionString + ")";
			apiExecutorUpdate.transact(classname, "processGenesisLogin", matlMgrAPI);
			// success
			global.setUser(user);

			SharedPreferences shared = NewNFCTagActivity.this.getSharedPreferences("RFID-data", MODE_PRIVATE);
			String oldUserId = shared.getString(Constants.USER_ID, "");
			if (!CommonUtility.isEmpty(oldUserId)) {
				String api = "getCurrentUserMachAssignment(attributes='machId',userId='" + oldUserId + "',hostName='android-" + global.getAndroidSecureID()
						+ ".ap.freescale.net')";
				DataCollection assignedMachData = apiExecutorQuery.query(classname, "processGenesisLogin", api);
				if (!CommonUtility.isEmpty(assignedMachData)) {
					List<String> apiCommandList = new ArrayList<String>();
					for (ArrayList<String> mach : assignedMachData) {
						String machID = mach.get(0);
						String transApi = "registerOperatorOnMachine(transUserId='" + oldUserId + "', machId='" + machID + "', stepName='', hostName='') ";
						apiCommandList.add(transApi);
					}
					CommonTrans commonTrans = new CommonTrans();
					String multiAPI = commonTrans.getMultipleAPI(global.getUser().getUserID(), apiCommandList);
					apiExecutorUpdate.transact(classname, "processGenesisLogin", multiAPI);
				}
			}

			CommonTrans commonTrans = new CommonTrans();
			String link = "servlet/LoginServlet?action=logUserLogin&coreID=" + user.getUserID() + "&deviceID=" + global.getAndroidSecureID();
			String output = "";
			try {
				output = commonTrans.queryFromServer(link);
				CommonUtility.logError("Login coreID=" + user.getUserID(), Constants.LOG_FILE_ERR);
			} catch (RfidException e) {
				CommonUtility.logError(user.getUserID() + " LoginServlet: " + e.toString(), Constants.LOG_FILE_ERR);
			}
			ControlActivity.lastAssignedMachCheck = 0;
			// String loggedOnTime = Constants.dateFormat.format(new Date());
			Editor e = shared.edit();
			e.putString(Constants.USER_ID, global.getUser().getUserID());
			e.putString(Constants.FIRST_NAME, global.getUser().getFirstName());
			e.putString(Constants.LAST_NAME, global.getUser().getLastName());
			e.putString(Constants.DEPARTMENT, global.getUser().getDepartment());
			e.putString(Constants.SERVER_CURRENT_DATE, output);
			e.remove("assignedMach");
			e.commit();
			global.getUser().setLastSuccessLogin(output);
		}

		private void getAlotByTag(String mCarrierID) throws BaseException {
			checkTag(mCarrierID);
			if (!resultCode.equals("alotNumber") && !resultCode.equals("keyIn")) {
				throw new RfidException("此RFID标签[" + carrierName + "]不是弹夹标签。", classname, "getAlotByTag", "CarrierID: " + mCarrierID);
			}
		}

		private void getAlotByTagAfter(String mCarrierID) {
			if (resultCode.equals("alotNumber")) {
				if (global.getScanTarget().equals(Constants.SCAN_TARGET_INIT)) {
					Intent intent = new Intent(NewNFCTagActivity.this, CarrierAssignActivity.class);
					startActivity(intent);
				} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_INIT)) {
					Intent intent = new Intent(NewNFCTagActivity.this, StripInspectionActivity.class);
					startActivity(intent);
				}
			} else if (resultCode.equals("keyIn")) {
				formAlotNumberInputForm(mCarrierID);
			}
		}

		private void carrierAssign(String mALotNumber, String mCarrierID, String port, String comments) throws BaseException {
			String api = "getCarrierAttributes(carrierId='" + mCarrierID + "', attributes='status,lotNumber,carrierName,carrierType')";
			DataCollection carrierInfo = apiExecutorQuery.query(classname, "carrierAssign", api);
			if (CommonUtility.isEmpty(carrierInfo)) {
				throw new RfidException("此RFID标签不存在。", classname, "carrierAssign", api);
			} else if (carrierInfo.get(0).get(0).equals("GOOD") && carrierInfo.get(0).get(1).equals("None")) {
				CommonTrans commonTrans = new CommonTrans();
				commonTrans.putLotIntoCarriers(apiExecutorQuery, apiExecutorUpdate, global, mALotNumber, mCarrierID, port, comments, global.getMachID());
			} else {
				if (!carrierInfo.get(0).get(1).equals(mALotNumber)) {
					resultCode = "deassignToAssign";
					carrierID = mCarrierID;
					carrierName = carrierInfo.get(0).get(2);
					// throw new RfidException("此RFID标签[" + carrierName + "]已被绑定。", classname, "carrierAssign", api);
					oldLotNumber = carrierInfo.get(0).get(1);
					newLotNumber = mALotNumber;
				}
			}
		}

		private void carrierDeassign(String mALotNumber, String mCarrierID, String port, String comments) throws BaseException {
			String api = "getCarrierAttributes(carrierId='" + mCarrierID + "', attributes='status,lotNumber,carrierName,carrierType')";
			DataCollection carrierInfo = apiExecutorQuery.query(classname, "carrierDeassign", api);
			if (CommonUtility.isEmpty(carrierInfo)) {
				throw new RfidException("此RFID标签不存在。", classname, "carrierDeassign", api);
			} else if (carrierInfo.get(0).get(0).equals("OCCUPIED") && carrierInfo.get(0).get(1).equals(mALotNumber)) {
				CommonTrans commonTrans = new CommonTrans();
				commonTrans.removeLotFromCarriers(apiExecutorQuery, apiExecutorUpdate, global, mALotNumber, mCarrierID, port, comments, global.getMachID());
			} else {
				carrierName = carrierInfo.get(0).get(2);
				throw new RfidException("此RFID标签[" + carrierName + "]未被绑定到当前lot[" + mALotNumber + "]。", classname, "carrierDeassign", api);
			}
		}

		private void carrierAssignAfter() {
			if (resultCode.equals("deassignToAssign")) {
				formDeassignToAssignForm(carrierID, carrierName, oldLotNumber, newLotNumber);
			} else {
				if (global.getScanTarget().equals(Constants.SCAN_TARGET_ASSIGN)) {
					Intent intent = new Intent(NewNFCTagActivity.this, CarrierAssignActivity.class);
					startActivity(intent);
				} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_ASSIGN)) {
					Intent intent = new Intent(NewNFCTagActivity.this, StripInspectionActivity.class);
					startActivity(intent);
				}
			}
		}

		private void carrierDeassignAfter() {
			if (global.getScanTarget().equals(Constants.SCAN_TARGET_DEASSIGN)) {
				Intent intent = new Intent(NewNFCTagActivity.this, CarrierAssignActivity.class);
				startActivity(intent);
			} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_DEASSIGN)) {
				Intent intent = new Intent(NewNFCTagActivity.this, StripInspectionActivity.class);
				startActivity(intent);
			}
		}

		private void getCarrierForRack(String mCarrierID) throws BaseException {
			String api = "getCarrierAttributes(carrierId='" + mCarrierID
					+ "', attributes='status,location,receiptDate,lotNumber,carrierName,carrierType,carrierLayer,carrierGroupId')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "getCarrierForRack", api);
			if (!CommonUtility.isEmpty(queryResult) && queryResult.size() == 1) {
				String carrierType = queryResult.get(0).get(5);
				if (!CommonUtility.isEmpty(carrierType) && carrierType.equals("RACK")) {
					String location = queryResult.get(0).get(4); // carrierName
					String[] temp = location.split(":");
					global.setRackName(temp[0]);
					global.setSlotName(temp[1]);
					// global.setAoLot(null);
					// global.setCarrierID("");
					// } else if (!CommonUtility.isEmpty(queryResult.get(0).get(3)) && !queryResult.get(0).get(3).equals("None")) {
					// global.setAoLot(new AOLot(queryResult.get(0).get(3)));
					// global.setCarrierID(carrierID);
				} else {
					throw new RfidException("不是rack标签", classname, "getCarrierForRack", api);
				}
			} else {
				throw new RfidException("标签不存在或标签信息有误。", classname, "getCarrierForRack", api);
			}
		}

		private void login(String mCarrierID) throws BaseException {
			checkTag(mCarrierID);
			if (resultCode.equals("login")) {
			} else {
				throw new RfidException("此RFID标签[" + carrierName + "]不是用户标签。", classname, "login", "CarrierID: " + mCarrierID);
			}
		}

		private void lotInquiry(String mCarrierID) throws BaseException {
			checkTag(mCarrierID);
			if (resultCode.equals("alotNumber")) {
			} else {
				throw new RfidException("此RFID标签[" + carrierName + "]未被绑定到lot。", classname, "lotInquiry", "CarrierID: " + mCarrierID);
			}
		}

		private void deassignThenAssign(String mCarrierID, String oldLotNumber, String newLotNumber, String port, String comments) throws BaseException {
			CommonTrans commonTrans = new CommonTrans();
			try {
				commonTrans.removeLotFromCarriers(apiExecutorQuery, apiExecutorUpdate, global, oldLotNumber, mCarrierID, "INTRANS", "", global.getMachID());
			} catch (RfidException e) {
				logf(e.toString());
			}
			commonTrans.putLotIntoCarriers(apiExecutorQuery, apiExecutorUpdate, global, newLotNumber, mCarrierID, port, comments, global.getMachID());
		}

		private void deassignThenAssignAfter() {
			lotForm.setVisibility(View.GONE);
			if (global.getScanTarget().equals(Constants.SCAN_TARGET_ASSIGN)) {
				Intent intent = new Intent(NewNFCTagActivity.this, CarrierAssignActivity.class);
				startActivity(intent);
			} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_SI_ASSIGN)) {
				Intent intent = new Intent(NewNFCTagActivity.this, StripInspectionActivity.class);
				startActivity(intent);
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

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null) {
			switch (resultCode) {
			case -1:
				String result = data.getStringExtra("SCAN_RESULT");
				if (!CommonUtility.isEmpty(result)) { // && result.trim().startsWith("1T")
					alotNumberAlertInput.setText(result.trim()); // .substring(2)
				}
				break;
			}
		}
	}

	@SuppressLint("DefaultLocale")
	private class ChiliNfcBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String receivedata = intent.getStringExtra("result");
			String carrierID = receivedata.split(" ")[1].substring(0, 8).toUpperCase();
			if (tagScanCount == 0) {
				checkScanTarget(carrierID);
				tagScanCount++;
				Intent cmdIntent = new Intent(NewNFCTagActivity.this, NFCService.class);
				cmdCode = 0;
				cmdIntent.putExtra("cmd", cmdCode);
				startService(cmdIntent);
			}
		}

	}

	@Override
	protected void onDestroy() {
		if (Constants.type == 5) {
			Intent cmdIntent = new Intent(NewNFCTagActivity.this, NFCService.class);
			cmdCode = 0;
			cmdIntent.putExtra("cmd", cmdCode);
			startService(cmdIntent);
			unregisterReceiver(chiliNfcBroadcastReceiver);
		}
		super.onDestroy();
	}
}
