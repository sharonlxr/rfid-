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

public class LotInquiryCPKDataActivity extends BaseActivity {

	private QueryTask qTask = null;
	private ListView cpkTitleListView;
	private List<HashMap<String, Object>> cpkTitleListItem;
	private List<ArrayList<HashMap<String, Object>>> cpkDetailListItem = new ArrayList<ArrayList<HashMap<String, Object>>>();
	private DataCollection cpkData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_inquiry);
		mFormView = findViewById(R.id.inquiry_form);
		mStatusView = findViewById(R.id.lot_inquiry_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		//((TextView) findViewById(R.id.title_lot_information)).setText(R.string.title_activity_lot_inquiry_cpk_data);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.lot_inquiry_tb_fragment);
		this.tagBarcodeInputFragment.getTagBarcodeInput().setVisibility(View.GONE);
		this.tagBarcodeInputFragment.getAlotTextView().setText(R.string.title_activity_lot_inquiry_cpk_data);
		cpkTitleListView = (ListView) findViewById(R.id.inquiry_lot_info_list);
		cpkTitleListItem = new ArrayList<HashMap<String, Object>>();
		cpkData = new DataCollection();
		if (null == qTask && null != global.getAoLot()) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("goToViewCPKData");
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
				if (cmdName.equals("goToViewCPKData")) {
					goToViewCPKData();
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
				if (cmdName.equals("goToViewCPKData")) {
					goToViewCPKDataAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotInquiryCPKDataActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotInquiryCPKDataActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void goToViewCPKData() throws BaseException {
			String ProcNameResult = global.getAoLot().getCurrentStep().getProcName();
			String StepSeqResult = global.getAoLot().getCurrentStep().getStepSeq();
			String stepNameSession = global.getAoLot().getCurrentStep().getStepName();
			String API = "getCpkValue(attributes='cpkType,cpkCode,cpkName,cpkControlLimit,cpkValue,cpkKControlLimit, cpkKValue, setTime, setUserId'," + "lotNumber='"
					+ global.getAoLot().getAlotNumber() + "',stepName='" + stepNameSession + "')";
			DataCollection dataContainer = apiExecutorQuery.query("LotInquiryCPKData", "goToViewCPKData", API);
			for (int i = 0; i < dataContainer.size(); i++) {
				ArrayList<String> stepResult = new ArrayList<String>();
				stepResult.add(ProcNameResult);
				stepResult.add(StepSeqResult);
				stepResult.add(stepNameSession);
				for (int l = 0; l < dataContainer.get(0).size(); l++) {
					stepResult.add(dataContainer.get(i).get(l));
				}
				cpkData.add(stepResult);
			}
			if (CommonUtility.isEmpty(cpkData)) {
				throw new RfidException("没有CPK数据信息", "LotInquiryCPKData", "goToViewCPKData", API);
			}
		}

		private void goToViewCPKDataAfter() {
			for (int i = 0; i < cpkData.size(); i++) {
				ArrayList<String> detail = cpkData.get(i);
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, detail.get(5));
				cpkTitleListItem.add(m);
				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				// ProcessName
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.process_name));
				m1.put(Constants.ITEM_TEXT, detail.get(0));
				list.add(m1);
				// StepSeq
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.step_seq));
				m2.put(Constants.ITEM_TEXT, detail.get(1));
				list.add(m2);
				// StepName
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.step_name));
				m3.put(Constants.ITEM_TEXT, detail.get(2));
				list.add(m3);
				// CPKCode
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.cpk_code));
				m4.put(Constants.ITEM_TEXT, detail.get(3));
				list.add(m4);
				// CPKName
				HashMap<String, Object> m5 = new HashMap<String, Object>();
				m5.put(Constants.ITEM_TITLE, getString(R.string.cpk_name));
				m5.put(Constants.ITEM_TEXT, detail.get(4));
				list.add(m5);
				// CPKControlLimit
				HashMap<String, Object> m6 = new HashMap<String, Object>();
				m6.put(Constants.ITEM_TITLE, getString(R.string.cpk_control_limit));
				m6.put(Constants.ITEM_TEXT, detail.get(5));
				list.add(m6);
				// CPKValue
				HashMap<String, Object> m7 = new HashMap<String, Object>();
				m7.put(Constants.ITEM_TITLE, getString(R.string.cpk_value));
				m7.put(Constants.ITEM_TEXT, detail.get(6));
				list.add(m7);
				// CPK_KControlLimit
				HashMap<String, Object> m8 = new HashMap<String, Object>();
				m8.put(Constants.ITEM_TITLE, getString(R.string.cpk_k_control_limit));
				m8.put(Constants.ITEM_TEXT, detail.get(7));
				list.add(m8);
				// CPK_KValue
				HashMap<String, Object> m9 = new HashMap<String, Object>();
				m9.put(Constants.ITEM_TITLE, getString(R.string.cpk_k_value));
				m9.put(Constants.ITEM_TEXT, detail.get(8));
				list.add(m9);
				// EventTime
				HashMap<String, Object> m10 = new HashMap<String, Object>();
				m10.put(Constants.ITEM_TITLE, getString(R.string.event_time));
				m10.put(Constants.ITEM_TEXT, detail.get(9));
				list.add(m9);
				// UserID
				HashMap<String, Object> m11 = new HashMap<String, Object>();
				m11.put(Constants.ITEM_TITLE, getString(R.string.user_id));
				m11.put(Constants.ITEM_TEXT, detail.get(10));
				list.add(m11);

				cpkDetailListItem.add(list);
			}
			SimpleAdapter listItemAdapter = new SimpleAdapter(LotInquiryCPKDataActivity.this, cpkTitleListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			cpkTitleListView.setAdapter(listItemAdapter);
			cpkTitleListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String stepName = (String) item.get(Constants.ITEM_TITLE);
					ArrayList<HashMap<String, Object>> temp = cpkDetailListItem.get(position);
					SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotInquiryCPKDataActivity.this, temp, R.layout.lot_inquiry_list_item, new String[] {
							Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
					final ListView detailView = new ListView(LotInquiryCPKDataActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(detailListItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(LotInquiryCPKDataActivity.this);
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
