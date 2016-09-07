package com.fsl.cimei.rfid;

import java.text.ParseException;
import java.util.Date;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.freescale.api.DateFormatter;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class PiecePartOptionsActivity extends BaseActivity {
	private QueryTask qTask;

	private String ppMatlType = "";
	private String ppDevc = "";
	private DataCollection ppOptionsDC = new DataCollection();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_piece_part_options);
		mFormView = findViewById(R.id.pp_options_form);
		mStatusView = findViewById(R.id.pp_options_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		if (null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("getPpByMatlTypeAndDevice");
		}
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("getPpByMatlTypeAndDevice")) {
					getPpByMatlTypeAndDevice();
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
				if (cmdName.equals("getPpByMatlTypeAndDevice")) {
					getPpByMatlTypeAndDeviceAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(PiecePartOptionsActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(PiecePartOptionsActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void getPpByMatlTypeAndDevice() throws BaseException {
			boolean onlyActivePPLot = false;
			String API = "getMESParmValues(attributes='parmValue',parmOwnerType='APPL',parmOwner='MaterialMgr',parmName='enableOnlyActivePPLot')";
			DataCollection dc = apiExecutorQuery.query("PiecePartOptions", "getPpByMatlTypeAndDevice", API);
			if (!CommonUtility.isEmpty(dc) && dc.size() > 0) {
				String val = dc.get(0).get(0);
				if (val.equals("1")) {
					onlyActivePPLot = true;
				}
			}
			API = "getSysdate()";
			dc = apiExecutorQuery.query("PiecePartOptions", "getPpByMatlTypeAndDevice", API);
			String sysDate = "";
			if (!CommonUtility.isEmpty(dc) && dc.size() > 0) {
				sysDate = dc.get(0).get(0);
			}
			API = "getPPContainerAttributes(attributes='containerId,ppLotNumber,devcNumber,containerRecvDate,containerOpenTime,floorLifeExpiryDate,currentQty',mtrlType = '"
					+ ppMatlType + "',devcNumber='" + ppDevc + "',status='AC')";
			dc = apiExecutorQuery.query("PiecePartOptions", "getPpByMatlTypeAndDevice", API);
			for (int i = 0; i < dc.size(); i++) {
				boolean add = false;
				if (onlyActivePPLot) {
					String floorLifeExpiryDate = dc.get(i).get(5);
					try {
						Date date1 = DateFormatter.getSimpleDateToDate(floorLifeExpiryDate);
						Date date2 = DateFormatter.getSimpleDateToDate(sysDate);
						if (date1.compareTo(date2) > 0) {
							add = true;
						}
					} catch (ParseException e) {
						throw new RfidException(e.toString(), "PiecePartOptions", "getPpByMatlTypeAndDevice", API);
					}
				} else {
					add = true;
				}
				if (add) {
					ppOptionsDC.add(dc.get(i));
				}
			}
		}

		private void getPpByMatlTypeAndDeviceAfter() {

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
