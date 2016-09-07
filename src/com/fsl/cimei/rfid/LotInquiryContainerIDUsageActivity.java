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

public class LotInquiryContainerIDUsageActivity extends BaseActivity {

	private QueryTask qTask = null;
	private ListView containerIDUsageTitleListView;
	private List<HashMap<String, Object>> containerIDUsageTitleListItem;
	private List<ArrayList<HashMap<String, Object>>> containerIDUsageDetailListItem = new ArrayList<ArrayList<HashMap<String, Object>>>();
	private DataCollection containerIDUsage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_inquiry);
		mFormView = findViewById(R.id.inquiry_form);
		mStatusView = findViewById(R.id.lot_inquiry_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		//((TextView) findViewById(R.id.title_lot_information)).setText(R.string.title_activity_lot_inquiry_container_id_usage);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.lot_inquiry_tb_fragment);
		this.tagBarcodeInputFragment.getTagBarcodeInput().setVisibility(View.GONE);
		this.tagBarcodeInputFragment.getAlotTextView().setText(R.string.title_activity_lot_inquiry_container_id_usage);
		containerIDUsageTitleListView = (ListView) findViewById(R.id.inquiry_lot_info_list);
		containerIDUsageTitleListItem = new ArrayList<HashMap<String, Object>>();
		containerIDUsage = new DataCollection();
		if (null == qTask && null != global.getAoLot()) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("goToViewContainerIDUsage");
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
				if (cmdName.equals("goToViewContainerIDUsage")) {
					goToViewContainerIDUsage();
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
				if (cmdName.equals("goToViewContainerIDUsage")) {
					goToViewContainerIDUsageAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotInquiryContainerIDUsageActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotInquiryContainerIDUsageActivity.this, e.getErrorMsg());
				}
			}
			
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void goToViewContainerIDUsage() throws BaseException {
			String API = "getPiecepartPerAOLot(attributes='ppLotNumber, containerId, procName, stepSeq, stepName, machId,qty, eventTime,checkoutTime'," + "lotNumber='"
					+ global.getAoLot().getAlotNumber() + "')";
			containerIDUsage = apiExecutorQuery.query("LotInquiryContainerIDUsage", "goToViewContainerIDUsage", API);
			if (CommonUtility.isEmpty(containerIDUsage)) {
				throw new RfidException("No container ID usage information for this lot.", "LotInquiryContainerIDUsage", "goToViewContainerIDUsage", API);
			}
		}

		private void goToViewContainerIDUsageAfter() {
			for (int i = 0; i < containerIDUsage.size(); i++) {
				ArrayList<String> detail = containerIDUsage.get(i);
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, detail.get(0) + " " + detail.get(4));
				containerIDUsageTitleListItem.add(m);
				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				// PPLotNum
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.pp_lot_number));
				m1.put(Constants.ITEM_TEXT, detail.get(0));
				list.add(m1);
				// ContainerID
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.container_id));
				m2.put(Constants.ITEM_TEXT, detail.get(1));
				list.add(m2);
				// ProcessName
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.process_name));
				m3.put(Constants.ITEM_TEXT, detail.get(2));
				list.add(m3);
				// StepSeq
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.step_seq));
				m4.put(Constants.ITEM_TEXT, detail.get(3));
				list.add(m4);
				// StepName
				HashMap<String, Object> m5 = new HashMap<String, Object>();
				m5.put(Constants.ITEM_TITLE, getString(R.string.step_name));
				m5.put(Constants.ITEM_TEXT, detail.get(4));
				list.add(m5);
				// MachineId
				HashMap<String, Object> m6 = new HashMap<String, Object>();
				m6.put(Constants.ITEM_TITLE, getString(R.string.mach_ID));
				m6.put(Constants.ITEM_TEXT, detail.get(5));
				list.add(m6);
				// Quantity
				HashMap<String, Object> m7 = new HashMap<String, Object>();
				m7.put(Constants.ITEM_TITLE, getString(R.string.quantity));
				m7.put(Constants.ITEM_TEXT, detail.get(6));
				list.add(m7);
				// EventTime
				HashMap<String, Object> m8 = new HashMap<String, Object>();
				m8.put(Constants.ITEM_TITLE, getString(R.string.event_time));
				m8.put(Constants.ITEM_TEXT, detail.get(7));
				list.add(m8);
				// CheckOutTime
				HashMap<String, Object> m9 = new HashMap<String, Object>();
				m9.put(Constants.ITEM_TITLE, getString(R.string.check_out_time));
				m9.put(Constants.ITEM_TEXT, detail.get(8));
				list.add(m9);

				containerIDUsageDetailListItem.add(list);
			}
			SimpleAdapter listItemAdapter = new SimpleAdapter(LotInquiryContainerIDUsageActivity.this, containerIDUsageTitleListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			containerIDUsageTitleListView.setAdapter(listItemAdapter);
			containerIDUsageTitleListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String stepName = (String) item.get(Constants.ITEM_TITLE);
					ArrayList<HashMap<String, Object>> temp = containerIDUsageDetailListItem.get(position);
					SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotInquiryContainerIDUsageActivity.this, temp, R.layout.lot_inquiry_list_item, new String[] {
							Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
					final ListView detailView = new ListView(LotInquiryContainerIDUsageActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(detailListItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(LotInquiryContainerIDUsageActivity.this);
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
