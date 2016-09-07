package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.entity.DataCollection;
import app.utils.login.genesis.GenesisUser;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.AOLot;
import com.fsl.cimei.rfid.entity.Carrier;

public class NFCTagEventActivity extends BaseActivity {

	private QueryTask qTask;
	private String carrierID = "";
	private String carrierName = "";
	private TextView alotNumberTextView;
	private String alotNumber = "";
	private LinearLayout assignedCarrierListView;
	private List<Carrier> assignedCarrierList;
	private Button scanToAssignButton;
	private Button scanToDeassignButton;
	private EditText carrierNameInput;
	// private boolean scanToAssign = false;
	// private boolean scanToDeassign = false;
	private Button submitButton;
	private NfcAdapter mAdapter;
	private PendingIntent mPendingIntent;
	private EditText alotNumberAlertInput;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		if (null == global.getUser()) {
//			Intent i = new Intent(NFCTagEventActivity.this, ControlActivity.class);
//			startActivity(i);
//		}
		setContentView(R.layout.activity_nfc_tag_event);
		mFormView = findViewById(R.id.nfc_tag_form);
		mStatusView = findViewById(R.id.nfc_tag_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		alotNumberTextView = (TextView) findViewById(R.id.nfc_tag_alot_number);
		assignedCarrierListView = (LinearLayout) findViewById(R.id.nfc_tag_assigned_list);
		scanToAssignButton = (Button) findViewById(R.id.nfc_tag_scan_assign);
		scanToDeassignButton = (Button) findViewById(R.id.nfc_tag_scan_deassign);
		carrierNameInput = (EditText) findViewById(R.id.nfc_tag_carrier_name_input);
		submitButton = (Button) findViewById(R.id.nfc_tag_assign_submit);
		assignedCarrierList = new ArrayList<Carrier>();
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		setupActionBar();
		resolveIntent(getIntent());
		mAdapter = NfcAdapter.getDefaultAdapter(this);
		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}

	@SuppressLint("DefaultLocale")
	private void resolveIntent(Intent intent) {
		String action = intent.getAction();
		log("NFCTagEventActivity resolveIntent");
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
			Parcelable parcelable = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			Tag tag = (Tag) parcelable;
			byte[] id = tag.getId();
			String infoStr = getHex(id).toUpperCase();
			carrierID = infoStr.substring(0, infoStr.length() / 2);
			log("tag " + carrierID);
			global.setTagUUID(carrierID);
			if (CommonUtility.isEmpty(global.getScanTarget())) {
				if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("checkTag");
				}
				// } else if (global.getScanTarget().equals(Constants.NFC_TAG_SCAN_TARGET_INIT)) {
				// if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
				// qTask = new QueryTask();
				// qTask.execute("getALotByTag");
				// }
			} else {
				if (null == global.getUser()) {
					Intent i = new Intent(NFCTagEventActivity.this, ControlActivity.class);
					startActivity(i);
				}
				if (global.getScanTarget().equals(Constants.SCAN_TARGET_ASSIGN)) {
					// scanToAssign = true;
					if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("carrierAssign");
					}
				} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_DEASSIGN)) {
					// scanToDeassign = true;
					if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("carrierDeassign");
					}
				} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_IN)) {
					if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("getCarrierForRack");
					}
				} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_RACK_OUT)) {
					if (qTask == null && !CommonUtility.isEmpty(carrierID)) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("getCarrierForRack");
					}
				} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_EMT_LOG)) {
					global.setCarrierID(carrierID);
					Intent i = new Intent(NFCTagEventActivity.this, EmtLogActivity.class);
					startActivity(i);
				} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_IN_OUT_ASSIGN_IN)) {
					global.setCarrierID(carrierID);
					Intent i = new Intent(NFCTagEventActivity.this, InOutCarrierAssignmentActivity.class);
					startActivity(i);
				} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_IN_OUT_ASSIGN_OUT)) {
					global.setCarrierID(carrierID);
					Intent i = new Intent(NFCTagEventActivity.this, InOutCarrierAssignmentActivity.class);
					startActivity(i);
				}
			}
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		log("NFCTagEventActivity onNewIntent");
		setIntent(intent);
		resolveIntent(intent);
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

	public class QueryTask extends AsyncTask<String, Void, String[]> {
		DataCollection queryResult = null;

		@SuppressLint("DefaultLocale")
		@Override
		protected String[] doInBackground(String... params) {
			StringBuilder errorMsg = new StringBuilder();
			String resultCode = "";
			String cmdName = params[0];
			log("NFCTagEventActivity " + cmdName);
			String tagID = "";
			if (params.length == 2) {
				tagID = params[1];
			} else {
				tagID = carrierID;
			}
			String tagName = "";
			try {
				if (cmdName.equals("getALotByTag")) {
					String api;
					// getCarrierAttributes,Parameters: attributes
					// (pkgCode,carrierId,carrierName,carrierType,carrierLayer,receiptDate,status,lotNumber,carrierGroupId,location,cassetteLotNumber,cassetteOrMagazine),pkgCode,carrierId,status,
					// lotNumber,carrierGroupId,location,cassetteLotNumber,cassetteOrMagazine
					api = "getCarrierAttributes(carrierId='" + carrierID
							+ "', attributes='status,location,receiptDate,lotNumber,carrierName,carrierType,carrierLayer,carrierGroupId')";
					queryResult = apiExecutorQuery.query(api);
					if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
						errorMsg.append(apiExecutorQuery.getMessage()).append(" ");
					} else {
						if (!CommonUtility.isEmpty(queryResult)) {
							if (!CommonUtility.isEmpty(queryResult.get(0).get(3)) && !queryResult.get(0).get(3).equals("None")) {
								alotNumber = queryResult.get(0).get(3);
								AOLot aoLot = new AOLot();
								aoLot.setAlotNumber(alotNumber);
								global.setAoLot(aoLot);
							} else {
								alotNumber = "";
								global.setAoLot(null);
								resultCode = "keyIn";
							}
						} else {
							errorMsg.append("标签不存在。");
						}
					}
					errorMsg.append(loadAssignedCarriers());
				} else if (cmdName.equals("carrierAssign")) {
					log("assign: " + alotNumber + " " + tagID);
					String api = "getCarrierAttributes(carrierId='" + tagID
							+ "', attributes='status,location,receiptDate,lotNumber,carrierName,carrierType,carrierLayer,carrierGroupId,carrierId')";
					DataCollection carrierInfo = apiExecutorQuery.query(api);
					if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
						errorMsg.append(apiExecutorQuery.getMessage());
					} else if (CommonUtility.isEmpty(carrierInfo) || 1 != carrierInfo.size()) {
						errorMsg.append("标签信息有误。");
					} else {
						tagName = carrierInfo.get(0).get(4);
						errorMsg.append(putLotIntoCarriers(alotNumber, tagID));
					}
					errorMsg.append(loadAssignedCarriers());
				} else if (cmdName.equals("carrierDeassign")) {
					log("de-assign: " + alotNumber + " " + tagID);
					String api = "getCarrierAttributes(carrierId='" + tagID
							+ "', attributes='status,location,receiptDate,lotNumber,carrierName,carrierType,carrierLayer,carrierGroupId,carrierId')";
					DataCollection carrierInfo = apiExecutorQuery.query(api);
					if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
						errorMsg.append(apiExecutorQuery.getMessage());
					} else if (CommonUtility.isEmpty(carrierInfo) || 1 != carrierInfo.size()) {
						/* carrier not existed */
						errorMsg.append("标签信息有误。");
					} else {
						errorMsg.append(removeLotFromCarriers(alotNumber, tagID));
					}
					errorMsg.append(loadAssignedCarriers());
				} else if (cmdName.equals("assignByCarrierName")) {
					String api = "getCarrierAttributes(carrierName='" + params[1].toUpperCase()
							+ "', attributes='status,location,receiptDate,lotNumber,carrierName,carrierType,carrierLayer,carrierGroupId,carrierId')";
					DataCollection carrierInfo = apiExecutorQuery.query(api);
					if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
						errorMsg.append(apiExecutorQuery.getMessage());
					} else if (CommonUtility.isEmpty(carrierInfo) || 1 != carrierInfo.size()) {
						/* carrier not existed */
						errorMsg.append("标签信息有误。");
					} else {
						tagID = carrierInfo.get(0).get(8);
						errorMsg.append(putLotIntoCarriers(alotNumber, tagID));
					}
					errorMsg.append(loadAssignedCarriers());
				} else if (cmdName.equals("assignByKeyinLotNumber")) {
					String alotNumberInput = params[1];
					String mCarrierID = params[2];
					errorMsg.append(putLotIntoCarriers(alotNumberInput, mCarrierID));
					if (CommonUtility.isEmpty(errorMsg.toString())) {
						alotNumber = alotNumberInput;
						global.setAoLot(new AOLot(alotNumber));
						errorMsg.append(loadAssignedCarriers());
						// global.setScanTarget(Constants.NFC_TAG_SCAN_TARGET_INIT);
					}
				} else if (cmdName.equals("getCarrierForRack")) {
					String api = "getCarrierAttributes(carrierId='" + carrierID
							+ "', attributes='status,location,receiptDate,lotNumber,carrierName,carrierType,carrierLayer,carrierGroupId')";
					queryResult = apiExecutorQuery.query(api);
					if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
						errorMsg.append(apiExecutorQuery.getMessage());
					} else {
						if (!CommonUtility.isEmpty(queryResult) && queryResult.size() == 1) {
							String carrierType = queryResult.get(0).get(5);
							if (!CommonUtility.isEmpty(carrierType) && carrierType.equals("RACK")) {
								String location = queryResult.get(0).get(1);
								String[] temp = location.split(":");
								global.setRackName(temp[0]);
								global.setSlotName(temp[1]);
								global.setAoLot(null);
								global.setCarrierID("");
							} else if (!CommonUtility.isEmpty(queryResult.get(0).get(3)) && !queryResult.get(0).get(3).equals("None")) {
								global.setAoLot(new AOLot(queryResult.get(0).get(3)));
								global.setCarrierID(carrierID);
							} else {
								errorMsg.append("标签信息有误。");
							}
						} else {
							errorMsg.append("标签信息有误。");
						}
					}
				} else if (cmdName.equals("checkTag")) {
					String[] result = checkTag();
					errorMsg.append(result[0]);
					resultCode = result[1];
				} else if (cmdName.equals("loadAssignedCarriers")) {
					errorMsg.append(loadAssignedCarriers());
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
				}
			} catch (Exception e) {
				errorMsg.append(e.toString());
			}

			return new String[] { cmdName, errorMsg.toString(), tagID, tagName, resultCode };
		}

		@SuppressLint("InflateParams")
		@Override
		protected void onPostExecute(final String[] result) {
			qTask = null;
			showProgress(false);
			String cmdName = result[0];
			String errorMsg = result[1];
			String resultCode = result[4];
			
			if (cmdName.equals("checkTag") && resultCode.equals("login")) {
			} else {
				if (null == global.getUser()) {
					Intent i = new Intent(NFCTagEventActivity.this, ControlActivity.class);
					startActivity(i);
				}
			}
			
			alotNumberTextView.setText(alotNumber);
			assignedCarrierListView.removeAllViews();
			for (Carrier c : assignedCarrierList) {
				LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.carrier_assign_list_item, null);
				TextView tagNameTextView = (TextView) l.findViewById(R.id.carrier_assign_tag_name);
				tagNameTextView.setText(c.getTagName());
				Button deassignButton = (Button) l.findViewById(R.id.carrier_assign_button_deassign);
				// DeassignButtonOnClickListener listener = new DeassignButtonOnClickListener(c.getTagID());
				// deassignButton.setOnClickListener(listener);
				deassignButton.setVisibility(View.INVISIBLE);
				assignedCarrierListView.addView(l);
			}
			// if (null == global.getAoLot()) {
			// scanToAssignButton.setEnabled(false);
			// scanToDeassignButton.setEnabled(false);
			// submitButton.setEnabled(false);
			// } else {
			// scanToAssignButton.setEnabled(true);
			// scanToDeassignButton.setEnabled(true);
			// submitButton.setEnabled(true);
			// }
			if (CommonUtility.isEmpty(errorMsg)) {
				// query success
				if (cmdName.equals("getALotByTag")) {
					// Intent i = new Intent(NFCTagEventActivity.this, CarrierAssignActivity.class);
					// startActivity(i);
				} else if (cmdName.equals("carrierAssign")) {
					// Intent i = new Intent(NFCTagEventActivity.this, CarrierAssignActivity.class);
					// startActivity(i);
				} else if (cmdName.equals("carrierDeassign")) {
					// Intent i = new Intent(NFCTagEventActivity.this, CarrierAssignActivity.class);
					// startActivity(i);
				} else if (cmdName.equals("assignByCarrierName")) {
					// Intent i = new Intent(NFCTagEventActivity.this, CarrierAssignActivity.class);
					// startActivity(i);
				} else if (cmdName.equals("getCarrierForRack")) {
					Intent i = new Intent(NFCTagEventActivity.this, RackMgmtActivity.class);
					startActivity(i);
				} else if (cmdName.equals("checkTag")) {
					checkTagAfter(resultCode);
				} else if (cmdName.equals("assignByKeyinLotNumber")) {
					if (CommonUtility.isEmpty(global.getScanTarget())) {
						scanToAssignButton.setVisibility(View.VISIBLE);
						scanToDeassignButton.setVisibility(View.VISIBLE);
						scanToAssignButton.setText(R.string.button_assign);
						scanToDeassignButton.setText(R.string.button_deassign);
					}
				}
			} else {
				// query fail
				AlertDialog.Builder builder = new AlertDialog.Builder(NFCTagEventActivity.this);
				builder.setTitle("出错了");
				builder.setMessage(errorMsg);
//				builder.setPositiveButton("返回", new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						Intent i = new Intent(NFCTagEventActivity.this, CarrierAssignActivity.class);
//						startActivity(i);
//					}
//				});
				builder.setNegativeButton(getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						NFCTagEventActivity.this.finish();
					}
				});
				builder.show();
				// showError(errorMsg);
				// if (cmdName.equals("assignByKeyinLotNumber")) {
				// NFCTagEventActivity.this.finish();
				// }
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private String putLotIntoCarriers(String mALotNumber, String mCarrierID) throws Exception {
			String errorMsg = "";
			String api = "getCurrentStepContext(attributes='procName, stepSeq, stepName', lotNumber='" + mALotNumber + "')";
			DataCollection lotInfo = apiExecutorQuery.query(api);
			if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
				errorMsg = apiExecutorQuery.getMessage();
			} else if (CommonUtility.isEmpty(lotInfo)) {
				/* Lot not existed */
				errorMsg = "Lot [" + mALotNumber + "] 不存在";
			} else {
				String procName = lotInfo.get(0).get(0);
				String stepSeq = lotInfo.get(0).get(1);
				String stepName = lotInfo.get(0).get(2);
				api = "putLotIntoCarriers(transUserId='" + global.getUser().getUserID() + "', lotNumber='" + mALotNumber + "', carrierIdList=['" + mCarrierID + "',])";
				apiExecutorQuery.query(api);
				if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
					errorMsg = apiExecutorQuery.getMessage();
				} else {
					api = "setAlotCarrierHists(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + mALotNumber + "', stepName='" + stepName + "', procName='"
							+ procName + "',pseqNumber=" + stepSeq + ",transId='START',port='INTRANS', carrierIdList=['" + mCarrierID + "',])";
					apiExecutorQuery.query(api);
					if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
						errorMsg = apiExecutorQuery.getMessage();
					}
				}
			}
			return errorMsg;
		}

		private String removeLotFromCarriers(String mALotNumber, String mCarrierID) throws Exception {
			String errorMsg = "";
			String api = "removeLotFromCarriers(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + mALotNumber + "',carrierIdList=['" + mCarrierID + "',])";
			apiExecutorQuery.query(api);
			if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
				errorMsg = apiExecutorQuery.getMessage();
			} else {
				api = "getCurrentStepContext(attributes='procName,stepSeq,stepName',lotNumber='" + mALotNumber + "')";
				DataCollection lotInfo = apiExecutorQuery.query(api);
				if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
					errorMsg = apiExecutorQuery.getMessage();
				} else if (CommonUtility.isEmpty(lotInfo)) {
					errorMsg = "Lot不在step";
				} else {
					String procName = lotInfo.get(0).get(0);
					String stepSeq = lotInfo.get(0).get(1);
					String stepName = lotInfo.get(0).get(2);
					api = "setAlotCarrierHists(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + mALotNumber + "',stepName='" + stepName + "',procName='"
							+ procName + "',pseqNumber=" + stepSeq + ",transId='END',port='INTRANS',carrierIdList=['" + mCarrierID + "',])";
					apiExecutorQuery.query(api);
					if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
						errorMsg = apiExecutorQuery.getMessage();
					}
				}
			}
			return errorMsg;
		}

		private String loadAssignedCarriers() throws Exception {
			String errorMsg = "";
			assignedCarrierList.clear();
			if (!CommonUtility.isEmpty(alotNumber)) {
				String api = "getCarrierAttributes(lotNumber='" + alotNumber + "', attributes='carrierId,carrierName')";
				queryResult = apiExecutorQuery.query(api);
				if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
					errorMsg = apiExecutorQuery.getMessage();
				} else {
					if (!CommonUtility.isEmpty(queryResult)) {
						for (ArrayList<String> temp : queryResult) {
							Carrier c = new Carrier();
							c.setTagID(temp.get(0));
							c.setTagName(temp.get(1));
							assignedCarrierList.add(c);
						}
					}
				}
			}
			return errorMsg;
		}

		private String[] checkTag() throws Exception {
			StringBuilder errorMsg = new StringBuilder();
			String resultCode = "";
			String api = "getCarrierAttributes(carrierId='" + carrierID
					+ "', attributes='status,location,receiptDate,lotNumber,carrierName,carrierType,carrierLayer,carrierGroupId,cassetteOrMagazine')";
			queryResult = apiExecutorQuery.query(api);
			if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
				errorMsg.append(apiExecutorQuery.getMessage());
			} else {
				if (!CommonUtility.isEmpty(queryResult)) {
					if (!CommonUtility.isEmpty(queryResult.get(0).get(5)) && queryResult.get(0).get(5).equals("RACK")) {
						String loc = queryResult.get(0).get(1);
						String[] temp = loc.split(":");
						global.setRackName(temp[0]);
						global.setSlotName(temp[1]);
						resultCode = "rack";
					} else if (!CommonUtility.isEmpty(queryResult.get(0).get(5)) && queryResult.get(0).get(5).equals("USER")) {
						String userid = queryResult.get(0).get(4);
						errorMsg.append(processGenesisLogin(userid));
						// if success, put it in global, turn to control screen
						resultCode = "login";
					} else if (!CommonUtility.isEmpty(queryResult.get(0).get(3)) && !queryResult.get(0).get(3).equals("None")) {
						alotNumber = queryResult.get(0).get(3);
						AOLot aoLot = new AOLot();
						aoLot.setAlotNumber(alotNumber);
						global.setAoLot(aoLot);
						resultCode = "alotNumber";
					} else {
						alotNumber = "";
						// global.setAoLot(null);
						global.setRackName("");
						global.setSlotName("");
						global.setCarrierID("");
						resultCode = "keyIn";
						carrierName = queryResult.get(0).get(4);
					}
				} else {
					alotNumber = "";
					global.setAoLot(null);
					global.setRackName("");
					global.setSlotName("");
					global.setCarrierID("");
					errorMsg.append("标签不存在。");
				}
			}
			// errorMsg.append(loadAssignedCarriers());
			return new String[] { errorMsg.toString(), resultCode };
		}

		private String processGenesisLogin(String userid) throws Exception {
			String errorMsg = "";

	        GenesisUser user = new GenesisUser();
	        String API = "getMESParmValues(attributes='parmValue',parmOwnerType='APPL',parmOwner='SFC',parmName='genesisBaselineVersion')";
	        ArrayList< ArrayList< String>> genesisVersion = apiExecutorQuery.query(API);
	        if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
	            return apiExecutorQuery.getMessage();
	        }
	        String genesisVersionBaseLine = "";
	        String sfcVersionString = "";
	        if (genesisVersion.size() > 0) {
	            genesisVersionBaseLine = genesisVersion.get(0).get(0);
	            sfcVersionString = ",sfcVersion='" + genesisVersionBaseLine + "'";
	        }

	        API = "getUserAttributes(attributes='firstName,lastName,password,operation,department,failAttempt,failReason,lastSuccessLogin,supervisor', userId='" + userid + "')";
	        ArrayList< ArrayList< String>> users = apiExecutorQuery.query(API);
	        if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
	            return apiExecutorQuery.getMessage();
	        }
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
	            return "非法SFC用户 ";
	        }

	        if (CommonUtility.isEmpty(user.getPassword()) || user.getPassword().trim().equals("None")) {
	            if (CommonUtility.isEmpty(user.getFailReason())) {
	                user.setFailReason("");
	            }
	            if (CommonUtility.isEmpty(user.getFailAttempt())) {
	                user.setFailAttempt("");
	            }
	            if (user.getFailAttempt().equals("0") && user.getFailReason().equals("SUSPENDED DUE TO TOO MANY LOGIN FAILURE ATTEMPTS")) {
	            	return "登录失败次数过多，账号被挂起 ";
	            } else {
	                return "账号被挂起 ";
	            }
	        }
