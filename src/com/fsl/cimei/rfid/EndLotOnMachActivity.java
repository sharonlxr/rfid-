package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;

public class EndLotOnMachActivity extends BaseActivity {

	private QueryTask qTask;

	private TextView machIdSelect;
	private LinearLayout machIdLine;
	private AlertDialog machIdAlertDialog = null;
	private String[] machIdArray;
	private String machID = "";

	private LinearLayout lotListView;
	private DataCollection lotDC;
	private List<LotViewHolder> lotViewHolderList;
	private List<String> selectedLot;

	private Button submitButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_end_lot_on_mach);
		mFormView = findViewById(R.id.end_lot_on_mach_form);
		mStatusView = findViewById(R.id.end_lot_on_mach_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		showProgress(false);
		machIdSelect = (TextView) findViewById(R.id.end_lot_on_mach_mach_ID);
		machIdLine = (LinearLayout) findViewById(R.id.end_lot_on_mach_ll1);
		machIdLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null != machIdAlertDialog) {
					machIdAlertDialog.show();
				} else {
					if (null == qTask) {
						mStatusMessageView.setText(getResources().getString(R.string.loading_data));
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("getAssignedMach");
					}
				}
			}
		});

		lotListView = (LinearLayout) findViewById(R.id.end_lot_on_mach_lot_list);
		lotDC = new DataCollection();
		lotViewHolderList = new ArrayList<LotViewHolder>();
		selectedLot = new ArrayList<String>();
		submitButton = (Button) findViewById(R.id.end_lot_on_mach_submit);
		submitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				for (LotViewHolder holder : lotViewHolderList) {
					if (holder.cb.isChecked()) {
						selectedLot.add("" + holder.lotNumber.getText());
					}
				}
				if (selectedLot.size() > 0 && null == qTask) {
					mStatusMessageView.setText(getResources().getString(R.string.submitting_data));
					showProgress(true);
					qTask = new QueryTask();
					qTask.execute("endLotOnMachines");
				}
			}
		});
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";
		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("getAssignedMach")) {
					getAssignedMach();
				} else if (cmdName.equals("getLotsByMach")) {
					getLotsByMach();
				} else if (cmdName.equals("endLotOnMachines")) {
					endLotOnMachines();
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
				if (cmdName.equals("getAssignedMach")) {
					getAssignedMachAfter();
				} else if (cmdName.equals("getLotsByMach")) {
					getLotsByMachAfter();
				} else if (cmdName.equals("endLotOnMachines")) {
					endLotOnMachinesAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(EndLotOnMachActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(EndLotOnMachActivity.this, e.getErrorMsg());
				}
			}
		}

		private void endLotOnMachines() throws BaseException {
			CommonTrans commonTrans = new CommonTrans();
			List<String> apiCommandList = new ArrayList<String>();
			for (String alotNumber : selectedLot) {
				DataCollection lotCurrentStepContext = commonTrans.getCurrentStepContext(apiExecutorQuery, alotNumber);
				if (lotCurrentStepContext.size() > 0) {
					String stepName = lotCurrentStepContext.get(0).get(2);
					String api = "endLotOnMachines(transUserId='" + global.getUser().getUserID() + "',stepName='" + stepName + "',machDict={'" + machID + "':0},lotNumber='"
							+ alotNumber + "')";
					apiCommandList.add(api);
				}
			}
			String multiAPI = commonTrans.getMultipleAPI(global.getUser().getUserID(), apiCommandList);
			apiExecutorUpdate.transact("EndLotOnMach", "endLotOnMachines", multiAPI);
		}

		private void endLotOnMachinesAfter() {
			lotViewHolderList.clear();
			lotListView.removeAllViews();
			machID = "";
			machIdSelect.setText(machID);
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void getLotsByMach() throws BaseException {
			CommonTrans commonTrans = new CommonTrans();
			lotDC = commonTrans.getLotsByMach(apiExecutorQuery, global, machID);
		}

		@SuppressLint("InflateParams")
		private void getLotsByMachAfter() {
			if (!CommonUtility.isEmpty(lotDC)) {
				for (ArrayList<String> lot : lotDC) {
					if (CommonUtility.isEmpty(lot.get(2)) || lot.get(2).equalsIgnoreCase("None")) {
						LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.end_lot_on_mach_lot_item, null);
						LotViewHolder holder = new LotViewHolder();
						holder.lotNumber = (TextView) l.findViewById(R.id.end_lot_on_mach_lot_item);
						holder.devcNumber = (TextView) l.findViewById(R.id.end_lot_on_mach_lot_devc);
						holder.cb = (CheckBox) l.findViewById(R.id.end_lot_on_mach_lot_cb);
						holder.lotNumber.setText(lot.get(0));
						holder.devcNumber.setText(lot.get(1));
						holder.cb.setChecked(false);
						lotListView.addView(l);
						lotViewHolderList.add(holder);
					}
				}
			}
		}

		private void getAssignedMach() throws BaseException {
			CommonTrans commonTrans = new CommonTrans();
			machIdArray = commonTrans.getAssignedMachArray(apiExecutorQuery, global);
		}

		private void getAssignedMachAfter() {
			machIdAlertDialog = new AlertDialog.Builder(EndLotOnMachActivity.this).setTitle("选择机台").setItems(machIdArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					machID = "" + machIdArray[which];
					machIdSelect.setText(machID);
					if (qTask == null) {
						mStatusMessageView.setText(getResources().getString(R.string.loading_data));
						showProgress(true);
						lotViewHolderList.clear();
						lotListView.removeAllViews();
						qTask = new QueryTask();
						qTask.execute("getLotsByMach");
					}
				}
			}).setNegativeButton(getResources().getString(R.string.cancel), null).create();
			machIdAlertDialog.show();
		}
	}

	private class LotViewHolder {
		TextView lotNumber;
		TextView devcNumber;
		CheckBox cb;
	}
	
	@Override
	protected void onPause() {
		if (null != qTask) {
			qTask.cancel(true);
		}
		super.onPause();
	}
}
