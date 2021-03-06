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

public class LotInquiryDieReliefActivity extends BaseActivity {

	private QueryTask qTask = null;
	private ListView dieDeviceListView;
	private List<HashMap<String, Object>> dieDeviceListItem;
	private HashMap<String, ArrayList<HashMap<String, Object>>> dieReliefDetailListItem = new HashMap<String, ArrayList<HashMap<String, Object>>>();
	private DataCollection dieRelief;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_inquiry);
		mFormView = findViewById(R.id.inquiry_form);
		mStatusView = findViewById(R.id.lot_inquiry_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
//		((TextView) findViewById(R.id.title_lot_information)).setText(R.string.title_activity_lot_inquiry_die_relief);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.lot_inquiry_tb_fragment);
		this.tagBarcodeInputFragment.getTagBarcodeInput().setVisibility(View.GONE);
		this.tagBarcodeInputFragment.getAlotTextView().setText(R.string.title_activity_lot_inquiry_die_relief);
		dieDeviceListView = (ListView) findViewById(R.id.inquiry_lot_info_list);
		dieDeviceListItem = new ArrayList<HashMap<String, Object>>();
		dieRelief = new DataCollection();
		if (null == qTask && null != global.getAoLot()) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("goToDieReliefInfo");
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
				if (cmdName.equals("goToDieReliefInfo")) {
					goToDieReliefInfo();
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
				if (cmdName.equals("goToDieReliefInfo")) {
					goToDieReliefInfoAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotInquiryDieReliefActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotInquiryDieReliefActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void goToDieReliefInfo() throws BaseException {
			String API = "getCompUsedForLot(attributes='componentItem,componentQty,matlOwner,custSrc,fromBank,compLotNumber',"
					+ "lotType = 'ALOT', specColItem = '1',lotNumber='" + global.getAoLot().getAlotNumber() + "')";
			dieRelief = apiExecutorQuery.query("LotInquiryDieRelief", "goToDieReliefInfo", API);
			if (CommonUtility.isEmpty(dieRelief)) {
				throw new RfidException("No die relief info.", "LotInquiryDieRelief", "goToDieReliefInfo", API);
			}
		}

		private void goToDieReliefInfoAfter() {
			for (ArrayList<String> detail : dieRelief) {
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, detail.get(0));
				dieDeviceListItem.add(m);
				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				// DieDevice
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.die_device));
				m1.put(Constants.ITEM_TEXT, detail.get(0));
				list.add(m1);
				// LotQty
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.lot_qty));
				m2.put(Constants.ITEM_TEXT, detail.get(1));
				list.add(m2);
				// MatlOwner
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.matl_owner));
				m3.put(Constants.ITEM_TEXT, detail.get(2));
				list.add(m3);
				// CustSrc
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.cust_src));
				m4.put(Constants.ITEM_TEXT, detail.get(3));
				list.add(m4);
				// Bank
				HashMap<String, Object> m5 = new HashMap<String, Object>();
				m5.put(Constants.ITEM_TITLE, getString(R.string.bank));
				m5.put(Constants.ITEM_TEXT, detail.get(4));
				list.add(m5);
				// DieLot
				HashMap<String, Object> m6 = new HashMap<String, Object>();
				m6.put(Constants.ITEM_TITLE, getString(R.string.die_lot));
				m6.put(Constants.ITEM_TEXT, detail.get(5));
				list.add(m6);

				dieReliefDetailListItem.put(detail.get(0), list);
			}
			SimpleAdapter listItemAdapter = new SimpleAdapter(LotInquiryDieReliefActivity.this, dieDeviceListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			dieDeviceListView.setAdapter(listItemAdapter);
			dieDeviceListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String stepName = (String) item.get(Constants.ITEM_TITLE);
					ArrayList<HashMap<String, Object>> temp = dieReliefDetailListItem.get(stepName);
					SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotInquiryDieReliefActivity.this, temp, R.layout.lot_inquiry_list_item, new String[] {
							Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
					final ListView detailView = new ListView(LotInquiryDieReliefActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(detailListItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(LotInquiryDieReliefActivity.this);
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
