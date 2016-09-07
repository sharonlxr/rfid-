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

public class LotInquiryReasonHistActivity extends BaseActivity {

	private QueryTask qTask;
	private ListView reasonHistListView;
	private List<HashMap<String, Object>> reasonHistListItem = new ArrayList<HashMap<String, Object>>();
	private ListView mesAttrValueListView;
	private List<HashMap<String, Object>> mesAttrValueListItem = new ArrayList<HashMap<String, Object>>();
	private ListView mesAssignmentsHistListView;
	private List<HashMap<String, Object>> mesAssignmentsHistListItem = new ArrayList<HashMap<String, Object>>();
	private DataCollection reasonHist;
	private DataCollection mesAttrValueHist;
	private DataCollection mesAssignmentsHist;
	private List<ArrayList<HashMap<String, Object>>> reasonHistDetailListItem = new ArrayList<ArrayList<HashMap<String, Object>>>();
	private List<ArrayList<HashMap<String, Object>>> mesAttrValueDetailListItem = new ArrayList<ArrayList<HashMap<String, Object>>>();
	private List<ArrayList<HashMap<String, Object>>> mesAssignmentsHistDetailListItem = new ArrayList<ArrayList<HashMap<String, Object>>>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_inquiry_reason_hist);
		mFormView = findViewById(R.id.inquiry_reason_form);
		mStatusView = findViewById(R.id.inquiry_reason_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		reasonHistListView = (ListView) findViewById(R.id.inquiry_lot_reason_hist);
		mesAttrValueListView = (ListView) findViewById(R.id.inquiry_lot_mes_attr_value);
		mesAssignmentsHistListView = (ListView) findViewById(R.id.inquiry_lot_mes_assignments_hist);
		reasonHist = new DataCollection();
		mesAttrValueHist = new DataCollection();
		mesAssignmentsHist = new DataCollection();
		if (null != global.getAoLot() && null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("goToViewReasonHistory");
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
				if (cmdName.equals("goToViewReasonHistory")) {
					goToViewReasonHistory();
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
				if (cmdName.equals("goToViewReasonHistory")) {
					goToViewReasonHistoryAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotInquiryReasonHistActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotInquiryReasonHistActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void goToViewReasonHistory() throws BaseException {
			String API = "getLotReasonHistory(attributes='stepName,startTime,startUserId,reasonCode,category,currentFlag,recordSequence,reasonQty,sourceLots,targetLots,description,comment, MRBNumber',"
					+ "lotNumber='" + global.getAoLot().getAlotNumber() + "')";
			reasonHist = apiExecutorQuery.query("LotInquiryReasonHist", "goToViewReasonHistory", API);
			API = "getMESAttrValueHistory(attributes='attrName,attrValue,setTime,setUserId,reasonCode,comments'," + "entityId='" + global.getAoLot().getAlotNumber()
					+ "',entityType='ALOT')";
			mesAttrValueHist = apiExecutorQuery.query("LotInquiryReasonHist", "goToViewReasonHistory", API);
			API = "getMESAssignmentsHistory(attributes='assignmentOwnerType,assignmentOwner,setTime,setUserId,reasonCode,comments'," + "assignmentValue='"
					+ global.getAoLot().getAlotNumber() + "',assignmentType='ALOT')";
			mesAssignmentsHist = apiExecutorQuery.query("LotInquiryReasonHist", "goToViewReasonHistory", API);
		}

		private void goToViewReasonHistoryAfter() {
			for (int i = 0; i < reasonHist.size(); i++) {
				ArrayList<String> detail = reasonHist.get(i);
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, detail.get(0) + " " + detail.get(3));
				reasonHistListItem.add(m);

				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				// StepName
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.step_name));
				m1.put(Constants.ITEM_TEXT, detail.get(0));
				list.add(m1);
				// StartTime
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.start_time));
				m2.put(Constants.ITEM_TEXT, detail.get(1));
				list.add(m2);
				// UserID
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.user_id));
				m3.put(Constants.ITEM_TEXT, detail.get(2));
				list.add(m3);
				// ReasonCode
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.reason_code));
				m4.put(Constants.ITEM_TEXT, detail.get(3));
				list.add(m4);
				// Category
				HashMap<String, Object> m5 = new HashMap<String, Object>();
				m5.put(Constants.ITEM_TITLE, getString(R.string.category));
				m5.put(Constants.ITEM_TEXT, detail.get(4));
				list.add(m5);
				// CurrentFlag
				HashMap<String, Object> m6 = new HashMap<String, Object>();
				m6.put(Constants.ITEM_TITLE, getString(R.string.current_flag));
				m6.put(Constants.ITEM_TEXT, detail.get(5));
				list.add(m6);
				// RecordSeq
				HashMap<String, Object> m7 = new HashMap<String, Object>();
				m7.put(Constants.ITEM_TITLE, getString(R.string.record_seq));
				m7.put(Constants.ITEM_TEXT, detail.get(6));
				list.add(m7);
				// Quantity
				HashMap<String, Object> m8 = new HashMap<String, Object>();
				m8.put(Constants.ITEM_TITLE, getString(R.string.quantity));
				m8.put(Constants.ITEM_TEXT, detail.get(7));
				list.add(m8);
				// SourcesLots
				HashMap<String, Object> m9 = new HashMap<String, Object>();
				m9.put(Constants.ITEM_TITLE, getString(R.string.sources_lots));
				m9.put(Constants.ITEM_TEXT, detail.get(8));
				list.add(m9);
				// TargetLots
				HashMap<String, Object> m10 = new HashMap<String, Object>();
				m10.put(Constants.ITEM_TITLE, getString(R.string.target_lots));
				m10.put(Constants.ITEM_TEXT, detail.get(9));
				list.add(m10);
				// Description
				HashMap<String, Object> m11 = new HashMap<String, Object>();
				m11.put(Constants.ITEM_TITLE, getString(R.string.description));
				m11.put(Constants.ITEM_TEXT, detail.get(10));
				list.add(m11);
				// Comment
				HashMap<String, Object> m12 = new HashMap<String, Object>();
				m12.put(Constants.ITEM_TITLE, getString(R.string.comment));
				m12.put(Constants.ITEM_TEXT, detail.get(11));
				list.add(m12);
				// MRBNum
				HashMap<String, Object> m13 = new HashMap<String, Object>();
				m13.put(Constants.ITEM_TITLE, getString(R.string.mrb_num));
				m13.put(Constants.ITEM_TEXT, detail.get(12));
				list.add(m13);
				reasonHistDetailListItem.add(list);
			}
			SimpleAdapter listItemAdapter = new SimpleAdapter(LotInquiryReasonHistActivity.this, reasonHistListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			reasonHistListView.setAdapter(listItemAdapter);
			reasonHistListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String title = (String) item.get(Constants.ITEM_TITLE);
					ArrayList<HashMap<String, Object>> temp = reasonHistDetailListItem.get(position);
					SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotInquiryReasonHistActivity.this, temp, R.layout.lot_inquiry_list_item, new String[] {
							Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
					final ListView detailView = new ListView(LotInquiryReasonHistActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(detailListItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(LotInquiryReasonHistActivity.this);
					builder.setTitle(title).setIcon(android.R.drawable.ic_dialog_info).setView(detailView);
					builder.setPositiveButton(getResources().getString(R.string.close), null);
					builder.show();
				}
			});
			for (int i = 0; i < mesAttrValueHist.size(); i++) {
				ArrayList<String> detail = mesAttrValueHist.get(i);
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, detail.get(0) + " " + detail.get(1) + " " + detail.get(4));
				mesAttrValueListItem.add(m);

				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				// AttrName
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.attr_name));
				m1.put(Constants.ITEM_TEXT, detail.get(0));
				list.add(m1);
				// AttrValue
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.attr_value));
				m2.put(Constants.ITEM_TEXT, detail.get(1));
				list.add(m2);
				// SetTime
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.set_time));
				m3.put(Constants.ITEM_TEXT, detail.get(2));
				list.add(m3);
				// SetUserID
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.set_user_id));
				m4.put(Constants.ITEM_TEXT, detail.get(3));
				list.add(m4);
				// ReasonCode
				HashMap<String, Object> m5 = new HashMap<String, Object>();
				m5.put(Constants.ITEM_TITLE, getString(R.string.reason_code));
				m5.put(Constants.ITEM_TEXT, detail.get(4));
				list.add(m5);
				// Comment
				HashMap<String, Object> m6 = new HashMap<String, Object>();
				m6.put(Constants.ITEM_TITLE, getString(R.string.comment));
				m6.put(Constants.ITEM_TEXT, detail.get(5));
				list.add(m6);
				mesAttrValueDetailListItem.add(list);
			}
			SimpleAdapter listItemAdapter2 = new SimpleAdapter(LotInquiryReasonHistActivity.this, mesAttrValueListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			mesAttrValueListView.setAdapter(listItemAdapter2);
			mesAttrValueListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String title = (String) item.get(Constants.ITEM_TITLE);
					ArrayList<HashMap<String, Object>> temp = mesAttrValueDetailListItem.get(position);
					SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotInquiryReasonHistActivity.this, temp, R.layout.lot_inquiry_list_item, new String[] {
							Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
					final ListView detailView = new ListView(LotInquiryReasonHistActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(detailListItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(LotInquiryReasonHistActivity.this);
					builder.setTitle(title).setIcon(android.R.drawable.ic_dialog_info).setView(detailView);
					builder.setPositiveButton(getResources().getString(R.string.close), null);
					builder.show();
				}
			});
			for (int i = 0; i < mesAssignmentsHist.size(); i++) {
				ArrayList<String> detail = mesAssignmentsHist.get(i);
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, detail.get(0) + " " + detail.get(1) + " " + detail.get(4));
				mesAssignmentsHistListItem.add(m);
				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				// OwnerType
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.owner_type));
				m1.put(Constants.ITEM_TEXT, detail.get(0));
				list.add(m1);
				// Owner
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.owner));
				m2.put(Constants.ITEM_TEXT, detail.get(1));
				list.add(m2);
				// SetTime
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.set_time));
				m3.put(Constants.ITEM_TEXT, detail.get(2));
				list.add(m3);
				// SetUserID
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.set_user_id));
				m4.put(Constants.ITEM_TEXT, detail.get(3));
				list.add(m4);
				// ReasonCode
				HashMap<String, Object> m5 = new HashMap<String, Object>();
				m5.put(Constants.ITEM_TITLE, getString(R.string.reason_code));
				m5.put(Constants.ITEM_TEXT, detail.get(4));
				list.add(m5);
				// Comment
				HashMap<String, Object> m6 = new HashMap<String, Object>();
				m6.put(Constants.ITEM_TITLE, getString(R.string.comment));
				m6.put(Constants.ITEM_TEXT, detail.get(5));
				list.add(m6);
				mesAssignmentsHistDetailListItem.add(list);
			}
			SimpleAdapter listItemAdapter3 = new SimpleAdapter(LotInquiryReasonHistActivity.this, mesAssignmentsHistListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			mesAssignmentsHistListView.setAdapter(listItemAdapter3);
			mesAssignmentsHistListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String title = (String) item.get(Constants.ITEM_TITLE);
					ArrayList<HashMap<String, Object>> temp = mesAssignmentsHistDetailListItem.get(position);
					SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotInquiryReasonHistActivity.this, temp, R.layout.lot_inquiry_list_item, new String[] {
							Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
					final ListView detailView = new ListView(LotInquiryReasonHistActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(detailListItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(LotInquiryReasonHistActivity.this);
					builder.setTitle(title).setIcon(android.R.drawable.ic_dialog_info).setView(detailView);
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
