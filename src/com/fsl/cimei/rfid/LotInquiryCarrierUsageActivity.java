package com.fsl.cimei.rfid;

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

public class LotInquiryCarrierUsageActivity extends BaseActivity {

	private QueryTask qTask = null;
	private ListView carrierUsageTitleListView;
	private List<HashMap<String, Object>> carrierUsageTitleListItem;
	private List<ArrayList<HashMap<String, Object>>> carrierUsageDetailListItem = new ArrayList<ArrayList<HashMap<String, Object>>>();
	private DataCollection carrierUsageData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_inquiry);
		mFormView = findViewById(R.id.inquiry_form);
		mStatusView = findViewById(R.id.lot_inquiry_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
//		((TextView) findViewById(R.id.title_lot_information)).setText(R.string.title_activity_lot_inquiry_carrier_usage);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.lot_inquiry_tb_fragment);
		this.tagBarcodeInputFragment.getTagBarcodeInput().setVisibility(View.GONE);
		this.tagBarcodeInputFragment.getAlotTextView().setText(R.string.title_activity_lot_inquiry_carrier_usage);
		carrierUsageTitleListView = (ListView) findViewById(R.id.inquiry_lot_info_list);
		carrierUsageTitleListItem = new ArrayList<HashMap<String, Object>>();
		carrierUsageData = new DataCollection();
		if (null == qTask && null != global.getAoLot()) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("goToViewCarrierUsage");
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
				if (cmdName.equals("goToViewCarrierUsage")) {
					goToViewCarrierUsage();
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
				if (cmdName.equals("goToViewCarrierUsage")) {
					goToViewCarrierUsageAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotInquiryCarrierUsageActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotInquiryCarrierUsageActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void goToViewCarrierUsage() throws BaseException {
			String API = "getAlotCarrierHists(attributes='stepName,carrierId,transType,transUserId,transTime'," + "lotNumber='" + global.getAoLot().getAlotNumber()
					+ "')";
			DataCollection dataContainer = apiExecutorQuery.query("LotInquiryCarrierUsage", "goToViewCarrierUsage", API);
			if (!CommonUtility.isEmpty(dataContainer)) {
				String inCarrierId = "", outCarrierId = "", inProcCarrierId = "";
				String inUserId = "", outUserId = "", inProcUserId = "";
				String inTransTime = "", outTransTime = "", inProcTransTime = "";
				String prevStepName = "";
				for (int i = 0; i < dataContainer.size(); i++) {
					if (prevStepName.equals("")) { // the first step
						String transType = dataContainer.get(i).get(2);
						prevStepName = dataContainer.get(i).get(0);
						if (transType.equals("START")) {
							inCarrierId = dataContainer.get(i).get(1);
							inUserId = dataContainer.get(i).get(3);
							inTransTime = dataContainer.get(i).get(4);
						} else if (transType.equals("END")) {
							outCarrierId = dataContainer.get(i).get(1);
							outUserId = dataContainer.get(i).get(3);
							outTransTime = dataContainer.get(i).get(4);
						} else {
							inProcCarrierId = dataContainer.get(i).get(1);
							inProcUserId = dataContainer.get(i).get(3);
							inProcTransTime = dataContainer.get(i).get(4);
						}
						if (dataContainer.size() == 1) { // just 1 row.
							ArrayList<String> row = new ArrayList<String>();
							row.add(prevStepName);
							row.add(inCarrierId);
							row.add(inProcCarrierId);
							row.add(outCarrierId);
							row.add(inUserId);
							row.add(getUserNameList(inUserId));
							row.add(inTransTime);
							row.add(inProcUserId);
							row.add(getUserNameList(inProcUserId));
							row.add(inProcTransTime);
							row.add(outUserId);
							row.add(getUserNameList(outUserId));
							row.add(outTransTime);
							carrierUsageData.add(row);
						}
					} else if (prevStepName.equals(dataContainer.get(i).get(0))) { // the same step with prev step.
						String transType = dataContainer.get(i).get(2);
						if (transType.equals("START")) {
							if (!inCarrierId.contains(dataContainer.get(i).get(1))) {
								inCarrierId = inCarrierId.equals("") ? dataContainer.get(i).get(1) : inCarrierId + "," + dataContainer.get(i).get(1);
							}
							if (!inUserId.contains(dataContainer.get(i).get(3))) {
								inUserId = inUserId.equals("") ? dataContainer.get(i).get(3) : inUserId + "," + dataContainer.get(i).get(3);
							}
							if (!inTransTime.equals("")) {
								// get max time value.
								try {
									Date d1 = DateFormatter.getSimpleDateToDate(inTransTime);
									Date d2 = DateFormatter.getSimpleDateToDate(dataContainer.get(i).get(4));
									inTransTime = d1.compareTo(d2) > 0 ? inTransTime : dataContainer.get(i).get(4);
								} catch (ParseException e) {
									throw new RfidException(e.toString(), "LotInquiryCarrierUsage", "goToViewCarrierUsage", API);
								}
							} else {
								inTransTime = dataContainer.get(i).get(4);
							}

						} else if (transType.equals("END")) {
							if (!outCarrierId.contains(dataContainer.get(i).get(1))) {
								outCarrierId = outCarrierId.equals("") ? dataContainer.get(i).get(1) : outCarrierId + "," + dataContainer.get(i).get(1);
							}
							if (!outUserId.contains(dataContainer.get(i).get(3))) {
								outUserId = outUserId.equals("") ? dataContainer.get(i).get(3) : outUserId + "," + dataContainer.get(i).get(3);
							}
							if (!outTransTime.equals("")) {
								// get max time value.
								try {
									Date d1 = DateFormatter.getSimpleDateToDate(outTransTime);
									Date d2 = DateFormatter.getSimpleDateToDate(dataContainer.get(i).get(4));
									outTransTime = d1.compareTo(d2) > 0 ? outTransTime : dataContainer.get(i).get(4);
								} catch (ParseException e) {
									throw new RfidException(e.toString(), "LotInquiryCarrierUsage", "goToViewCarrierUsage", API);
								}
							} else {
								outTransTime = dataContainer.get(i).get(4);
							}

						} else {
							if (inProcCarrierId.contains(dataContainer.get(i).get(1))) {
								inProcCarrierId = inProcCarrierId.equals("") ? dataContainer.get(i).get(1) : inProcCarrierId + "," + dataContainer.get(i).get(1);
							}
							if (!inProcUserId.contains(dataContainer.get(i).get(3))) {
								inProcUserId = inProcUserId.equals("") ? dataContainer.get(i).get(3) : inProcUserId + "," + dataContainer.get(i).get(3);
							}
							if (!inProcTransTime.equals("")) {
								// get max time value.
								try {
									Date d1 = DateFormatter.getSimpleDateToDate(inProcTransTime);
									Date d2 = DateFormatter.getSimpleDateToDate(dataContainer.get(i).get(4));
									inProcTransTime = d1.compareTo(d2) > 0 ? inProcTransTime : dataContainer.get(i).get(4);
								} catch (ParseException e) {
									throw new RfidException(e.toString(), "LotInquiryCarrierUsage", "goToViewCarrierUsage", API);
								}
							} else {
								inProcTransTime = dataContainer.get(i).get(4);
							}

						}
						if (i == dataContainer.size() - 1) {
							// the last step.
							ArrayList<String> row = new ArrayList<String>();
							row.add(prevStepName);
							row.add(inCarrierId);
							row.add(inProcCarrierId);
							row.add(outCarrierId);
							row.add(inUserId);
							row.add(getUserNameList(inUserId));
							row.add(inTransTime);
							row.add(inProcUserId);
							row.add(getUserNameList(inProcUserId));
							row.add(inProcTransTime);
							row.add(outUserId);
							row.add(getUserNameList(outUserId));
							row.add(outTransTime);
							carrierUsageData.add(row);
						}
					} else { // different step.
						ArrayList<String> row = new ArrayList<String>();
						row.add(prevStepName);
						row.add(inCarrierId);
						row.add(inProcCarrierId);
						row.add(outCarrierId);
						row.add(inUserId);
						row.add(getUserNameList(inUserId));
						row.add(inTransTime);
						row.add(inProcUserId);
						row.add(getUserNameList(inProcUserId));
						row.add(inProcTransTime);
						row.add(outUserId);
						row.add(getUserNameList(outUserId));
						row.add(outTransTime);
						carrierUsageData.add(row);
						inCarrierId = "";
						outCarrierId = "";
						inProcCarrierId = "";
						inUserId = "";
						outUserId = "";
						inProcUserId = "";
						inTransTime = "";
						outTransTime = "";
						inProcTransTime = "";
						prevStepName = dataContainer.get(i).get(0);

						String transType = dataContainer.get(i).get(2);
						if (transType.equals("START")) {
							if (!inCarrierId.contains(dataContainer.get(i).get(1))) {
								inCarrierId = inCarrierId.equals("") ? dataContainer.get(i).get(1) : inCarrierId + "," + dataContainer.get(i).get(1);
							}
							if (!inUserId.contains(dataContainer.get(i).get(3))) {
								inUserId = inUserId.equals("") ? dataContainer.get(i).get(3) : inUserId + "," + dataContainer.get(i).get(3);
							}
							if (!inTransTime.equals("")) {
								// get max time value.
								try {
									Date d1 = DateFormatter.getSimpleDateToDate(inTransTime);
									Date d2 = DateFormatter.getSimpleDateToDate(dataContainer.get(i).get(4));
									inTransTime = d1.compareTo(d2) > 0 ? inTransTime : dataContainer.get(i).get(4);
								} catch (ParseException e) {
									throw new RfidException(e.toString(), "LotInquiryCarrierUsage", "goToViewCarrierUsage", API);
								}
							} else {
								inTransTime = dataContainer.get(i).get(4);
							}

						} else if (transType.equals("END")) {
							if (!outCarrierId.contains(dataContainer.get(i).get(1))) {
								outCarrierId = outCarrierId.equals("") ? dataContainer.get(i).get(1) : outCarrierId + "," + dataContainer.get(i).get(1);
							}
							if (!outUserId.contains(dataContainer.get(i).get(3))) {
								outUserId = outUserId.equals("") ? dataContainer.get(i).get(3) : outUserId + "," + dataContainer.get(i).get(3);
							}
							if (!outTransTime.equals("")) {
								// get max time value.
								try {
									Date d1 = DateFormatter.getSimpleDateToDate(outTransTime);
									Date d2 = DateFormatter.getSimpleDateToDate(dataContainer.get(i).get(4));
									outTransTime = d1.compareTo(d2) > 0 ? outTransTime : dataContainer.get(i).get(4);
								} catch (ParseException e) {
									throw new RfidException(e.toString(), "LotInquiryCarrierUsage", "goToViewCarrierUsage", API);
								}
							} else {
								outTransTime = dataContainer.get(i).get(4);
							}
						} else {
							if (!inProcCarrierId.contains(dataContainer.get(i).get(1))) {
								inProcCarrierId = inProcCarrierId.equals("") ? dataContainer.get(i).get(1) : inProcCarrierId + "," + dataContainer.get(i).get(1);
							}
							if (!inProcUserId.contains(dataContainer.get(i).get(3))) {
								inProcUserId = inProcUserId.equals("") ? dataContainer.get(i).get(3) : inProcUserId + "," + dataContainer.get(i).get(3);
							}
							if (!inProcTransTime.equals("")) {
								// get max time value.
								try {
									Date d1 = DateFormatter.getSimpleDateToDate(inProcTransTime);
									Date d2 = DateFormatter.getSimpleDateToDate(dataContainer.get(i).get(4));
									inProcTransTime = d1.compareTo(d2) > 0 ? inProcTransTime : dataContainer.get(i).get(4);
								} catch (ParseException e) {
									throw new RfidException(e.toString(), "LotInquiryCarrierUsage", "goToViewCarrierUsage", API);
								}
							} else {
								inProcTransTime = dataContainer.get(i).get(4);
							}
						}
					}
				}
			} else {
				throw new RfidException("没有carrier使用信息", "LotInquiryCarrierUsage", "goToViewCarrierUsage", API);
			}
		}

		private void goToViewCarrierUsageAfter() {
			for (int i = 0; i < carrierUsageData.size(); i++) {
				ArrayList<String> detail = carrierUsageData.get(i);
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, detail.get(0) + " " + detail.get(1) + " " + detail.get(3));
				carrierUsageTitleListItem.add(m);
				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				// StepName
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.step_name));
				m1.put(Constants.ITEM_TEXT, detail.get(0));
				list.add(m1);
				// carrierIdIn
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.carrier_id_in));
				m2.put(Constants.ITEM_TEXT, detail.get(1));
				list.add(m2);
				// carrierIdInproc
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.carrier_id_inproc));
				m3.put(Constants.ITEM_TEXT, detail.get(2));
				list.add(m3);
				// carrierIdOut
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.carrier_id_out));
				m4.put(Constants.ITEM_TEXT, detail.get(3));
				list.add(m4);
				// userIdIn
				HashMap<String, Object> m5 = new HashMap<String, Object>();
				m5.put(Constants.ITEM_TITLE, getString(R.string.user_id_in));
				m5.put(Constants.ITEM_TEXT, detail.get(4));
				list.add(m5);
				// userNameIn
				HashMap<String, Object> m6 = new HashMap<String, Object>();
				m6.put(Constants.ITEM_TITLE, getString(R.string.user_name_in));
				m6.put(Constants.ITEM_TEXT, detail.get(5));
				list.add(m6);
				// transTimeIn
				HashMap<String, Object> m7 = new HashMap<String, Object>();
				m7.put(Constants.ITEM_TITLE, getString(R.string.trans_time_in));
				m7.put(Constants.ITEM_TEXT, detail.get(6));
				list.add(m7);
				// userIdInproc
				HashMap<String, Object> m8 = new HashMap<String, Object>();
				m8.put(Constants.ITEM_TITLE, getString(R.string.user_id_inproc));
				m8.put(Constants.ITEM_TEXT, detail.get(7));
				list.add(m8);
				// userNameInproc
				HashMap<String, Object> m9 = new HashMap<String, Object>();
				m9.put(Constants.ITEM_TITLE, getString(R.string.user_name_inproc));
				m9.put(Constants.ITEM_TEXT, detail.get(8));
				list.add(m9);
				// transTimeInproc
				HashMap<String, Object> m10 = new HashMap<String, Object>();
				m10.put(Constants.ITEM_TITLE, getString(R.string.trans_time_inproc));
				m10.put(Constants.ITEM_TEXT, detail.get(9));
				list.add(m9);
				// userIdOut
				HashMap<String, Object> m11 = new HashMap<String, Object>();
				m11.put(Constants.ITEM_TITLE, getString(R.string.user_id_out));
				m11.put(Constants.ITEM_TEXT, detail.get(10));
				list.add(m11);
				// userNameOut
				HashMap<String, Object> m12 = new HashMap<String, Object>();
				m12.put(Constants.ITEM_TITLE, getString(R.string.user_name_out));
				m12.put(Constants.ITEM_TEXT, detail.get(11));
				list.add(m12);
				// transTimeOut
				HashMap<String, Object> m13 = new HashMap<String, Object>();
				m13.put(Constants.ITEM_TITLE, getString(R.string.trans_time_out));
				m13.put(Constants.ITEM_TEXT, detail.get(12));
				list.add(m13);

				carrierUsageDetailListItem.add(list);
			}
			SimpleAdapter listItemAdapter = new SimpleAdapter(LotInquiryCarrierUsageActivity.this, carrierUsageTitleListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			carrierUsageTitleListView.setAdapter(listItemAdapter);
			carrierUsageTitleListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String stepName = (String) item.get(Constants.ITEM_TITLE);
					ArrayList<HashMap<String, Object>> temp = carrierUsageDetailListItem.get(position);
					SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotInquiryCarrierUsageActivity.this, temp, R.layout.lot_inquiry_list_item, new String[] {
							Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
					final ListView detailView = new ListView(LotInquiryCarrierUsageActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(detailListItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(LotInquiryCarrierUsageActivity.this);
					builder.setTitle(stepName).setIcon(android.R.drawable.ic_dialog_info).setView(detailView);
					builder.setPositiveButton(getResources().getString(R.string.close), null);
					builder.show();
				}
			});
		}

		private String getUserNameList(String userIdList) throws BaseException {
			String userNameList = "";
			String userIdList_SQL = "";
			if (!CommonUtility.isEmpty(userIdList)) {
				String[] UserIds = userIdList.split(",");
				for (String id : UserIds) {
					if (!CommonUtility.isEmpty(id) && (!userIdList_SQL.contains(id))) {
						userIdList_SQL = userIdList_SQL.equals("") ? "'" + id + "'" : userIdList_SQL + ",'" + id + "'";
					}
				}
				String API = "execSql(\\\"select user_first_name ||' '|| user_last_name, user_id from users where user_id in (" + userIdList_SQL + ")\\\")";
				DataCollection outUserNameDC = apiExecutorQuery.query("LotInquiryCarrierUsage", "getUserNameList", API);
				for (int i = 0; i < outUserNameDC.size(); i++) {
					userNameList = userNameList.equals("") ? outUserNameDC.get(i).get(0) : userNameList + "," + outUserNameDC.get(i).get(0);
				}
			}
			return userNameList;
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