//	        String DBPasswordSecret = null;
//	        password = XMLUtilities.unescapeXML(password);
//	        String MatlMgrAPI = null;
//	        String userPassword = user.getPassword().trim();
//	        try {
//	            DBPasswordSecret = SFCEncryptDecrypt.decrypt_API(userPassword);
//	        } catch (Exception e) {
//	            LoginInfo.setSuccess(false);
//	            LoginInfo.setReason("Password is not encrypted!");
//	            LoginInfo.setUser(user);
//	            return LoginInfo;
//	        }
//	        String terminal = InterfaceMgrUtils.getHostName(machineHost);
	        String terminal = global.getAndroidSecureID();
//	        if (!password.equals(DBPasswordSecret)) {
//	            MatlMgrAPI = "loginStatus(transUserId='" + userId + "', userId='" + userId + "',terminalId='" + terminal + "',loginSuccess='false'" + sfcVersionString + ")";
//	            APIExecutor.transact(MatlMgrAPI);
//	            if (!CommonUtilities.isEmpty(APIExecutor.getMessage())) {
//	                LoginInfo.setSuccess(false);
//	                LoginInfo.setReason(APIExecutor.getMessage());
//	                LoginInfo.setUser(user);
//	                return LoginInfo;
//	            }
//	            LoginInfo.setSuccess(false);
//	            LoginInfo.setReason(LoginStatus.LOGIN_ERROR_FIELDS_INVALID);
//	            LoginInfo.setUser(user);
//	            return LoginInfo;
//	        }

	        String loginMessage = "User Id last login successful on " + user.getLastSuccessLogin() + " at " + terminal + ". ";

	        if (Integer.parseInt(user.getFailAttempt().trim()) > 0) {
//	            MatlMgrAPI = "execSql('select distinct uhist_failure_terminal,max(uhist_login_failure_date) from user_failure_login_hist where uhist_id=\\'" + userId.toUpperCase() + "\\' group by uhist_failure_terminal order by max(uhist_login_failure_date)')";
	            String MatlMgrAPI = "execSql(\\\"select distinct uhist_failure_terminal,max(uhist_login_failure_date) from user_failure_login_hist where uhist_id='" + userid + "' group by uhist_failure_terminal order by max(uhist_login_failure_date)\\\")";
	            ArrayList< ArrayList< String>> result = apiExecutorQuery.query(MatlMgrAPI);
	            if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
	                return apiExecutorQuery.getMessage();
	            }
	            if (!CommonUtility.isEmpty(result)) {
	                ArrayList<String> col = new ArrayList<String>();
	                for (ArrayList<String> row : result) {
	                    col = row;
	                }
	                loginMessage = "User Id last login failed on " + col.get(1).toString() + " at " + col.get(0).toString() + ". " + loginMessage;
	            }
	        }

	        /*Check if user password expired and need to reset password*/
	        String MatlMgrAPI = "execSql(\\\"select sysdate - user_last_pw_change from users where user_id='" + userid + "'\\\")";
	        DataCollection result = apiExecutorQuery.query(MatlMgrAPI);
	        if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
	            return apiExecutorQuery.getMessage();
	        }
	        String passwdChNumofDate = "";
	        if (!CommonUtility.isEmpty(result)) {
	            passwdChNumofDate = result.get(0).get(0);
	            if (passwdChNumofDate.equals("None")) {
	                return "密码被重置，请到SFC中修改密码 ";
	            }
	        }

	        MatlMgrAPI = "getMESParmValues(attributes='parmValue',parmOwnerType='USER',parmName='transRoles',parmOwner='" + userid + "')";
	        result = apiExecutorQuery.query(MatlMgrAPI);
	        if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
	        	return apiExecutorQuery.getMessage();
	        }
	        ArrayList<String> expireValue = new ArrayList<String>();
	        if (!CommonUtility.isEmpty(result)) {
	            String[] roles = result.get(0).get(0).split(",");
	            for (int i = 0; i < roles.length; i++) {
	                MatlMgrAPI = "getMESParmValues(attributes='parmValue',parmOwnerType='TROL',parmName='passwordExpiryCheck',parmOwner='" + roles[i].toString() + "')";
	                DataCollection result1 = apiExecutorQuery.query(MatlMgrAPI);
	                if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
	                    return apiExecutorQuery.getMessage();
	                }
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
	            return "密码过期，请到SFC中修改密码 ";
	        }

	        /* Login successful */
	        MatlMgrAPI = "loginStatus(transUserId='" + userid + "', userId='" + userid
	                + "',terminalId='" + terminal + "',loginSuccess='true'" + sfcVersionString + ")";
	        apiExecutorQuery.transact(MatlMgrAPI);
	        if (!CommonUtility.isEmpty(apiExecutorQuery.getMessage())) {
	            return apiExecutorQuery.getMessage();
	        } else {
	            // sucess
	        	global.setUser(user);
	        	SharedPreferences shared = NFCTagEventActivity.this.getSharedPreferences("RFID-data", MODE_PRIVATE);
				String loggedOnTime = Constants.dateFormat.format(new Date());
				Editor e = shared.edit();
				e.putString(Constants.USER_ID, global.getUser().getUserID());
				e.putString(Constants.FIRST_NAME, global.getUser().getFirstName());
				e.putString(Constants.LAST_NAME, global.getUser().getLastName());
				e.putString(Constants.DEPARTMENT, global.getUser().getDepartment());
				e.putString(Constants.SERVER_CURRENT_DATE, loggedOnTime);
				global.getUser().setLastSuccessLogin(loggedOnTime);
				e.commit();
	        	CommonTrans commonTrans = new CommonTrans();
				commonTrans.checkUserInfo(apiExecutorQuery, global);
	        }
			
			return errorMsg;
		}

		@SuppressLint("DefaultLocale")
		private void checkTagAfter(String resultCode) {
			if (resultCode.equals("rack")) {
				global.setAoLot(null);
				global.setCarrierID("");
				Intent i = new Intent(NFCTagEventActivity.this, RackMgmtActivity.class);
				startActivity(i);
			} else if (resultCode.equals("keyIn")) {
				// final TextView infoView = new TextView(NFCTagEventActivity.this);
				// infoView.setText(carrierName);
				// AlertDialog.Builder builder = new AlertDialog.Builder(NFCTagEventActivity.this);
				// builder.setTitle("此RFID标签未对应lot").setIcon(android.R.drawable.ic_dialog_info).setView(infoView);
				// builder.setNegativeButton(getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
				// @Override
				// public void onClick(DialogInterface dialog, int which) {
				// NFCTagEventActivity.this.finish();
				// }
				// });
				// builder.show();
				alotNumberAlertInput = new EditText(NFCTagEventActivity.this);
				alotNumberInputHandler = new BaseHandler(alotNumberAlertInput);
				alotNumberAlertInput.setOnKeyListener(new View.OnKeyListener() {
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						if (keyCode == 131 && qTask == null) {
							qTask = new QueryTask();
							qTask.execute("scanBarcode");
						}
						return false;
					}
				});
				// if (global.getAoLot() != null) {
				// inputServer.setText(global.getAoLot().getAlotNumber());
				// }
				AlertDialog.Builder builder = new AlertDialog.Builder(NFCTagEventActivity.this);
				builder.setTitle("此RFID标签[" + carrierName + "]未对应lot。点击F1扫描或输入lot号。").setIcon(android.R.drawable.ic_dialog_info).setView(alotNumberAlertInput);
				builder.setPositiveButton(getResources().getString(R.string.button_assign), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String alotNumberInput = alotNumberAlertInput.getText().toString().toUpperCase();
						if (!CommonUtility.isEmpty(alotNumberInput) && null == qTask) {
							qTask = new QueryTask();
							qTask.execute("assignByKeyinLotNumber", alotNumberInput, carrierID);
						}
					}
				});
				builder.setNegativeButton(getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						NFCTagEventActivity.this.finish();
					}
				});
				builder.show();
			} else if (resultCode.equals("alotNumber")) {
				Intent intent = new Intent(NFCTagEventActivity.this, LotInquiryActivity.class);
				startActivity(intent);
			}  else if (resultCode.equals("login")) {
				Intent intent = new Intent(NFCTagEventActivity.this, ControlActivity.class);
				startActivity(intent);
			}
		}
	}

	@Override
	protected void onResume() {
		if (mAdapter != null) {
			mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
		}
		submitButton.setVisibility(View.INVISIBLE);
		carrierNameInput.setVisibility(View.INVISIBLE);
		if (CommonUtility.isEmpty(global.getScanTarget())) {
			if (null == global.getAoLot()) {
				scanToAssignButton.setVisibility(View.INVISIBLE);
				scanToDeassignButton.setVisibility(View.INVISIBLE);
			} else {
				scanToAssignButton.setVisibility(View.VISIBLE);
				scanToDeassignButton.setVisibility(View.VISIBLE);
			}
			scanToAssignButton.setText(R.string.button_assign);
			scanToDeassignButton.setText(R.string.button_deassign);
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_ASSIGN)) {
			scanToAssignButton.setText(R.string.button_done);
			scanToAssignButton.setVisibility(View.VISIBLE);
			scanToDeassignButton.setText(R.string.button_deassign);
			scanToDeassignButton.setVisibility(View.INVISIBLE);
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_DEASSIGN)) {
			scanToAssignButton.setText(R.string.button_assign);
			scanToAssignButton.setVisibility(View.INVISIBLE);
			scanToDeassignButton.setText(R.string.button_done);
			scanToDeassignButton.setVisibility(View.VISIBLE);
		}

		scanToAssignButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (scanToAssignButton.getText().equals(getResources().getString(R.string.button_done))) {
					scanToAssignButton.setText(R.string.button_assign);
					// scanToAssignFlag = false;
					// scanToDeassignButton.setEnabled(true);
					scanToDeassignButton.setVisibility(View.VISIBLE);
					// global.setScanTarget(Constants.NFC_TAG_SCAN_TARGET_INIT);
					global.setScanTarget("");
				} else {
					scanToAssignButton.setText(R.string.button_done);
					// scanToAssignFlag = true;
					// scanToDeassignButton.setEnabled(false);
					scanToDeassignButton.setVisibility(View.INVISIBLE);
					global.setScanTarget(Constants.SCAN_TARGET_ASSIGN);
				}
			}
		});

		scanToDeassignButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (scanToDeassignButton.getText().equals(getResources().getString(R.string.button_done))) {
					scanToDeassignButton.setText(R.string.button_deassign);
					// scanToDeassignFlag = false;
					// scanToAssignButton.setEnabled(true);
					scanToAssignButton.setVisibility(View.VISIBLE);
					// global.setScanTarget(Constants.NFC_TAG_SCAN_TARGET_INIT);
					global.setScanTarget("");
				} else {
					scanToDeassignButton.setText(R.string.button_done);
					// scanToDeassignFlag = true;
					// scanToAssignButton.setEnabled(false);
					scanToAssignButton.setVisibility(View.INVISIBLE);
					global.setScanTarget(Constants.SCAN_TARGET_DEASSIGN);
				}
			}
		});

		submitButton.setOnClickListener(new OnClickListener() {
			@SuppressLint("DefaultLocale")
			@Override
			public void onClick(View v) {
				String carrierName = carrierNameInput.getText().toString().toUpperCase();
				if (!CommonUtility.isEmpty(carrierName) && null == qTask) {
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("assignByCarrierName", carrierName);
				}
			}
		});

		if (null != global.getAoLot()) {
			alotNumber = global.getAoLot().getAlotNumber();
			alotNumberTextView.setText(global.getAoLot().getAlotNumber());
			if (null == qTask) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("loadAssignedCarriers");
			}
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (mAdapter != null) {
			mAdapter.disableForegroundDispatch(this);
		}
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
}
