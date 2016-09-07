package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.AOLot;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class LotPassWindowActivity extends BaseActivity {

	private QueryTask qTask = null;
	private List<Map<String, String>> lotInfoListItem = new ArrayList<Map<String, String>>();
	private SimpleAdapter listItemAdapter;
	private ListView lotListView;
	private final String classname = "LotPassWindow";
	private final String LOT = "LOT";
	private final String PASS_WINDOW = "PASS_WINDOW";
	private final String CARRIERS = "CARRIERS";
	private final String TRANS_TIME = "TRANS_TIME";
	private String lot = "";
	private String passWindowName = "";
	private String passWindowSlot = "";
	private int lotCount = 0; // load by passwindow
	private String tagID = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_pass_window);
		setupActionBar();
		mFormView = findViewById(R.id.lot_pass_window_form);
		mStatusView = findViewById(R.id.lot_pass_window_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.lot_pass_window_tb_fragment);
		super.initTagBarcodeInput();
		lotListView = (ListView) findViewById(R.id.lot_pass_window_list);
		listItemAdapter = new SimpleAdapter(LotPassWindowActivity.this, lotInfoListItem, R.layout.lot_pass_window_list_item, new String[] { LOT, PASS_WINDOW, TRANS_TIME,
				CARRIERS }, new int[] { R.id.lot_pass_window_item_lot, R.id.lot_pass_window_item_pass_window, R.id.lot_pass_window_item_trans_time,
				R.id.lot_pass_window_item_carrier_count });
		lotListView.setAdapter(listItemAdapter);
		lotListView.setOnItemClickListener(new OnItemClickListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				HashMap<String, String> item = (HashMap<String, String>) parent.getItemAtPosition(position);
				if (!CommonUtility.isEmpty(item.get(LOT))) {
					global.setAoLot(new AOLot(item.get(LOT)));
					Intent i = new Intent(LotPassWindowActivity.this, LotInquiryActivity.class);
					startActivity(i);
				}
			}
		});
	}

	public class QueryTask extends AsyncTask<String, Void, BaseException> {
		DataCollection queryResult = null;
		String cmdName = "";

		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("loadByLot")) {
					loadByLot();
				} else if (cmdName.equals("loadByPassWindow")) {
					loadByPassWindow();
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
				} else if (cmdName.equals("checkCarrierID")) {
					checkCarrierID();
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
			tagBarcodeInput.requestFocus();
			if (null == e) {
				if (cmdName.equals("loadByLot")) {
					alotNumberTextView.setText(lot);
					loadAfter();
				} else if (cmdName.equals("loadByPassWindow")) {
					alotNumberTextView.setText(passWindowName + ":" + passWindowSlot + " 共" + lotCount + "批料");
					loadAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotPassWindowActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotPassWindowActivity.this, e.getErrorMsg());
				}
			}
		}

		private void checkCarrierID() throws BaseException {
			String api = "getCarrierAttributes(carrierId='" + LotPassWindowActivity.this.tagID
					+ "', attributes='status,location,receiptDate,lotNumber,carrierName,carrierType,carrierLayer,carrierGroupId,cassetteOrMagazine,waferLotNumber')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "checkCarrierID", api);
			if (!CommonUtility.isEmpty(queryResult) || queryResult.size() != 1) {
				String carrierName;
				if (!CommonUtility.isEmpty(queryResult.get(0).get(4)) && !queryResult.get(0).get(4).equalsIgnoreCase("None")) {
					carrierName = queryResult.get(0).get(4);
				} else {
					carrierName = tagID;
				}
				if (!CommonUtility.isEmpty(queryResult.get(0).get(5)) && queryResult.get(0).get(5).equals("RACK") && carrierName.contains(":")) { // Pass Window
					String[] temp = carrierName.split(":");
					passWindowName = temp[0];
					passWindowSlot = temp[1];
					lot = "";
					cmdName = "loadByPassWindow";
					loadByPassWindow();
				} else if (!CommonUtility.isEmpty(queryResult.get(0).get(3)) && !queryResult.get(0).get(3).equalsIgnoreCase("None")) { // lot
					lot = queryResult.get(0).get(3);
					passWindowName = "";
					passWindowSlot = "";
					cmdName = "loadByLot";
					loadByLot();
				} else {
					throw new RfidException("此RFID标签[" + carrierName + "]未对应物料或pass through window", classname, "checkCarrierID", api);
				}
			} else {
				throw new RfidException("此RFID标签不存在或有误。", classname, "checkCarrierID", api);
			}
		}

		@SuppressLint("InflateParams")
		private void loadAfter() {
			listItemAdapter.notifyDataSetChanged();
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		String basicQueryTransTime1 = "execSql('select c.alot_number,max(h.trans_time) from carriers c, aolot_carrier_hists h "
				+ "where c.alot_number=h.alot_number and c.carrier_id=h.carrier_id and h.trans_type=\\\\'RACK_IN\\\\' and c.location=h.location and ";
		String basicQueryTransTime2 = " group by c.alot_number')";

		private void loadByLot() throws BaseException {
			// String api = "getRackCarrierLots(attributes='carrierId,carrierName,rackName,rackSlot,lotNumber',lotNumberList=['" + lot + "'])";
			// String api = "getCarrierAttributes(lotNumber='" + lot + "',attributes='carrierId,carrierName,location,lotNumber')";
			String api = "execSql('select c.carrier_id,c.carrier_name,r.rack_name,r.rack_slot,c.alot_number from rack_contents r,carriers c where r.content=c.carrier_id and c.alot_number=\\\\'"+lot+"\\\\'')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "loadByLot", api);
			DataCollection lotTransTime = apiExecutorQuery.query(classname, "loadByLot", basicQueryTransTime1 + "c.alot_number=\\\\'" + lot + "\\\\'"
					+ basicQueryTransTime2);
			formLotPassWindowCarrierListItem(queryResult, lotTransTime);
		}

		private void loadByPassWindow() throws BaseException {
			// String api = "getRackCarrierLots(attributes='carrierId,carrierName,rackName,rackSlot,lotNumber',rackList=[('" + passWindowName + "','" + passWindowSlot + "')])";
			// String api = "getCarrierAttributes(location='" + passWindowName + ":" + passWindowSlot + "',attributes='carrierId,carrierName,location,lotNumber')";
			String api = "execSql('select c.carrier_id,c.carrier_name,r.rack_name,r.rack_slot,c.alot_number from rack_contents r,carriers c where r.content=c.carrier_id and r.rack_name=\\\\'" + passWindowName + "\\\\' and r.rack_slot=\\\\'" + passWindowSlot + "\\\\'')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "loadByPassWindow", api);
			DataCollection lotTransTime = apiExecutorQuery.query(classname, "loadByPassWindow", basicQueryTransTime1 + "c.location=\\\\'" + passWindowName + ":"
					+ passWindowSlot + "\\\\'" + basicQueryTransTime2);
			formLotPassWindowCarrierListItem(queryResult, lotTransTime);
		}

		private void formLotPassWindowCarrierListItem(DataCollection queryResult, DataCollection lotTransTime) {
			Map<String, Map<String, List<String>>> lotPassWindowCarrier = new HashMap<String, Map<String, List<String>>>();
			for (ArrayList<String> temp : queryResult) {
				String lot = temp.get(4);
				Map<String, List<String>> passWindowCarrier;
				if (lotPassWindowCarrier.containsKey(lot)) {
					passWindowCarrier = lotPassWindowCarrier.get(lot);
				} else {
					passWindowCarrier = new HashMap<String, List<String>>();
					lotPassWindowCarrier.put(lot, passWindowCarrier);
				}
				String passWindow = temp.get(2) + ":" + temp.get(3);
				List<String> carrierList;
				if (passWindowCarrier.containsKey(passWindow)) {
					carrierList = passWindowCarrier.get(passWindow);
				} else {
					carrierList = new ArrayList<String>();
					passWindowCarrier.put(passWindow, carrierList);
				}
				carrierList.add(temp.get(1));
			}
			lotInfoListItem.clear();
			lotCount = lotPassWindowCarrier.keySet().size();
			for (String lot : lotPassWindowCarrier.keySet()) {
				boolean first = true;
				for (String passWindow : lotPassWindowCarrier.get(lot).keySet()) {
					Map<String, String> line = new HashMap<String, String>();
					if (first) {
						line.put(LOT, lot);
						first = false;
					} else {
						line.put(LOT, "");
					}
					line.put(PASS_WINDOW, passWindow);
					List<String> carrierList = lotPassWindowCarrier.get(lot).get(passWindow);
					StringBuilder sBuilder = new StringBuilder();
					for (String t : carrierList) {
						sBuilder.append(",  ").append(t);
					}
					line.put(CARRIERS, "[ " + sBuilder.toString().substring(1) + " ]");
					String transTime = "";
					for (ArrayList<String> temp : lotTransTime) {
						if (temp.get(0).equals(lot)) {
							transTime = temp.get(1);
						}
					}
					line.put(TRANS_TIME, transTime);
					lotInfoListItem.add(line);
				}
			}
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (null != qTask) {
			qTask.cancel(true);
		}
		super.onPause();
	}

	public void setBarcodeInput(String input) {
		// lot, passWindow
		if (input.contains(".") || input.contains(":")) { // passWindow
			String[] temp = null;
			if (input.contains(".")) {
				temp = input.split("\\.");
			} else if (input.contains(":")) {
				temp = input.split(":");
			}
			passWindowName = temp[0];
			passWindowSlot = temp[1];
			if (qTask == null) {
				alotNumberTextView.setText(passWindowName + ":" + passWindowSlot);
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("loadByPassWindow");
			}
		} else {
			if (qTask == null) {
				lot = input;
				alotNumberTextView.setText(lot);
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("loadByLot");
			}
		}
	}

	@Override
	public void setTagId(String tagId) {
		this.tagID = tagId;
		showProgress(true);
		qTask = new QueryTask();
		qTask.execute("checkCarrierID");
	}

	public void startScanBarcode() {
		if (qTask == null) {
			tagBarcodeInput.setText("");
			qTask = new QueryTask();
			qTask.execute("scanBarcode");
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		String carrierID = intent.getStringExtra("carrierID");
		if (!CommonUtility.isEmpty(carrierID) && !carrierID.equals(this.tagID)) {
			setTagId(carrierID);
		}
		super.onNewIntent(intent);
	}
}
