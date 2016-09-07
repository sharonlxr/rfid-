package com.fsl.cimei.rfid;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
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
import com.freescale.api.DateFormatter;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class LotInquiryMemoActivity extends BaseActivity {

	private QueryTask qTask = null;
	private ListView lotAttrListView;
	private List<HashMap<String, Object>> lotAttrListItem;
	private ListView memoTitleListView;
	private List<HashMap<String, Object>> memoTitleListItem;
	private ArrayList<ArrayList<HashMap<String, Object>>> memoDetailListItem = new ArrayList<ArrayList<HashMap<String, Object>>>();
	private DataCollection lotAttrData;
	private DataCollection dataEngineeringMemo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_inquiry_memo);
		mFormView = findViewById(R.id.inquiry_memo_form);
		mStatusView = findViewById(R.id.inquiry_memo_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		lotAttrListView = (ListView) findViewById(R.id.inquiry_lot_attr_list);
		lotAttrListItem = new ArrayList<HashMap<String, Object>>();
		memoTitleListView = (ListView) findViewById(R.id.inquiry_lot_info_list1);
		memoTitleListItem = new ArrayList<HashMap<String, Object>>();
		lotAttrData = new DataCollection();
		dataEngineeringMemo = new DataCollection();
		if (null == qTask && null != global.getAoLot()) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("goToViewMemo");
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
			String cmdName = params[0];
			try {
				if (cmdName.equals("goToViewMemo")) {
					goToViewMemo();
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
			for (ArrayList<String> detail : lotAttrData) {
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.lot_number));
				m1.put(Constants.ITEM_TEXT, detail.get(0));
				lotAttrListItem.add(m1);
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.device_number));
				m2.put(Constants.ITEM_TEXT, detail.get(1));
				lotAttrListItem.add(m2);
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.assembly_lot_number));
				m3.put(Constants.ITEM_TEXT, detail.get(2));
				lotAttrListItem.add(m3);
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.wafer_lot_number));
				m4.put(Constants.ITEM_TEXT, detail.get(3));
				lotAttrListItem.add(m4);
			}
			SimpleAdapter lotAttrListItemAdapter = new SimpleAdapter(LotInquiryMemoActivity.this, lotAttrListItem, R.layout.lot_inquiry_list_item, new String[] {
					Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
			lotAttrListView.setAdapter(lotAttrListItemAdapter);
			if (null == e) {
				if (cmdName.equals("goToViewMemo")) {
					goToViewMemoAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotInquiryMemoActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotInquiryMemoActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void goToViewMemo() throws BaseException {
			String API = "getLotAttributes(attributes='lotNumber,devcNumber,assyLotNumber,waferLotNumber'," + "lotNumber='" + global.getAoLot().getAlotNumber() + "')";
			lotAttrData = apiExecutorQuery.query("LotInquiryMemo", "goToViewMemo", API);
			API = "getUIAllMemos(lotNumber = '" + global.getAoLot().getAlotNumber() + "', stepName = '" + global.getAoLot().getCurrentStep().getStepName() + "')";
			dataEngineeringMemo = apiExecutorQuery.query("LotInquiryMemo", "goToViewMemo", API);
			String ackMemoMaxSetTime = "";
			if (dataEngineeringMemo.size() > 0) {
				for (int i = 0; i < dataEngineeringMemo.size(); i++) {
					if (!CommonUtility.isEmpty(dataEngineeringMemo.get(i).get(2))) {
						String memoIntr;
						try {
							memoIntr = URLDecoder.decode(dataEngineeringMemo.get(i).get(2).replace("\\x", "%"), "gbk");
						} catch (UnsupportedEncodingException e) {
							throw new RfidException(e.toString(), "LotInquiryMemo", "goToViewMemo", API);
						}
						dataEngineeringMemo.get(i).set(2, memoIntr);
						if (!CommonUtility.isEmpty(ackMemoMaxSetTime)) {
							try {
								Date d1 = DateFormatter.getSimpleDateToDate(ackMemoMaxSetTime);
								Date d2 = DateFormatter.getSimpleDateToDate(dataEngineeringMemo.get(i).get(3));
								ackMemoMaxSetTime = d1.compareTo(d2) > 0 ? ackMemoMaxSetTime : dataEngineeringMemo.get(i).get(3);
							} catch (ParseException e) {
								throw new RfidException(e.toString(), "LotInquiryMemo", "goToViewMemo", API);
							}
						} else {
							ackMemoMaxSetTime = dataEngineeringMemo.get(i).get(3);
						}
					}
					String setTime = dataEngineeringMemo.get(i).get(3);
					String instrBy = dataEngineeringMemo.get(i).get(5) + " " + dataEngineeringMemo.get(i).get(6) + "(" + dataEngineeringMemo.get(i).get(4) + ")";
					dataEngineeringMemo.get(i).set(3, instrBy);
					dataEngineeringMemo.get(i).set(4, setTime);
					for (int j = dataEngineeringMemo.get(i).size() - 1; j > 4; j--) {
						dataEngineeringMemo.get(i).remove(j);
					}
				}
			}
			if (CommonUtility.isEmpty(dataEngineeringMemo)) {
				throw new RfidException("没有memo信息", "LotInquiryMemo", "goToViewMemo", API);
			}
		}

		private void goToViewMemoAfter() {
			for (int i = 0; i < dataEngineeringMemo.size(); i++) {
				ArrayList<String> detail = dataEngineeringMemo.get(i);
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, detail.get(0) + " " + detail.get(1) + " " + detail.get(2));
				memoTitleListItem.add(m);

				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				// MemoType
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.memo_type));
				m1.put(Constants.ITEM_TEXT, detail.get(0));
				list.add(m1);
				// MemoSetFor
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.memo_set_for));
				m2.put(Constants.ITEM_TEXT, detail.get(1));
				list.add(m2);
				// MemoText
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.memo_text));
				m3.put(Constants.ITEM_TEXT, detail.get(2));
				list.add(m3);
				// InstructionBy
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.instruction_by));
				m4.put(Constants.ITEM_TEXT, detail.get(3));
				list.add(m4);
				// SetTime
				HashMap<String, Object> m5 = new HashMap<String, Object>();
				m5.put(Constants.ITEM_TITLE, getString(R.string.set_time));
				m5.put(Constants.ITEM_TEXT, detail.get(4));
				list.add(m5);

				memoDetailListItem.add(list);
			}
			SimpleAdapter listItemAdapter = new SimpleAdapter(LotInquiryMemoActivity.this, memoTitleListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			memoTitleListView.setAdapter(listItemAdapter);
			memoTitleListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String title = (String) item.get(Constants.ITEM_TITLE);
					ArrayList<HashMap<String, Object>> temp = memoDetailListItem.get(position);
					SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotInquiryMemoActivity.this, temp, R.layout.lot_inquiry_list_item, new String[] {
							Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
					final ListView detailView = new ListView(LotInquiryMemoActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(detailListItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(LotInquiryMemoActivity.this);
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
