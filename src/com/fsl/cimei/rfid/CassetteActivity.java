package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class CassetteActivity extends BaseActivity {

	private QueryTask qTask = null;
	private EditText tagInput;
	private String carrierGroupId;
	private String waferLotNumber;
	private List<String> alotList = new ArrayList<String>();
	private ListView alotListView;
	private ArrayAdapter<String> alotAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cassette);
		mFormView = findViewById(R.id.cassette_form);
		mStatusView = findViewById(R.id.cassette_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		showProgress(false);
		alotListView = (ListView) findViewById(R.id.cassette_alot_list);
		alotAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, alotList);
		alotListView.setAdapter(alotAdapter);

		// for type 3, just for scan RFID tag
		tagInput = (EditText) findViewById(R.id.cassette_tag);
		tagInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.cassette_tag_search_t || id == EditorInfo.IME_NULL) {
					String tagId = tagInput.getText().toString().trim();
					if (tagId.length() == 16) {
						tagInput.setText("");
						tagId = tagId.substring(0, tagId.length() / 2);
						if (qTask == null) {
							showProgress(true);
							qTask = new QueryTask();
							qTask.execute("loadPage", tagId);
						}
					}
					return true;
				}
				return false;
			}
		});
	}

	@Override
	protected void onResume() {
		if (!CommonUtility.isEmpty(global.getCarrierID()) && qTask == null) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("loadPage", global.getCarrierID());
		}
		super.onResume();
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			BaseException exception = null;
			cmdName = params[0];
			try {
				if (cmdName.equals("loadPage")) {
					String carrierId = params[1];
					loadPage(carrierId);
				}
			} catch (BaseException e) {
				exception = e;
			}
			return exception;
		}

		@Override
		protected void onPostExecute(BaseException e) {
			qTask = null;
			showProgress(false);
			if (cmdName.equals("loadPage")) {
				loadPageAfter();
			}
			if (e == null) {
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(CassetteActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(CassetteActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void loadPage(String carrierId) throws BaseException {
			alotList.clear();
			String api = "getCarrierAttributes(attributes='carrierGroupId,waferLotNumber,status,carrierName',carrierId = '" + carrierId + "')";
			DataCollection dc = apiExecutorQuery.query("Cassette", "getWaferlotFromCarrier", api);
			if (!CommonUtility.isEmpty(dc)) {
				carrierGroupId = dc.get(0).get(0);
				waferLotNumber = dc.get(0).get(1);
				if (carrierGroupId.equalsIgnoreCase("None") && waferLotNumber.equalsIgnoreCase("None")) {
					throw new RfidException("提篮" + dc.get(0).get(3) + "未被使用", "Cassette", "loadPage", api);
				} else if (carrierGroupId.equalsIgnoreCase("None") && !CommonUtility.isEmpty(waferLotNumber)) { // 辅die
					alotList.add("Wafer Lot:   " + waferLotNumber);
					api = "getWaferLotAttributes(attributes='devcNumber',lotNumber='" + waferLotNumber + "')";
					dc = apiExecutorQuery.query("Cassette", "loadPage", api);
					if (!CommonUtility.isEmpty(dc)) {
						alotList.add("Devc Number: " + dc.get(0).get(0));
					}
					api = "getCarrierAttributes(attributes='carrierId,carrierName',waferLotNumber='" + waferLotNumber + "')";
					dc = apiExecutorQuery.query("Cassette", "loadPage", api);
					alotList.add("");
					alotList.add("Carrier Name");
					if (!CommonUtility.isEmpty(dc)) {
						for (ArrayList<String> temp : dc) {
							alotList.add(temp.get(1));
						}
					}
					api = "getWaferAttributes(attributes='waferNumber,startQty,currQty,waferStatus',waferLotNumber='" + waferLotNumber + "')";
					dc = apiExecutorQuery.query("Cassette", "loadPage", api);
					alotList.add("");
					alotList.add("Wafer Info");
					if (!CommonUtility.isEmpty(dc)) {
						for (ArrayList<String> temp : dc) {
							alotList.add(temp.get(0) + "  " + temp.get(1));
						}
					}
				} else if (waferLotNumber.equalsIgnoreCase("None") && !CommonUtility.isEmpty(carrierGroupId)) { // 主die
					alotList.add("Cassette Schedule ID: " + carrierGroupId);
					alotList.add("");
					api = "getLotAttributes(attributes='lotNumber',carrierGroupId = '" + carrierGroupId + "')";
					dc = apiExecutorQuery.query("Cassette", "loadPage", api);
					alotList.add("Current Lot Number");
					if (!CommonUtility.isEmpty(dc)) {
						for (ArrayList<String> temp : dc) {
							alotList.add(temp.get(0));
						}
					}
					api = "getCarrierAttributes(attributes='carrierId,carrierName',carrierGroupId='" + carrierGroupId + "')";
					dc = apiExecutorQuery.query("Cassette", "loadPage", api);
					alotList.add("");
					alotList.add("Carrier Name");
					if (!CommonUtility.isEmpty(dc)) {
						for (ArrayList<String> temp : dc) {
							alotList.add(temp.get(1));
						}
					}
				} else {
					throw new RfidException("该提篮未被使用", "Cassette", "loadPage", api);
				}
			} else {
				throw new RfidException("该标签不存在", "Cassette", "loadPage", api);
			}
		}

		private void loadPageAfter() {
			alotAdapter.notifyDataSetChanged();
		}

	}
}
