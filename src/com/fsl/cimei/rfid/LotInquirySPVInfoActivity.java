package com.fsl.cimei.rfid;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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

public class LotInquirySPVInfoActivity extends BaseActivity {

	private QueryTask qTask;
	private ListView currentInfoListView;
	private List<HashMap<String, Object>> currentInfoListItem = new ArrayList<HashMap<String, Object>>();
	private ListView stepParamHistTitleListView;
	private List<HashMap<String, Object>> stepParamHistTitleListItem = new ArrayList<HashMap<String, Object>>();
	private ListView stepInstrTitleListView;
	private List<HashMap<String, Object>> stepInstrTitleListItem = new ArrayList<HashMap<String, Object>>();
	private DataCollection currentInfo;
	private DataCollection stepParamHist;
	private DataCollection stepInstr;
	private ArrayList<ArrayList<HashMap<String, Object>>> stepParamHistDetailListItem = new ArrayList<ArrayList<HashMap<String, Object>>>();
	private ArrayList<ArrayList<HashMap<String, Object>>> stepInstrDetailListItem = new ArrayList<ArrayList<HashMap<String, Object>>>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_inquiry_spv_info);
		mFormView = findViewById(R.id.inquiry_spv_form);
		mStatusView = findViewById(R.id.inquiry_spv_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		currentInfoListView = (ListView) findViewById(R.id.inquiry_lot_current_info);
		stepParamHistTitleListView = (ListView) findViewById(R.id.inquiry_lot_step_param_hist);
		stepInstrTitleListView = (ListView) findViewById(R.id.inquiry_lot_step_instr);
		currentInfo = new DataCollection();
		stepParamHist = new DataCollection();
		stepInstr = new DataCollection();
		if (null != global.getAoLot() && null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("goToViewSPVInfo");
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
				if (cmdName.equals("goToViewSPVInfo")) {
					goToViewSPVInfo();
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
				if (cmdName.equals("goToViewSPVInfo")) {
					goToViewSPVInfoAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotInquirySPVInfoActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotInquirySPVInfoActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void goToViewSPVInfo() throws BaseException {
			String API = "getSPVAttributes(attributes='spvAOLotId',lotNumber='" + global.getAoLot().getAlotNumber() + "')";
			DataCollection dataContainer = apiExecutorQuery.query("LotInquirySPVInfo", "goToViewSPVInfo", API);
			for (ArrayList<String> row : dataContainer) {
				ArrayList<String> spvAOLotIdRow = new ArrayList<String>();
				for (String cell : row) {
					if (!cell.equalsIgnoreCase("None")) {
						spvAOLotIdRow.add(cell);
					}
				}
				if (spvAOLotIdRow.size() > 0) {
					currentInfo.add(spvAOLotIdRow);
				}
			}

			API = "getLotAttributes(attributes='spvId,spvAssyId'," + "lotNumber='" + global.getAoLot().getAlotNumber() + "')";
			dataContainer = apiExecutorQuery.query("LotInquirySPVInfo", "goToViewSPVInfo", API);
			if (!CommonUtility.isEmpty(dataContainer)) {
				String spvID = dataContainer.get(0).get(0).trim();
				String spvAssyID = dataContainer.get(0).get(1).trim();
				API = "getSPVParmRevisionHistory(attributes='spvAOLotId, spvRevisionDate, stepName, parmName, parmValue, stepOcc'," + "spvAOLotId='" + spvID + "')";
				stepParamHist = apiExecutorQuery.query("LotInquirySPVInfo", "goToViewSPVInfo", API);

				if (!spvID.equals("None")) {
					API = "getSPVInstructionHistory(attributes='spvAOLotId, stepName, spvSetDate, instructions, userId'," + "spvAOLotId='" + spvID + "')";
					stepInstr = apiExecutorQuery.query("LotInquirySPVInfo", "goToViewSPVInfo", API);
				} else {
					API = "getSPVInstructionHistory(attributes='spvAOLotId, stepName, spvSetDate, instructions, userId'," + "spvAOLotId='" + spvAssyID + "')";
					stepInstr = apiExecutorQuery.query("LotInquirySPVInfo", "goToViewSPVInfo", API);
				}

				if (!CommonUtility.isEmpty(stepInstr)) {
					for (int i = 0; i < stepInstr.size(); i++) {
						try {
							stepInstr.get(i).set(3, URLDecoder.decode(stepInstr.get(i).get(3).replace("\\x", "%"), "gbk"));
						} catch (UnsupportedEncodingException e) {
							throw new RfidException(e.toString(), "LotInquirySPVInfo", "goToViewSPVInfo", API);
						}
					}
				}
			}

			if (CommonUtility.isEmpty(currentInfo) && CommonUtility.isEmpty(stepParamHist) && CommonUtility.isEmpty(stepInstr)) {
				throw new RfidException("没有SPV信息", "LotInquirySPVInfo", "goToViewSPVInfo", API);
			}
		}

		private void goToViewSPVInfoAfter() {
			for (int i = 0; i < currentInfo.size(); i++) {
				ArrayList<String> detail = currentInfo.get(i);
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, detail.get(0));
				currentInfoListItem.add(m);
			}
			SimpleAdapter listItemAdapter = new SimpleAdapter(LotInquirySPVInfoActivity.this, currentInfoListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			currentInfoListView.setAdapter(listItemAdapter);

			for (int i = 0; i < stepParamHist.size(); i++) {
				ArrayList<String> detail = stepParamHist.get(i);
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, detail.get(0));
				stepParamHistTitleListItem.add(m);

				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				// SPVAlotID
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.spv_alot_id));
				m1.put(Constants.ITEM_TEXT, detail.get(0));
				list.add(m1);
				// RevDate
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.rev_date));
				m2.put(Constants.ITEM_TEXT, detail.get(1));
				list.add(m2);
				// StepName
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.step_name));
				m3.put(Constants.ITEM_TEXT, detail.get(2));
				list.add(m3);
				// ParameterName
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.param_name));
				m4.put(Constants.ITEM_TEXT, detail.get(3));
				list.add(m4);
				// ParameterValue
				HashMap<String, Object> m5 = new HashMap<String, Object>();
				m5.put(Constants.ITEM_TITLE, getString(R.string.param_value));
				m5.put(Constants.ITEM_TEXT, detail.get(4));
				list.add(m5);
				// StepOccur
				HashMap<String, Object> m6 = new HashMap<String, Object>();
				m6.put(Constants.ITEM_TITLE, getString(R.string.step_occur));
				m6.put(Constants.ITEM_TEXT, detail.get(5));
				list.add(m6);
				stepParamHistDetailListItem.add(list);
			}
			SimpleAdapter listItemAdapter2 = new SimpleAdapter(LotInquirySPVInfoActivity.this, stepParamHistTitleListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			stepParamHistTitleListView.setAdapter(listItemAdapter2);
			stepParamHistTitleListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String title = (String) item.get(Constants.ITEM_TITLE);
					ArrayList<HashMap<String, Object>> temp = stepParamHistDetailListItem.get(position);
					SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotInquirySPVInfoActivity.this, temp, R.layout.lot_inquiry_list_item, new String[] {
							Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
					final ListView detailView = new ListView(LotInquirySPVInfoActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(detailListItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(LotInquirySPVInfoActivity.this);
					builder.setTitle(title).setIcon(android.R.drawable.ic_dialog_info).setView(detailView);
					builder.setPositiveButton(getResources().getString(R.string.close), null);
					builder.show();
				}
			});

			for (int i = 0; i < stepInstr.size(); i++) {
				ArrayList<String> detail = stepInstr.get(i);
				HashMap<String, Object> m = new HashMap<String, Object>();
				m.put(Constants.ITEM_TITLE, detail.get(0));
				stepInstrTitleListItem.add(m);

				ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
				// SPVAlotID
				HashMap<String, Object> m1 = new HashMap<String, Object>();
				m1.put(Constants.ITEM_TITLE, getString(R.string.spv_alot_id));
				m1.put(Constants.ITEM_TEXT, detail.get(0));
				list.add(m1);
				// StepName
				HashMap<String, Object> m2 = new HashMap<String, Object>();
				m2.put(Constants.ITEM_TITLE, getString(R.string.step_name));
				m2.put(Constants.ITEM_TEXT, detail.get(1));
				list.add(m2);
				// SetDate
				HashMap<String, Object> m3 = new HashMap<String, Object>();
				m3.put(Constants.ITEM_TITLE, getString(R.string.set_date));
				m3.put(Constants.ITEM_TEXT, detail.get(2));
				list.add(m3);
				// Instruction
				HashMap<String, Object> m4 = new HashMap<String, Object>();
				m4.put(Constants.ITEM_TITLE, getString(R.string.instrcution));
				m4.put(Constants.ITEM_TEXT, detail.get(3));
				list.add(m4);
				// UserID
				HashMap<String, Object> m5 = new HashMap<String, Object>();
				m5.put(Constants.ITEM_TITLE, getString(R.string.user_id));
				m5.put(Constants.ITEM_TEXT, detail.get(4));
				list.add(m5);
				stepInstrDetailListItem.add(list);
			}
			SimpleAdapter listItemAdapter3 = new SimpleAdapter(LotInquirySPVInfoActivity.this, stepInstrTitleListItem, R.layout.lot_step_hist_list_item,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.title_step_name });
			stepInstrTitleListView.setAdapter(listItemAdapter3);
			stepInstrTitleListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String title = (String) item.get(Constants.ITEM_TITLE);
					ArrayList<HashMap<String, Object>> temp = stepInstrDetailListItem.get(position);
					SimpleAdapter detailListItemAdapter = new SimpleAdapter(LotInquirySPVInfoActivity.this, temp, R.layout.lot_inquiry_list_item, new String[] {
							Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
					final ListView detailView = new ListView(LotInquirySPVInfoActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(detailListItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(LotInquirySPVInfoActivity.this);
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
