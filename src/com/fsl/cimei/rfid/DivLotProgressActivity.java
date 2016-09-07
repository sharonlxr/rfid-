package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.AOLot;
import com.fsl.cimei.rfid.entity.Step;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class DivLotProgressActivity extends BaseActivity {
	private QueryTask qTask = null;
	private ListView divLotListView;
	private ArrayList<HashMap<String, Object>> divLotListItem;
	private HashMap<String, ArrayList<HashMap<String, Object>>> lotDetailListItem = new HashMap<String, ArrayList<HashMap<String, Object>>>();
	private ArrayList<String> progresses;
	private ArrayList<String> magScan;
	private ArrayList<String> magNotScan;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_div_lot_progress);
		divLotListView = (ListView) findViewById(R.id.div_lot_list);
		divLotListItem = new ArrayList<HashMap<String, Object>>();
		progresses = this.getIntent().getStringArrayListExtra("progress");
		magScan = this.getIntent().getStringArrayListExtra("magScan");
		magNotScan = this.getIntent().getStringArrayListExtra("magNotScan");
		
		if (null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("getDivLotProgress");
		}
	}
	
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		getMenuInflater().inflate(R.menu.lot_div, menu);
//		return true;
//	}
	
	public class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("getDivLotProgress")) {
					getDivLotProgress();
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
				if (cmdName.equals("getDivLotProgress")) {
					getDivLotProgressAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(DivLotProgressActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(DivLotProgressActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void getDivLotProgress() throws BaseException {

			if (progresses == null) 
				return;
			for (int index = 0; index < progresses.size(); index++) {
				String item  = progresses.get(index);
				HashMap<String, Object> listm = new HashMap<String, Object>();
				listm.put(Constants.ITEM_TITLE, item);
				divLotListItem.add(listm);

				String lotNumber = item.substring(item.indexOf("[") + 1, item.lastIndexOf("]"));
				AOLot aoLot = global.getAoLot();

				// get magazine
				String magzineApi = "getCarrierAttributes(lotNumber='" + lotNumber + "', attributes='carrierId,carrierName')";
				DataCollection queryResult = apiExecutorQuery.query("LotInquiry", "lotInquiry", magzineApi);
				if (!CommonUtility.isEmpty(queryResult)) {
					StringBuilder sb = new StringBuilder();
					for (ArrayList<String> temp : queryResult) {
						sb.append(",").append(temp.get(1));
					}
					aoLot.setMagazines(sb.toString().substring(1));
				}

				// get strip num
				String stripApi = "getLHSTAttributes(lotNumber='" + lotNumber + "', attributes='STRIP_NUMBER')";
				DataCollection stripResult = apiExecutorQuery.query("LotInquiry", "lotInquiry", stripApi);
				if (!CommonUtility.isEmpty(stripResult) && !CommonUtility.isEmpty(stripResult.get(0))) {
					String strip = stripResult.get(0).get(0);
					if (!CommonUtility.isEmpty(strip) && !"None".equalsIgnoreCase(strip)) {
						aoLot.setStripNumber(strip);
					} else {
						aoLot.setStripNumber("");
					}
				}

				// get step info
				CommonTrans commonTrans = new CommonTrans();
				if (null != global.getUser()) {
					commonTrans.checkUserInfo(apiExecutorQuery, global);
				} else {
					throw new RfidException("用户未登录", "LotInquiry", "lotInquiry", "");
				}

				DataCollection currentStepContext = commonTrans.getCurrentStepContext(apiExecutorQuery, lotNumber);// apiExecutor.query(API);

				Step currentStep = new Step();
				if (currentStepContext.size() > 0) {
					String ProcNameResult = currentStepContext.get(0).get(0).trim();
					String StepSeqResult = currentStepContext.get(0).get(1).trim();
					String stepNameSession = currentStepContext.get(0).get(2).trim();
					currentStep.setProcName(ProcNameResult);
					currentStep.setStepSeq(StepSeqResult);
					currentStep.setStepName(stepNameSession);
					aoLot.setAlotNumber(lotNumber);
					aoLot.setCurrentStep(currentStep);
				} else {
					throw new RfidException("Lot [" + lotNumber + "] 不在step", "LotInquiry", "lotInquiry", "");
				}

				// get alot info
				String lotApi = "getLotAttributes(attributes='lotNumber,lotStatus,mpqFactor,"
						+ "devcNumber,maskSet,mooNumber,pkgCode,traceCode,traceCode2,bakeExpDate,startDate,endDate,lotQty,endQty,origTrakLotClass,assyLotNumber,previousLotNumber,wipRack,partialRack,holdRack',"
						+  "lotNumber='" + lotNumber + "')";
				DataCollection lotResult = apiExecutorQuery.query("LotInquiry", "lotInquiry", lotApi);
				if (CommonUtility.isEmpty(lotResult)) {
					throw new RfidException("Lot [" + lotNumber + "] 不存在", "LotInquiry", "lotInquiry", "");
				}

				for (ArrayList<String> lot : lotResult) {

					ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();

					HashMap<String, Object> m1 = new HashMap<String, Object>();
					m1.put(Constants.ITEM_TITLE, getString(R.string.lot_number));
					m1.put(Constants.ITEM_TEXT, lot.get(0));
					list.add(m1);

					HashMap<String, Object> m2 = new HashMap<String, Object>();
					m2.put(Constants.ITEM_TITLE, getString(R.string.strip_number));
					m2.put(Constants.ITEM_TEXT, global.getAoLot().getStripNumber());
					list.add(m2);

					HashMap<String, Object> m3 = new HashMap<String, Object>();
					m3.put(Constants.ITEM_TITLE, getString(R.string.step_name));
					m3.put(Constants.ITEM_TEXT, global.getAoLot().getCurrentStep() == null ? "" : global.getAoLot().getCurrentStep().getStepName());
					list.add(m3);

					HashMap<String, Object> m4 = new HashMap<String, Object>();
					m4.put(Constants.ITEM_TITLE, getString(R.string.lot_status));
					m4.put(Constants.ITEM_TEXT, lot.get(1));
					list.add(m4);

					HashMap<String, Object> m5 = new HashMap<String, Object>();
					m5.put(Constants.ITEM_TITLE, getString(R.string.device_number));
					m5.put(Constants.ITEM_TEXT, lot.get(3));
					list.add(m5);

					HashMap<String, Object> m6 = new HashMap<String, Object>();
					m6.put(Constants.ITEM_TITLE, getString(R.string.package_code));
					m6.put(Constants.ITEM_TEXT, lot.get(6));
					list.add(m6);

					HashMap<String, Object> m7 = new HashMap<String, Object>();
					m7.put(Constants.ITEM_TITLE, getString(R.string.lot_start_qty));
					m7.put(Constants.ITEM_TEXT, lot.get(12));
					list.add(m7);

					HashMap<String, Object> m8 = new HashMap<String, Object>();
					m8.put(Constants.ITEM_TITLE, getString(R.string.magazine));
					m8.put(Constants.ITEM_TEXT, global.getAoLot().getMagazines());
					list.add(m8);
					
					HashMap<String, Object> m9 = new HashMap<String, Object>();
					m9.put(Constants.ITEM_TITLE, getString(R.string.magazineScan));
					m9.put(Constants.ITEM_TEXT, magScan.get(index));
					list.add(m9);
					
					HashMap<String, Object> m10 = new HashMap<String, Object>();
					m10.put(Constants.ITEM_TITLE, getString(R.string.magazineNotScan));
					m10.put(Constants.ITEM_TEXT, magNotScan.get(index) );
					list.add(m10);
					lotDetailListItem.put(lotNumber, list);
				}
				
			}
		}

		private void getDivLotProgressAfter() {
			SimpleAdapter listItemAdapter = new SimpleAdapter(DivLotProgressActivity.this, divLotListItem, R.layout.lot_div_progress,
					new String[] { Constants.ITEM_TITLE }, new int[] { R.id.lot_name });
			divLotListView.setAdapter(listItemAdapter);

			divLotListView.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

					HashMap<String, Object> item = (HashMap<String, Object>) parent.getItemAtPosition(position);
					String ALotProcess = (String) item.get(Constants.ITEM_TITLE);

					String lotNumber = ALotProcess.substring(ALotProcess.indexOf("[") + 1, ALotProcess.lastIndexOf("]"));

					SimpleAdapter listItemAdapter = new SimpleAdapter(DivLotProgressActivity.this, lotDetailListItem.get(lotNumber), R.layout.lot_inquiry_list_item,
							new String[] { Constants.ITEM_TITLE, Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });

					final ListView detailView = new ListView(DivLotProgressActivity.this);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(6, 6, 6, 6);
					detailView.setLayoutParams(lp);
					detailView.setPadding(6, 6, 6, 6);
					detailView.setAdapter(listItemAdapter);
					AlertDialog.Builder builder = new AlertDialog.Builder(DivLotProgressActivity.this);
					builder.setTitle("Lot Information").setIcon(android.R.drawable.ic_dialog_info).setView(detailView);
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
