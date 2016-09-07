package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class LotCarrierHistActivity extends BaseActivity {

	private QueryTask qTask;
	private ListView infoListView;
	private Map<String, Map<String, List<String>>> infoData; // mach -> IN / OUT -> carrier name list
	private List<Map<String, String>> infoListItem;
	private LotCarrierListAdapter listItemAdapter;
	private String classname = "LotCarrierHist";
	private final String MACH = "MACH";
	private final String IN = "IN";
	private final String OUT = "OUT";
	private TextView stepNameView;
	private TextView assignedView;
	private TextView unusedInputView;
	private String alotNumber = "";
	private String stepName = "";
	private HashSet<String> assignedOutputMag = new HashSet<String>();
	private HashSet<String> unusedInputMag = new HashSet<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_carrier_hist);
		mFormView = findViewById(R.id.lot_carrier_hist_form);
		mStatusView = findViewById(R.id.lot_carrier_hist_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.lot_carrier_hist_tb_fragment);
		super.initTagBarcodeInput();
		stepNameView = (TextView) findViewById(R.id.lot_carrier_hist_step);
		assignedView = (TextView) findViewById(R.id.lot_carrier_hist_assigned);
		unusedInputView = (TextView) findViewById(R.id.lot_carrier_hist_unused_input);
		infoListView = (ListView) findViewById(R.id.lot_carrier_hist_list);
		infoListItem = new ArrayList<Map<String, String>>();
		Map<String, String> titleLine = new HashMap<String, String>();
		titleLine.put(MACH, "机台");
		titleLine.put(IN, "上料弹夹");
		titleLine.put(OUT, "下料弹夹");
		infoListItem.add(titleLine);
