package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.MsgDBHelper;
import com.fsl.cimei.rfid.exception.BaseException;
import com.zkc.pc700.helper.ScanGpio;

public class RejMagActivity extends BaseActivity {

	private final String classname = "RejMag";
	private QueryTask qTask;
	private TextView machIdSelect;
	private LinearLayout machIdLine;
	private AlertDialog machIdAlertDialog = null;
	private String[] machIdArray;
	private String machID = "";
	private final String[] typeArr = new String[] { "INPUT", "INPUT_2", "OUTPUT" };
	private Spinner typeSpinner;
	private ArrayAdapter<String> typeArrayAdapter;
	private Button submitBtn;
	private ListView carrierListView;
	private ArrayAdapter<String> carrierListAdapter;
	private List<String> carrierList = new ArrayList<String>();
	private boolean isCarrierID = false;
	private boolean isCarrierName = false;
	private MsgDBHelper msgdb;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rej_mag);
		mFormView = findViewById(R.id.rej_mag_form);
		mStatusView = findViewById(R.id.rej_mag_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.rej_mag_tb_fragment);
		super.initTagBarcodeInput();
//		this.initBarcodeInput();

//		this.alotNumberTextView.setText(getResources().getString(R.string.carrier_name));
//		this.tagBarcodeInput.setHint("扫描弹夹条形码");
//		this.tagBarcodeInput.setInputType(InputType.TYPE_CLASS_TEXT);
		
		carrierListView = (ListView) findViewById(R.id.rej_mag_carrier_list);
		carrierListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, carrierList);
		carrierListView.setAdapter(carrierListAdapter);
		msgdb = new MsgDBHelper(RejMagActivity.this);
		
		machIdSelect = (TextView) findViewById(R.id.rej_mag_mach_ID);
		machIdLine = (LinearLayout) findViewById(R.id.rej_mag_ll1);
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

		typeSpinner = (Spinner) findViewById(R.id.rej_mag_type);
		typeArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, typeArr);
		typeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		typeSpinner.setAdapter(typeArrayAdapter);

		submitBtn = (Button) findViewById(R.id.rej_mag_submit);
		submitBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (qTask == null) {
					String magName = tagBarcodeInput.getText().toString();
					String mach = machIdSelect.getText().toString();
					String type = typeSpinner.getSelectedItem().toString();
					if (!mach.equals(getResources().getString(R.string.pls_select))) {
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("submit", magName, mach, type);
					}
				}
			}
		});
		global.setScanTarget(Constants.SCAN_TARGET_REJ_MAG);
	}

	@SuppressWarnings("unused")
	private void initBarcodeInput() {
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
				if (id == R.id.tb_fragment_search_t || id == EditorInfo.IME_NULL) {
					String tagId = tagBarcodeInput.getText().toString().trim();
					// if (tagId.length() == 14 && tagId.startsWith("1T")) {
					// tagBarcodeInput.setText("");
					// setLotNumber(tagId.substring(2));
					// } else if (tagId.length() == 16) {
					// tagBarcodeInput.setText("");
					// tagId = tagId.substring(0, tagId.length() / 2);
					// setTagId(tagId);
					// }
					tagBarcodeInput.setText(tagId);
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
					// } else if (keyCode == 131 && Constants.type == 5) {
					// chiliBarcodeScanThread.scan();
					// } else if ((keyCode == 134 || keyCode == 135 || keyCode == 132) && Constants.type == 5) {
					// Intent intent = new Intent(BaseActivity.this, NewNFCTagActivity.class);
					// intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
					// startActivity(intent);
				}
				return false;
			}
		});

		tagBarcodeInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@SuppressLint("DefaultLocale")
			@Override
			public void afterTextChanged(Editable s) {
				// String tagId = s.toString();
				// tagId = tagId.trim();
				// if (tagId.length() == 12) {
				// tagBarcodeInput.setText("");
				// setLotNumber(tagId.toUpperCase());
				// } else if (tagId.length() == 18 && tagId.endsWith(";")) {
				// tagBarcodeInput.setText("");
				// String a = tagId.substring(2, 14);
				// setLotNumber(a);
				// }
			}
		});
		// for type 1
		n7ScanBarcode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent("com.google.zxing.client.android.SCAN");
				RejMagActivity.this.startActivityForResult(intent, 0);
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
			// Intent nfcServiceIntent = new Intent(BaseActivity.this, NFCService.class);
			// startService(nfcServiceIntent);
			// barcode
			// chiliBarcodeHandler = new Handler() {
			// public void handleMessage(android.os.Message msg) {
			// if (msg.what == ScanThread.SCAN) {
			// String data = msg.getData().getString("data");
			// if (data.startsWith("1T")) {
			// tagBarcodeInput.setText(data.trim().substring(2));
			// }
			// }
			// };
			// };
			// try {
			// chiliBarcodeScanThread = new ScanThread(chiliBarcodeHandler);
			// chiliBarcodeScanThread.start();
			// } catch (SecurityException e) {
			// e.printStackTrace();
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
		}
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("getAssignedMach")) {
					getAssignedMach();
				} else if (cmdName.equals("submit")) {
					String magName = params[1];
					String mach = params[2];
					String type = params[3];
					submit(magName, mach, type);
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
				}
			} catch (BaseException e) {
				return e;
			}
			return null;
		}

		private void submit(String magName, String mach, String type) throws BaseException {
//			String api = "getCarrierAttributes(carrierName='" + magName
//					+ "', attributes='carrierId,status,location,receiptDate,lotNumber,carrierType,carrierLayer,carrierGroupId,cassetteOrMagazine,waferLotNumber')";
//			DataCollection queryResult = apiExecutorQuery.query(classname, "submit", api);
//			if (CommonUtility.isEmpty(queryResult)) {
//				throw new RfidException("此弹夹不存在。", classname, "submit", api);
//			}
//			String carrierId = queryResult.get(0).get(0);
//			String cmd = "CMD/A=\\\"EVENT_REPORT\\\" MID/A=\\\"" + mach + "\\\" MTY/A=\\\"E\\\" ECD/U4=0 ETX/A=\\\"\\\" EVENT_ID/A=\\\"LOAD\\\" PORT/U1=\\\"" + type + "\\\" ID/A=\\\"" + carrierId
//					+ "\\\" TYPE/A=\\\"MANUAL\\\"";
//			api = "sendPicEvent('" + mach + "','" + cmd + "','equip_box')";
//			apiExecutorUpdate.transact(classname, "submit", api);
			
//    def manualCarrierIdLoad(self,
//        carrierId          = None,
//        machId             = None,
//        port               = None):
//         
//    def manualCarrierNameLoad(self,
//        carrierName        = None,
//        machId             = None,
//        port               = None): 
			if (isCarrierID) {
				for (String cid : carrierList) {
					String api = "manualCarrierIdLoad(carrierId='" + cid + "',machId='" + mach + "',port='" + type + "')";
					logf(api);
					apiExecutorUpdate.transact(classname, "submit", api);
				}
			}
			if (isCarrierName) {
				for (String cid : carrierList) {
					String api = "manualCarrierNameLoad(carrierId='" + cid + "',machId='" + mach + "',port='" + type + "')";
					logf(api);
					apiExecutorUpdate.transact(classname, "submit", api);
				}
			}
			msgdb.updateType(mach, Constants.TYPE_MISSING);
		}

		@Override
		protected void onPostExecute(BaseException e) {
			qTask = null;
			showProgress(false);
			if (null == e) {
				if (cmdName.equals("getAssignedMach")) {
					getAssignedMachAfter();
				} else if (cmdName.equals("submit")) {
					submitAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(RejMagActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(RejMagActivity.this, e.getErrorMsg());
				}
			}
		}

		private void submitAfter() {
			Toast.makeText(getApplicationContext(), "提交成功", Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(RejMagActivity.this, ControlActivity.class);
			setResult(5, intent);
			finish();
//			tagBarcodeInput.setText("");
//			tagBarcodeInput.requestFocus();
//			machIdSelect.setText("");
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void getAssignedMach() throws BaseException {
			CommonTrans commonTrans = new CommonTrans();
			machIdArray = commonTrans.getAssignedMachArray(apiExecutorQuery, global);
		}

		private void getAssignedMachAfter() {
			machIdAlertDialog = new AlertDialog.Builder(RejMagActivity.this).setTitle("选择机台").setItems(machIdArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					machID = "" + machIdArray[which];
					machIdSelect.setText(machID);
				}
			}).setNegativeButton(getResources().getString(R.string.cancel), null).create();
			machIdAlertDialog.show();
		}
	}

	public void startScanBarcode() {
		if (qTask == null) {
			tagBarcodeInput.setText("");
			qTask = new QueryTask();
			qTask.execute("scanBarcode");
		}
	}

	@Override
	public void setBarcodeInput(String input) {
		// magazine barcode
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null) {
			switch (resultCode) {
			case -1:
				String result = data.getStringExtra("SCAN_RESULT");
				if (!CommonUtility.isEmpty(result)) {
					// tagBarcodeInput.setText(result.trim() + ";");
					if (isCarrierID) {
						isCarrierID = false;
						carrierList.clear();
					}
					isCarrierName = true;
					if (!carrierList.contains(result)) {
						carrierList.add(result);
						carrierListAdapter.notifyDataSetChanged();
					}
				}
				break;
			}
		}
	}
	
	@Override
	protected void onResume() {
		if (!CommonUtility.isEmpty(global.getCarrierID())) {
			String carrierID = global.getCarrierID();
			global.setCarrierID("");
			if (isCarrierName) {
				isCarrierName = false;
				carrierList.clear();
			}
			isCarrierID = true;
			if (!carrierList.contains(carrierID)) {
				carrierList.add(carrierID);
				carrierListAdapter.notifyDataSetChanged();
			}
		}
		super.onResume();
	}
}
