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

public class LotInquiryMachInfoActivity extends BaseActivity {

	private QueryTask qTask = null;
	private ListView stepNameListView;
	private List<HashMap<String, Object>> machIDListItem;
	private HashMap<String, ArrayList<HashMap<String, Object>>> machDetailListItem = new HashMap<String, ArrayList<HashMap<String, Object>>>();
	private DataCollection machInfo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_inquiry);
		mFormView = findViewById(R.id.inquiry_form);
		mStatusView = findViewById(R.id.lot_inquiry_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
//		((TextView) findViewById(R.id.title_lot_information)).setText(R.string.title_activity_lot_inquiry_mach_info);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.lot_inquiry_tb_fragment);
		this.tagBarcodeInputFragment.getTagBarcodeInput().setVisibility(View.GONE);
		this.tagBarcodeInputFragment.getAlotTextView().setText(R.string.title_activity_lot_inquiry_mach_info);
		stepNameListView = (ListView) findViewById(R.id.inquiry_lot_info_list);
		machIDListItem = new ArrayList<HashMap<String, Object>>();
		machInfo = new DataCollection();
		if (null == qTask && null != global.getAoLot()) {
			qTask = new QueryTask();
			qTask.execute("getCurrentMachineContext");
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
				if (cmdName.equals("getCurrentMachineContext")) {
					getCurrentMachineContext();
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
				if (cmdName.equals("getCurrentMachineContext")) {
					getCurrentMachineContextAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotInquiryMachInfoActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotInquiryMachInfoActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void getCurrentMachineContext() throws BaseException {
			String API = "getCurrentMachineContext(attributes='machId,machName,machType,machModel,stepName,startTime,machQty,rejQty,startUserId',lotNumber='"
					+ global.getAoLot().getAlotNumber() + "')";
			machInfo = apiExecutorQuery.query("LotInquiryMachInfo", "getCurrentMachineContext", API);
			if (CommonUtility.isEmpty(machInfo)) {
				throw new RfidException("没有机台信息", "LotInquiryMachInfo", "getCurrentMachineContext", API);
			}
		}

		private void getCurrentMachineContextAfter() {
			for (ArrayList<String> mach : machInfo) {
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, mach.get(0));
				machIDListItem.add(m);
				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				// MachineName
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.mach_name));
				m1.put(Constants.ITEM_TEXT, mach.get(1));
				list.add(m1);
				// MachineType
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.mach_type));
				m2.put(Constants.ITEM_TEXT, mach.get(2));
				list.add(m2);
				// MachineModel
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.mach_model));
				m3.put(Constants.ITEM_TEXT, mach.get(3));
				list.add(m3);
				// StepName
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.step_name));
				m4.put(Constants.ITEM_TEXT, mach.get(4));
				list.add(m4);
				// StartTime
				HashMap<String, Object> m5 = new HashMap<String, Object>();
				m5.put(Constants.ITEM_TITLE, getString(R.string.start_time));
				m5.put(Constants.ITEM_TEXT, mach.get(5));
				list.add(m5);
				// OutputQty
				HashMap<String, Object> m6 = new HashMap<String, Object>();
				m6.put(Constants.ITEM_TITLE, getString(R.string.output_qty));
				m6.put(Constants.ITEM_TEXT, mach.get(6));
				list.add(m6);
				// RejQty
				HashMap<String, Object> m7 = new HashMap<String, Object>();
				m7.put(Constants.ITEM_TITLE, getString(R.string.rej_qty));
				m7.put(Constants.ITEM_TEXT, mach.get(7));
				list.add(m7);
				// StartUser
				HashMap<String, Object> m8 = new HashMap<String, Object>();
				m8.put(Constants.ITEM_TITLE, getString(R.string.start_user));
				m8.put(Constants.ITEM_TEXT, mach.get(8));
				list.add(m8);
				// MachineId
				HashMap<String, Object> m9 = new HashMap<String, Object>();
				m9.put(Constants.ITEM_TITLE, getString(R.string.mach_ID));
				m9.put(Constants.ITEM_TEXT, mach.get(0));
				list.add(m9);

				machDetailListItem.put(mach.get(0), list);
			}
			SimpleAdapter listItemAdapter = new SimpleAdapter(LotInquiryMachInfoActivity.this, machIDListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			stepNameListView.setAdapter(listItemAdapter);
			stepNameListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String stepName = (String) item.get(Constants.ITEM_TITLE);
					ArrayList<HashMap<String, Object>> temp = machDetailListItem.get(stepName);
					SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotInquiryMachInfoActivity.this, temp, R.layout.lot_inquiry_list_item, new String[] {
							Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
					final ListView detailView = new ListView(LotInquiryMachInfoActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(detailListItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(LotInquiryMachInfoActivity.this);
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
