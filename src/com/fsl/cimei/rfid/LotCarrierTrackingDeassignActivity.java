package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;

public class LotCarrierTrackingDeassignActivity extends BaseActivity {

	private QueryTask qTask = null;
	private String classname = "LotCarrierTrackingDeassign";
	private String lotNumber = "";
	private Button exitButton;
	private LinearLayout listView;
	private ArrayList<String> availableList = new ArrayList<String>();
	private ArrayList<String[]> toDeassignList = new ArrayList<String[]>();
	private List<LotItemView> layoutList = new ArrayList<LotItemView>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_carrier_tracking_deassign);
		mFormView = findViewById(R.id.lot_carrier_tracking_deassign_form);
		mStatusView = findViewById(R.id.lot_carrier_tracking_deassign_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		exitButton = (Button) findViewById(R.id.lot_carrier_tracking_deassign_cancel);
		listView = (LinearLayout) findViewById(R.id.lot_carrier_tracking_deassign_list);
		lotNumber = getIntent().getStringExtra("lotNumber");
		if (!CommonUtility.isEmpty(lotNumber)) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("getAOLotsFromSameWlotRFIDAssign");
		} else {
			availableList.clear();
			goBack();
		}

		exitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				goBack();
			}
		});
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";
		int index = -1;
		String assignedLot = "";
		String lotQty = "";

		@Override
		protected BaseException doInBackground(String... params) {
			BaseException exception = null;
			cmdName = params[0];
			try {
				if (cmdName.equals("getAOLotsFromSameWlotRFIDAssign")) {
					getAOLotsFromSameWlotRFIDAssign();
				} else if (cmdName.equals("lotDeassign")) {
					// lotNumber, lotCarrierType, carrierIdList, carrierIdNameList
					index = Integer.parseInt(params[1]);
					this.assignedLot = toDeassignList.get(index)[0];
					this.lotQty = toDeassignList.get(index)[4];
					CommonTrans commonTrans = new CommonTrans();
					commonTrans.lotDeassign(apiExecutorUpdate, assignedLot, toDeassignList.get(index)[2], toDeassignList.get(index)[1], global.getUser().getUserID());
				}
			} catch (BaseException e) {
				exception = e;
			}
			return exception;
		}

		@Override
		protected void onPostExecute(BaseException e) {
			qTask = null;
			showProgress(false);
			if (e == null) {
				if (cmdName.equals("getAOLotsFromSameWlotRFIDAssign")) {
					if (toDeassignList.isEmpty()) {
						goBack();
					} else {
						initListView();
					}
				} else if (cmdName.equals("lotDeassign")) {
					availableList.add(assignedLot + "|" + lotQty);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
						int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
						layoutList.get(index).doneImg.setVisibility(View.VISIBLE);
						layoutList.get(index).doneImg.animate().setDuration(shortAnimTime).alpha(1).setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								layoutList.get(index).doneImg.setVisibility(View.VISIBLE);
							}
						});
						layoutList.get(index).deassign.setVisibility(View.VISIBLE);
						layoutList.get(index).deassign.animate().setDuration(shortAnimTime).alpha(0).setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								layoutList.get(index).deassign.setVisibility(View.GONE);
							}
						});
					} else {
						layoutList.get(index).doneImg.setVisibility(View.VISIBLE);
						layoutList.get(index).deassign.setVisibility(View.GONE);
					}
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotCarrierTrackingDeassignActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotCarrierTrackingDeassignActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void getAOLotsFromSameWlotRFIDAssign() throws BaseException {
			if (CommonUtility.isEmpty(lotNumber)) {
				availableList.clear();
				toDeassignList.clear();
				return;
			}
			String api = "getAOLotsFromSameWlotRFIDAssign(lotNumber='" + lotNumber + "')";
			DataCollection dc = apiExecutorQuery.query(classname, "getAOLotsFromSameWlotRFIDAssign", api);
//			ArrayList<String> a = new ArrayList<String>();a.add("TEST1");a.add("1000");
//			ArrayList<String> b = new ArrayList<String>();b.add("TEST2");b.add("2000");
//			dc.add(a);dc.add(b);
			if (dc.size() > 0) {
				CommonTrans commonTrans = new CommonTrans();
				for (ArrayList<String> temp : dc) {
					String qty = ((CommonUtility.isEmpty(temp.get(1)) || temp.get(1).equalsIgnoreCase("none")) ? "" : temp.get(1));
					String[] result = commonTrans.getAoLotAssignedCarrier(apiExecutorQuery, temp.get(0));
					if (result[0].isEmpty()) {
						availableList.add(temp.get(0) + "|" + qty);
					} else { // lotNumber, lotCarrierType, carrierIdList, carrierIdNameList
						toDeassignList.add(new String[] { temp.get(0), result[0], result[1], result[2], qty }); 
					}
				}
			}
		}

		@SuppressLint("InflateParams")
		private void initListView() {
			for (int i = 0; i < toDeassignList.size(); i++) {
				String[] temp = toDeassignList.get(i);
				LinearLayout aoLotLine = (LinearLayout) getLayoutInflater().inflate(R.layout.lot_carrier_tracking_deassign_list_item, null);
				TextView name = (TextView) aoLotLine.findViewById(R.id.lot_carrier_tracking_deassign_name);
				name.setText("Carrier " + temp[3] + " 已绑定到 lot " + temp[0]);
				TextView deassign = (TextView) aoLotLine.findViewById(R.id.lot_carrier_tracking_deassign_submit);
				deassign.setOnClickListener(new DeassignButtonOnClickListener(i));
				ImageView doneImg = (ImageView) aoLotLine.findViewById(R.id.lot_carrier_tracking_deassign_done);
				doneImg.setVisibility(View.GONE);
				layoutList.add(new LotItemView(aoLotLine, name, deassign, doneImg));
				listView.addView(aoLotLine);
			}
		}

	}

	private class DeassignButtonOnClickListener implements View.OnClickListener {
		private int index = -1;

		DeassignButtonOnClickListener(int index) {
			this.index = index;
		}

		@Override
		public void onClick(View v) {
			if (null == qTask) {
				showProgress(true);
				qTask = new QueryTask();
				qTask.execute("lotDeassign", "" + index);
			}
		}
	}

	private void goBack() {
		Intent intent = new Intent(LotCarrierTrackingDeassignActivity.this, LotCarrierTrackingActivity.class);
		intent.putStringArrayListExtra("availableLotList", availableList);
		setResult(11, intent);
		finish();
	}

	private class LotItemView {
		@SuppressWarnings("unused")
		LinearLayout line;
		@SuppressWarnings("unused")
		TextView name;
		TextView deassign;
		ImageView doneImg;

		LotItemView(LinearLayout line, TextView name, TextView deassign, ImageView doneImg) {
			this.line = line;
			this.name = name;
			this.deassign = deassign;
			this.doneImg = doneImg;
		}
	}
}