//		listItemAdapter = new SimpleAdapter(LotCarrierHistActivity.this, infoListItem, R.layout.lot_carrier_hist_item, new String[] { MACH,
//				IN, OUT }, new int[] { R.id.lot_carrier_hist_item_mach, R.id.lot_carrier_hist_item_in, R.id.lot_carrier_hist_item_out });
		listItemAdapter = new LotCarrierListAdapter(LotCarrierHistActivity.this);
		infoListView.setAdapter(listItemAdapter);
	}

	private void clearUI() {
		alotNumber = "";
		alotNumberTextView.setText("");
		stepName = "";
		stepNameView.setText("");
		infoListItem.clear();
		Map<String, String> titleLine = new HashMap<String, String>();
		titleLine.put(MACH, "机台");
		titleLine.put(IN, "上料弹夹");
		titleLine.put(OUT, "下料弹夹");
		infoListItem.add(titleLine);
		listItemAdapter.notifyDataSetChanged();
		global.setAoLot(null);
	}

	public class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("getAlotCarrierHistsByLot")) {
					String lotNumber = params[1];
					getAlotCarrierHists(lotNumber);
				} else if (cmdName.equals("getAlotCarrierHistsByCarrierID")) {
					String carrierID = params[1];
					String lotNumber = getLotByCarrierID(carrierID);
					getAlotCarrierHists(lotNumber);
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
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
			tagBarcodeInput.requestFocus();
			if (null == e) {
				if (cmdName.equals("getAlotCarrierHistsByLot")) {
					alotNumberTextView.setText(alotNumber);
					stepNameView.setText(stepName);
					assignedView.setText("绑定的弹夹号：" + CommonUtility.formStrFromSet(assignedOutputMag));
					unusedInputView.setText(CommonUtility.formStrFromSet(unusedInputMag));
					listItemAdapter.notifyDataSetChanged();
				} else if (cmdName.equals("getAlotCarrierHistsByCarrierID")) {
					alotNumberTextView.setText(alotNumber);
					stepNameView.setText(stepName);
					assignedView.setText("绑定的弹夹号：" + CommonUtility.formStrFromSet(assignedOutputMag));
					unusedInputView.setText(CommonUtility.formStrFromSet(unusedInputMag));
					listItemAdapter.notifyDataSetChanged();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotCarrierHistActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotCarrierHistActivity.this, e.getErrorMsg());
				}
			}
		}
		
		

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void getAlotCarrierHists(String lotNumber) throws BaseException {
			CommonTrans commonTrans = new CommonTrans();
			DataCollection queryResult = commonTrans.getCurrentStepContext(apiExecutorQuery, lotNumber);
			if (CommonUtility.isEmpty(queryResult)) {
				throw new RfidException(lotNumber + " 不在step", classname, "getAlotCarrierHists", "getCurrentStepContext");
			}
			stepName = queryResult.get(0).get(2);
			String api = "getAlotCarrierHists(attributes='location,port,carrierName',lotNumber='" + lotNumber + "',stepName='" + stepName + "',transType='START')";
			queryResult = apiExecutorQuery.query(classname, "getAlotCarrierHists", api);
			api = "getCarrierAttributes(lotNumber='" + lotNumber + "',attributes='carrierName')";
			DataCollection assignedMagazines = apiExecutorQuery.query(classname, "getAlotCarrierHists", api);
			assignedOutputMag.clear();
			unusedInputMag.clear();
			infoData = new HashMap<String, Map<String, List<String>>>();
			for (ArrayList<String> line : queryResult) {
				String mach = line.get(0);
				String port = line.get(1);
				String carrierName = line.get(2);
				if (port.equals(OUT)) {
					assignedOutputMag.add(carrierName);
				}
				if (infoData.containsKey(mach)) {
					if (port.equals(IN)) {
						infoData.get(mach).get(IN).add(carrierName);
					} else if (port.equals(OUT)) {
						infoData.get(mach).get(OUT).add(carrierName);
					}
				} else {
					Map<String, List<String>> machCarriers = new HashMap<String, List<String>>();
					List<String> in = new ArrayList<String>();
					List<String> out = new ArrayList<String>();
					if (port.equals(IN)) {
						in.add(carrierName);
					} else if (port.equals(OUT)) {
						out.add(carrierName);
					}
					machCarriers.put(IN, in);
					machCarriers.put(OUT, out);
					infoData.put(mach, machCarriers);
				}
			}
			infoListItem.clear();
			Map<String, String> titleLine = new HashMap<String, String>();
			titleLine.put(MACH, "机台");
			titleLine.put(IN, "上料弹夹");
			titleLine.put(OUT, "下料弹夹");
			infoListItem.add(titleLine);
			for (String mach : infoData.keySet()) {
				List<String> in = infoData.get(mach).get(IN);
				List<String> out = infoData.get(mach).get(OUT);
				int size = in.size() > out.size() ? in.size() : out.size();
				for (int i = 0; i < size; i++) {
					Map<String, String> map = new HashMap<String, String>();
					if (i == 0) {
						map.put(MACH, mach);
					} else {
						map.put(MACH, "");
					}
					if (in.size() < i + 1) {
						map.put(IN, "");
					} else {
						map.put(IN, in.get(i));
					}
					if (out.size() < i + 1) {
						map.put(OUT, "");
					} else {
						map.put(OUT, out.get(i));
					}
					infoListItem.add(map);
				}
			}
			for (ArrayList<String> temp : assignedMagazines) {
				if (!assignedOutputMag.contains(temp.get(0))) {
					unusedInputMag.add(temp.get(0));
				}
			}
			alotNumber = lotNumber;
		}

		private String getLotByCarrierID(String mCarrierID) throws BaseException {
			String api = "getCarrierAttributes(carrierId='" + mCarrierID
					+ "', attributes='lotNumber,carrierName,status,location,receiptDate,carrierType,carrierLayer,carrierGroupId,cassetteOrMagazine,waferLotNumber')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "getLotByCarrierID", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				if (!CommonUtility.isEmpty(queryResult.get(0).get(0)) && !queryResult.get(0).get(0).equalsIgnoreCase("None")) {
					String alotNumber = queryResult.get(0).get(0);
					return alotNumber;
				} else {
					throw new RfidException("此RFID标签[" + queryResult.get(0).get(1) + "]未对应物料。", classname, "getLotByCarrierID", api);
				}
			} else {
				throw new RfidException("此RFID标签不存在。", classname, "getLotByCarrierID", api);
			}
		}
	}

	@Override
	protected void onResume() {
		if (null != global.getAoLot() && null == qTask) {
			String alotNumber = global.getAoLot().getAlotNumber();
			clearUI();
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("getAlotCarrierHistsByLot", alotNumber);
		} else if (!CommonUtility.isEmpty(global.getCarrierID()) && qTask == null) {
			clearUI();
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("getAlotCarrierHistsByCarrierID", global.getCarrierID());
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

	public void setBarcodeInput(String alotNumber) {
		clearUI();
		if (null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("getAlotCarrierHistsByLot", alotNumber);
		}
	}

	@Override
	public void setTagId(String tagId) {
		clearUI();
		showProgress(true);
		qTask = new QueryTask();
		qTask.execute("getAlotCarrierHistsByCarrierID", tagId);
		// super.setTagId(tagId);
	}

	public void startScanBarcode() {
		if (qTask == null) {
			tagBarcodeInput.setText("");
			qTask = new QueryTask();
			qTask.execute("scanBarcode");
		}
	}
	
	class LotCarrierListAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private ViewHolder holder = null;

		public LotCarrierListAdapter(Context context) {
			this.mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return infoListItem.size();
		}

		@Override
		public Object getItem(int arg0) {
			return infoListItem.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.lot_carrier_hist_item, null);
				holder.divider = convertView.findViewById(R.id.lot_carrier_hist_item_divider);
				holder.mach = (TextView) convertView.findViewById(R.id.lot_carrier_hist_item_mach);
				holder.in = (TextView) convertView.findViewById(R.id.lot_carrier_hist_item_in);
				holder.out = (TextView) convertView.findViewById(R.id.lot_carrier_hist_item_out);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.mach.setText(infoListItem.get(position).get(MACH));
			holder.in.setText(infoListItem.get(position).get(IN));
			holder.out.setText(infoListItem.get(position).get(OUT));
			holder.out.setTextColor(getResources().getColor(R.color.bg_green));
			if (position != 0 && !infoListItem.get(position).get(MACH).isEmpty()) {
				holder.divider.setVisibility(View.VISIBLE);
			} else {
				holder.divider.setVisibility(View.GONE);
			}
			return convertView;
		}
	}
	
	class ViewHolder {
		public View divider;
		public TextView mach;
		public TextView in;
		public TextView out;
	}
}
