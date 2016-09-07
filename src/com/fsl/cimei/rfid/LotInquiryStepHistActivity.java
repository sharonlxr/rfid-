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

public class LotInquiryStepHistActivity extends BaseActivity {

	private QueryTask qTask = null;
	private ListView stepNameListView;
	private List<HashMap<String, Object>> stepNameListItem;
	private HashMap<String, ArrayList<HashMap<String, Object>>> stepDetailListItem = new HashMap<String, ArrayList<HashMap<String, Object>>>();
	private DataCollection lotStepHistory;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_inquiry);
		mFormView = findViewById(R.id.inquiry_form);
		mStatusView = findViewById(R.id.lot_inquiry_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		//((TextView) findViewById(R.id.title_lot_information)).setText(R.string.title_activity_lot_inquiry_step_hist);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.lot_inquiry_tb_fragment);
		this.tagBarcodeInputFragment.getTagBarcodeInput().setVisibility(View.GONE);
		this.tagBarcodeInputFragment.getAlotTextView().setText(R.string.title_activity_lot_inquiry_step_hist);
		stepNameListView = (ListView) findViewById(R.id.inquiry_lot_info_list);
		stepNameListItem = new ArrayList<HashMap<String, Object>>();
		lotStepHistory = new DataCollection();
		if (null == qTask && null != global.getAoLot()) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("getLotStepHistory");
		} else {
			showProgress(false);
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
				if (cmdName.equals("getLotStepHistory")) {
					getLotStepHistory();
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
				if (cmdName.equals("getLotStepHistory")) {
					getLotStepHistoryAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotInquiryStepHistActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotInquiryStepHistActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void getLotStepHistory() throws BaseException {
			String API = "getLotStepHistory(attributes='stepName,trakOper,startTime,startQty,endTime,"
					+ "endQty,rejQty,startUserId,endUserId,stepSeq,procName,verifierUserId',lotNumber='" + global.getAoLot().getAlotNumber() + "')";
			lotStepHistory = apiExecutorQuery.query("LotInquiryStepHist", "getLotStepHistory", API);
			if (CommonUtility.isEmpty(lotStepHistory)) {
				throw new RfidException("No history step.", "LotInquiryStepHist", "getLotStepHistory", API);
			}
		}

		private void getLotStepHistoryAfter() {
			for (ArrayList<String> step : lotStepHistory) {
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, step.get(0));
				stepNameListItem.add(m);

				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				// StepName
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.step_name));
				m1.put(Constants.ITEM_TEXT, step.get(0));
				list.add(m1);
				// CurrentTRAKOper
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.trak_oper));
				m2.put(Constants.ITEM_TEXT, step.get(1));
				list.add(m2);
				// StartTime
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.start_time));
				m3.put(Constants.ITEM_TEXT, step.get(2));
				list.add(m3);
				// StartQty
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.lot_start_qty));
				m4.put(Constants.ITEM_TEXT, step.get(3));
				list.add(m4);
				// EndTime
				HashMap<String, Object> m5 = new HashMap<String, Object>();
				m5.put(Constants.ITEM_TITLE, getString(R.string.end_time));
				m5.put(Constants.ITEM_TEXT, step.get(4));
				list.add(m5);
				// EndQty
				HashMap<String, Object> m6 = new HashMap<String, Object>();
				m6.put(Constants.ITEM_TITLE, getString(R.string.lot_end_qty));
				m6.put(Constants.ITEM_TEXT, step.get(5));
				list.add(m6);
				// RejQty
				HashMap<String, Object> m7 = new HashMap<String, Object>();
				m7.put(Constants.ITEM_TITLE, getString(R.string.rej_qty));
				m7.put(Constants.ITEM_TEXT, step.get(6));
				list.add(m7);
				// StartUser
				HashMap<String, Object> m8 = new HashMap<String, Object>();
				m8.put(Constants.ITEM_TITLE, getString(R.string.start_user));
				m8.put(Constants.ITEM_TEXT, step.get(7));
				list.add(m8);
				// EndUser
				HashMap<String, Object> m9 = new HashMap<String, Object>();
				m9.put(Constants.ITEM_TITLE, getString(R.string.end_user));
				m9.put(Constants.ITEM_TEXT, step.get(8));
				list.add(m9);
				// StepSeq
				HashMap<String, Object> m10 = new HashMap<String, Object>();
				m10.put(Constants.ITEM_TITLE, getString(R.string.step_seq));
				m10.put(Constants.ITEM_TEXT, step.get(9));
				list.add(m10);
				// ProcessName
				HashMap<String, Object> m11 = new HashMap<String, Object>();
				m11.put(Constants.ITEM_TITLE, getString(R.string.process_name));
				m11.put(Constants.ITEM_TEXT, step.get(10));
				list.add(m11);
				// VerifierUserID
				HashMap<String, Object> m12 = new HashMap<String, Object>();
				m12.put(Constants.ITEM_TITLE, getString(R.string.verifier_user_id));
				m12.put(Constants.ITEM_TEXT, step.get(11));
				list.add(m12);

				stepDetailListItem.put(step.get(0), list);
			}
			SimpleAdapter listItemAdapter = new SimpleAdapter(LotInquiryStepHistActivity.this, stepNameListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			stepNameListView.setAdapter(listItemAdapter);
			stepNameListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String stepName = (String) item.get(Constants.ITEM_TITLE);
					ArrayList<HashMap<String, Object>> temp = stepDetailListItem.get(stepName);
					SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotInquiryStepHistActivity.this, temp, R.layout.lot_inquiry_list_item, new String[] {
							Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
					final ListView detailView = new ListView(LotInquiryStepHistActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(detailListItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(LotInquiryStepHistActivity.this);
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
