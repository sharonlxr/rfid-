package com.fsl.cimei.rfid;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class LotInquiryFutureHoldSettingActivity extends BaseActivity {

	private QueryTask qTask = null;
	private ListView futureHoldSettingTitleListView;
	private List<HashMap<String, Object>> futureHoldSettingTitleListItem;
	private ArrayList<ArrayList<HashMap<String, Object>>> futureHoldSettingDetailListItem = new ArrayList<ArrayList<HashMap<String, Object>>>();
	private DataCollection futureHoldSetting;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_inquiry);
		mFormView = findViewById(R.id.inquiry_form);
		mStatusView = findViewById(R.id.lot_inquiry_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
//		((TextView) findViewById(R.id.title_lot_information)).setText(R.string.title_activity_lot_inquiry_future_hold_setting);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.lot_inquiry_tb_fragment);
		this.tagBarcodeInputFragment.getTagBarcodeInput().setVisibility(View.GONE);
		this.tagBarcodeInputFragment.getAlotTextView().setText(R.string.title_activity_lot_inquiry_future_hold_setting);
		futureHoldSettingTitleListView = (ListView) findViewById(R.id.inquiry_lot_info_list);
		futureHoldSettingTitleListItem = new ArrayList<HashMap<String, Object>>();
		futureHoldSetting = new DataCollection();
		if (null == qTask && null != global.getAoLot()) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("goToViewFutureHoldSetting");
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
				if (cmdName.equals("goToViewFutureHoldSetting")) {
					goToViewFutureHoldSetting();
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
				if (cmdName.equals("goToViewFutureHoldSetting")) {
					goToViewFutureHoldSettingAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotInquiryFutureHoldSettingActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotInquiryFutureHoldSettingActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void goToViewFutureHoldSetting() throws BaseException {
			String API = "getLotFutureHoldConditions(attributes='stepName,reasonCode,setUserId,setTime,comment,holdTypes'," + "lotNumber='"
					+ global.getAoLot().getAlotNumber() + "')";
			DataCollection dataContainer = apiExecutorQuery.query("LotInquiryFutureHoldSetting", "goToViewFutureHoldSetting", API);
			ArrayList<String> parmValues = new ArrayList<String>();
			for (int i = 0; i < dataContainer.size(); i++) {
				API = "getMESParmValues(attributes='parmValue',parmOwnerType='RCDE',parmName='description',parmOwner='" + dataContainer.get(0).get(1).trim() + "')";
				DataCollection reasonCode = apiExecutorQuery.query("LotInquiryFutureHoldSetting", "goToViewFutureHoldSetting", API);
				try {
					parmValues.add(URLDecoder.decode(reasonCode.get(0).get(0).replace("\\x", "%"), "GB2312"));
				} catch (UnsupportedEncodingException e) {
					throw new RfidException(e.toString(), "LotInquiryFutureHoldSetting", "goToViewFutureHoldSetting", API);
				}
			}
			for (int i = 0; i < dataContainer.size(); i++) {
				ArrayList<String> futureResult = new ArrayList<String>();
				futureResult.add(parmValues.get(i));
				for (int j = 0; j < dataContainer.get(0).size(); j++) {
					futureResult.add(dataContainer.get(i).get(j));
				}
				futureHoldSetting.add(futureResult);
			}
			if (CommonUtility.isEmpty(futureHoldSetting)) {
				throw new RfidException("No future hold setting information for this lot.", "LotInquiryFutureHoldSetting", "goToViewFutureHoldSetting", API);
			}
		}

		private void goToViewFutureHoldSettingAfter() {
			for (int i = 0; i < futureHoldSetting.size(); i++) {
				ArrayList<String> detail = futureHoldSetting.get(i);
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, detail.get(0) + " " + detail.get(1));
				futureHoldSettingTitleListItem.add(m);
				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				// Description
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.description));
				m1.put(Constants.ITEM_TEXT, detail.get(0));
				list.add(m1);
				// StepName
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.step_name));
				m2.put(Constants.ITEM_TEXT, detail.get(1));
				list.add(m2);
				// ReasonCode
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.reason_code));
				m3.put(Constants.ITEM_TEXT, detail.get(2));
				list.add(m3);
				// SetUserID
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.set_user_id));
				m4.put(Constants.ITEM_TEXT, detail.get(3));
				list.add(m4);
				// SetTime
				HashMap<String, Object> m5 = new HashMap<String, Object>();
				m5.put(Constants.ITEM_TITLE, getString(R.string.set_time));
				m5.put(Constants.ITEM_TEXT, detail.get(4));
				list.add(m5);
				// Comment
				HashMap<String, Object> m6 = new HashMap<String, Object>();
				m6.put(Constants.ITEM_TITLE, getString(R.string.comment));
				m6.put(Constants.ITEM_TEXT, detail.get(5));
				list.add(m6);
				// HoldType
				HashMap<String, Object> m7 = new HashMap<String, Object>();
				m7.put(Constants.ITEM_TITLE, getString(R.string.hold_type));
				m7.put(Constants.ITEM_TEXT, detail.get(6));
				list.add(m7);

				futureHoldSettingDetailListItem.add(list);
			}
			SimpleAdapter listItemAdapter = new SimpleAdapter(LotInquiryFutureHoldSettingActivity.this, futureHoldSettingTitleListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			futureHoldSettingTitleListView.setAdapter(listItemAdapter);
			futureHoldSettingTitleListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String stepName = (String) item.get(Constants.ITEM_TITLE);
					ArrayList<HashMap<String, Object>> temp = futureHoldSettingDetailListItem.get(position);
					SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotInquiryFutureHoldSettingActivity.this, temp, R.layout.lot_inquiry_list_item, new String[] {
							Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
					final ListView detailView = new ListView(LotInquiryFutureHoldSettingActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(detailListItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(LotInquiryFutureHoldSettingActivity.this);
					builder.setTitle(stepName).setIcon(android.R.drawable.ic_dialog_info).setView(detailView);
					builder.setPositiveButton(getResources().getString(R.string.close), null);
					builder.show();
				}
			});
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
