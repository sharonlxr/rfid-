package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class EmtLogActivity extends BaseActivity {

	private QueryTask qTask;
	private ListView list;
	private Button doneButton;
	private DataCollection emtLogDC;
	private List<HashMap<String, Object>> emtLogListItem;
	private String carrierId = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_emt_log);
		mFormView = findViewById(R.id.emt_log_form);
		mStatusView = findViewById(R.id.emt_log_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		showProgress(false);
		list = (ListView) findViewById(R.id.emt_log_list);
		doneButton = (Button) findViewById(R.id.emt_log_done);

		emtLogDC = new DataCollection();
		emtLogListItem = new ArrayList<HashMap<String, Object>>();

		doneButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				global.setCarrierID("");
				global.setScanTarget("");
				finish();
			}
		});
		// global.setCarrierID("8A10A613");
		// global.setCarrierID("56F4A34F");
		// if (null == qTask && !CommonUtility.isEmpty(global.getCarrierID())) {
		// mStatusMessageView.setText(getResources().getString(R.string.loading_data));
		// showProgress(true);
		// qTask = new QueryTask();
		// qTask.execute("checkTag");
		// }
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {

		String cmdName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("checkTag")) {
					checkTag();
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
				if (cmdName.equals("checkTag")) {
					checkTagAfter();
				}
			} else {
				emtLogListItem = new ArrayList<HashMap<String, Object>>();
				HashMap<String, Object> m8 = new HashMap<String, Object>();
				m8.put(Constants.ITEM_TITLE, "Tag ID");
				m8.put(Constants.ITEM_TEXT, carrierId);
				emtLogListItem.add(m8);
				HashMap<String, Object> m9 = new HashMap<String, Object>();
				m9.put(Constants.ITEM_TITLE, "Error");
				m9.put(Constants.ITEM_TEXT, e.getMessage());
				emtLogListItem.add(m9);
				SimpleAdapter listItemAdapter = new SimpleAdapter(EmtLogActivity.this, emtLogListItem, R.layout.lot_inquiry_list_item, new String[] {
						Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
				list.setAdapter(listItemAdapter);
				// showError(errorMsg);
			}
		}

		private void checkTag() throws BaseException {
			String API = "execSql('select a.*,c.carrier_name from genesis.carriers c,"
					+ "(select ROWNUM AS rowno,log2.MID,log2.TS_STATE_START,log2.SEQ_NUM,log2.EVENT_ID,log2.TS_CNTX_START "
					+ "from genesis.EMT_LOG log2, genesis.EMT_LOG_ATTRIBUTES att2 " + "where log2.MID=att2.MID and log2.SEQ_NUM=att2.SEQ_NUM and att2.VALUE=\\\\'"
					+ carrierId + "\\\\' and log2.EVENT_ID=\\\\'LOAD\\\\' "
					+ "and log2.TS_STATE_START>(to_char(sysdate-1, \\\\'yyyy-mm-dd\\\\') ||\\\\' 12:00:00\\\\') order by log2.TS_STATE_START desc) a "
					+ "where a.rowno=1 and c.CARRIER_ID=\\\\'" + carrierId + "\\\\'')";

			// String API = "execSql('select log1.MID,log1.TS_STATE_START,log1.SEQ_NUM,log1.EVENT_ID,log1.TS_CNTX_START,c.CARRIER_NAME "
			// + "from genesis.EMT_LOG log1, genesis.EMT_LOG_ATTRIBUTES att1, genesis.carriers c, "
			// + "(select log3.MID as max_MID,max_seq_num from genesis.EMT_LOG log3, genesis.EMT_LOG_ATTRIBUTES att3,  "
			// + "(select max(log2.SEQ_NUM) as max_seq_num from genesis.EMT_LOG log2, genesis.EMT_LOG_ATTRIBUTES att2  "
			// + "where log2.MID=att2.MID and log2.SEQ_NUM=att2.SEQ_NUM and att2.VALUE=\\\\'"
			// + global.getCarrierID()
			// + "\\\\' and log2.EVENT_ID=\\\\'LOAD\\\\' and log2.TS_STATE_START>(to_char(sysdate-1, \\\\'yyyy-mm-dd\\\\') ||\\\\' 12:00:00\\\\') order by log2.SEQ_NUM desc)  "
			// + "where log3.MID=att3.MID and log3.SEQ_NUM=att3.SEQ_NUM and att3.VALUE=\\\\'"
			// + global.getCarrierID()
			// + "\\\\' and log3.EVENT_ID=\\\\'LOAD\\\\' and log3.SEQ_NUM=max_seq_num)  "
			// + "where log1.TS_STATE_START>(to_char(sysdate-1, \\\\'yyyy-mm-dd\\\\') ||\\\\' 12:00:00\\\\') and log1.SEQ_NUM=max_seq_num and log1.MID=att1.MID and log1.SEQ_NUM=att1.SEQ_NUM "
			// + "and att1.VALUE<>\\\\'" + global.getCarrierID() + "\\\\' and att1.MID=max_MID and c.CARRIER_ID=\\\\'" + global.getCarrierID() + "\\\\'')";

			// String API = "execSql('select log1.MID,log1.TS_STATE_START,log1.SEQ_NUM,log1.EVENT_ID,log1.TS_CNTX_START,att1.VALUE,c.CARRIER_NAME "
			// + "from genesis.EMT_LOG log1, genesis.EMT_LOG_ATTRIBUTES att1, genesis.carriers c, "
			// + "(select max(log2.SEQ_NUM) as max_seq_num from genesis.EMT_LOG log2, genesis.EMT_LOG_ATTRIBUTES att2 "
			// + "where log2.MID=att2.MID and log2.SEQ_NUM=att2.SEQ_NUM and att2.VALUE=\\\\'" + global.getCarrierID() +
			// "\\\\' and log2.EVENT_ID=\\\\'LOAD\\\\' and log2.TS_STATE_START>(to_char(sysdate, \\\\'yyyy-mm-dd\\\\') ||\\\\' 08:00:00\\\\') order by log2.SEQ_NUM desc) "
			// + "where log1.TS_STATE_START>(to_char(sysdate, \\\\'yyyy-mm-dd\\\\') ||\\\\' 08:00:00\\\\') and log1.SEQ_NUM=max_seq_num and log1.MID=att1.MID and log1.SEQ_NUM=att1.SEQ_NUM "
			// + "and att1.VALUE<>\\\\'" + global.getCarrierID() + "\\\\' and c.CARRIER_ID=\\\\'" + global.getCarrierID() + "\\\\'')";

			global.setCarrierID("");
			emtLogDC = apiExecutorQuery.query("EmtLog", "checkTag", API);
			if (CommonUtility.isEmpty(emtLogDC)) {
				throw new RfidException("Tag: " + carrierId + " : 查询结果数为0", "EmtLog", "checkTag", API);
			}
		}

		private void checkTagAfter() {
			emtLogListItem = new ArrayList<HashMap<String, Object>>();
			HashMap<String, Object> m1 = new HashMap<String, Object>();
			m1.put(Constants.ITEM_TITLE, "MID");
			m1.put(Constants.ITEM_TEXT, emtLogDC.get(0).get(1));
			emtLogListItem.add(m1);
			HashMap<String, Object> m2 = new HashMap<String, Object>();
			m2.put(Constants.ITEM_TITLE, "Ts State Start");
			m2.put(Constants.ITEM_TEXT, emtLogDC.get(0).get(2));
			emtLogListItem.add(m2);
			HashMap<String, Object> m3 = new HashMap<String, Object>();
			m3.put(Constants.ITEM_TITLE, "Seq Num");
			m3.put(Constants.ITEM_TEXT, emtLogDC.get(0).get(3));
			emtLogListItem.add(m3);
			HashMap<String, Object> m4 = new HashMap<String, Object>();
			m4.put(Constants.ITEM_TITLE, "Event ID");
			m4.put(Constants.ITEM_TEXT, emtLogDC.get(0).get(4));
			emtLogListItem.add(m4);
			HashMap<String, Object> m5 = new HashMap<String, Object>();
			m5.put(Constants.ITEM_TITLE, "Ts Cntx Start");
			m5.put(Constants.ITEM_TEXT, emtLogDC.get(0).get(5));
			emtLogListItem.add(m5);
			// HashMap<String, Object> m7 = new HashMap<String, Object>();
			// m7.put(Constants.ITEM_TITLE, "Value");
			// m7.put(Constants.ITEM_TEXT, emtLogDC.get(0).get(5));
			// emtLogListItem.add(m7);
			HashMap<String, Object> m8 = new HashMap<String, Object>();
			m8.put(Constants.ITEM_TITLE, "Tag ID");
			m8.put(Constants.ITEM_TEXT, carrierId);
			emtLogListItem.add(m8);
			HashMap<String, Object> m9 = new HashMap<String, Object>();
			m9.put(Constants.ITEM_TITLE, "Carrier Name");
			m9.put(Constants.ITEM_TEXT, emtLogDC.get(0).get(6));
			emtLogListItem.add(m9);
			SimpleAdapter listItemAdapter = new SimpleAdapter(EmtLogActivity.this, emtLogListItem, R.layout.lot_inquiry_list_item, new String[] { Constants.ITEM_TITLE,
					Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
			list.setAdapter(listItemAdapter);
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}
	}

	@Override
	protected void onResume() {
		if (null == qTask && !CommonUtility.isEmpty(global.getCarrierID())) {
			carrierId = global.getCarrierID();
			mStatusMessageView.setText(getResources().getString(R.string.loading_data));
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("checkTag");
		}
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		if (null != qTask) {
			qTask.cancel(true);
		}
		super.onPause();
	}
	
}
