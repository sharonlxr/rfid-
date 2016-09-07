package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class LotInquiryWaferInfoActivity extends BaseActivity {

	private QueryTask qTask = null;
	private ListView stepNameListView;
	private List<HashMap<String, Object>> stepNameListItem;
	private DataCollection waferInfo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_inquiry);
		mFormView = findViewById(R.id.inquiry_form);
		mStatusView = findViewById(R.id.lot_inquiry_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
//		((TextView) findViewById(R.id.title_lot_information)).setText(R.string.title_activity_lot_inquiry_wafer_info);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.lot_inquiry_tb_fragment);
		this.tagBarcodeInputFragment.getTagBarcodeInput().setVisibility(View.GONE);
		this.tagBarcodeInputFragment.getAlotTextView().setText(R.string.title_activity_lot_inquiry_wafer_info);
		stepNameListView = (ListView) findViewById(R.id.inquiry_lot_info_list);
		stepNameListItem = new ArrayList<HashMap<String, Object>>();
		waferInfo = new DataCollection();
		if (null == qTask && null != global.getAoLot()) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("goToWaferInfo");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.lot_inquiry, menu);
		return true;
	}

	public class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";
		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("goToWaferInfo")) {
					goToWaferInfo();
				}
			} catch (BaseException e) {
				return e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(BaseException e) {
			qTask = null;
			showProgress(false);
			if (null == e) {
				if (cmdName.equals("goToWaferInfo")) {
					goToWaferInfoAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotInquiryWaferInfoActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotInquiryWaferInfoActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void goToWaferInfo() throws BaseException {
			String API = "getWaferAttributes(attributes='waferNumber',lotNumber='" + global.getAoLot().getAlotNumber() + "')";
			waferInfo = apiExecutorQuery.query("LotInquiryWaferInfo", "goToWaferInfo", API);
			if (CommonUtility.isEmpty(waferInfo)) {
				throw new RfidException("No wafer ID information.", "LotInquiryWaferInfo", "goToWaferInfo", API);
			}
		}

		private void goToWaferInfoAfter() {
			for (ArrayList<String> step : waferInfo) {
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, step.get(0));
				stepNameListItem.add(m);
			}
			SimpleAdapter listItemAdapter = new SimpleAdapter(LotInquiryWaferInfoActivity.this, stepNameListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			stepNameListView.setAdapter(listItemAdapter);
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
