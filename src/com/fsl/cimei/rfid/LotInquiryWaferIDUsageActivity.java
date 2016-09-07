package com.fsl.cimei.rfid;

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

public class LotInquiryWaferIDUsageActivity extends BaseActivity {

	private QueryTask qTask = null;
	private ListView waferIDListView;
	private List<HashMap<String, Object>> waferIDListItem;
	private HashMap<String, ArrayList<HashMap<String, Object>>> waferIDUsageDetailListItem = new HashMap<String, ArrayList<HashMap<String, Object>>>();
	private DataCollection waferIDUsage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_inquiry);
		mFormView = findViewById(R.id.inquiry_form);
		mStatusView = findViewById(R.id.lot_inquiry_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
//		((TextView) findViewById(R.id.title_lot_information)).setText(R.string.title_activity_lot_inquiry_wafer_id_usage);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.lot_inquiry_tb_fragment);
		this.tagBarcodeInputFragment.getTagBarcodeInput().setVisibility(View.GONE);
		this.tagBarcodeInputFragment.getAlotTextView().setText(R.string.title_activity_lot_inquiry_wafer_id_usage);
		waferIDListView = (ListView) findViewById(R.id.inquiry_lot_info_list);
		waferIDListItem = new ArrayList<HashMap<String, Object>>();
		waferIDUsage = new DataCollection();
		if (null == qTask && null != global.getAoLot()) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("goToWaferIDUsage");
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
				if (cmdName.equals("goToWaferIDUsage")) {
					goToWaferIDUsage();
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
				if (cmdName.equals("goToWaferIDUsage")) {
					goToWaferIDUsageAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotInquiryWaferIDUsageActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotInquiryWaferIDUsageActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void goToWaferIDUsage() throws BaseException {
			String API = "getAlotWaferIdHists(attributes='stepName,waferId,transactType,transUserId,transTime'," + "lotNumber='" + global.getAoLot().getAlotNumber()
					+ "',procName='" + global.getAoLot().getCurrentStep().getProcName() + "',pseqNumber = " + global.getAoLot().getCurrentStep().getStepSeq()
					+ ",stepName='" + global.getAoLot().getCurrentStep().getStepName() + "')";
			waferIDUsage = apiExecutorQuery.query("LotInquiryWaferIDUsage", "goToWaferIDUsage", API);
			if (CommonUtility.isEmpty(waferIDUsage)) {
				throw new RfidException("没有wafer ID使用信息", "LotInquiryWaferIDUsage", "goToWaferIDUsage", API);
			}
		}

		private void goToWaferIDUsageAfter() {
			for (ArrayList<String> detail : waferIDUsage) {
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, detail.get(1));
				waferIDListItem.add(m);
				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				// StepName
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.step_name));
				m1.put(Constants.ITEM_TEXT, detail.get(0));
				list.add(m1);
				// WaferID
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.wafer_id));
				m2.put(Constants.ITEM_TEXT, detail.get(1));
				list.add(m2);
				// TransType
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.trans_type));
				m3.put(Constants.ITEM_TEXT, detail.get(2));
				list.add(m3);
				// TransTime
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.trans_time));
				m4.put(Constants.ITEM_TEXT, detail.get(3));
				list.add(m4);
				// UserID
				HashMap<String, Object> m5 = new HashMap<String, Object>();
				m5.put(Constants.ITEM_TITLE, getString(R.string.user_id));
				m5.put(Constants.ITEM_TEXT, detail.get(4));
				list.add(m5);
				// UserName
				HashMap<String, Object> m6 = new HashMap<String, Object>();
				m6.put(Constants.ITEM_TITLE, getString(R.string.user_name));
				m6.put(Constants.ITEM_TEXT, detail.get(5));
				list.add(m6);

				waferIDUsageDetailListItem.put(detail.get(0), list);
			}
			SimpleAdapter listItemAdapter = new SimpleAdapter(LotInquiryWaferIDUsageActivity.this, waferIDListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			waferIDListView.setAdapter(listItemAdapter);
			waferIDListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String stepName = (String) item.get(Constants.ITEM_TITLE);
					ArrayList<HashMap<String, Object>> temp = waferIDUsageDetailListItem.get(stepName);
					SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotInquiryWaferIDUsageActivity.this, temp, R.layout.lot_inquiry_list_item, new String[] {
							Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
					final ListView detailView = new ListView(LotInquiryWaferIDUsageActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(detailListItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(LotInquiryWaferIDUsageActivity.this);
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
