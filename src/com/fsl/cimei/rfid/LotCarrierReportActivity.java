package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class LotCarrierReportActivity extends BaseActivity {

	private final String classname = "LotCarrierReport";
	private QueryTask qTask;
	private TextView stepSelect;
	private LinearLayout stepLine;
	private AlertDialog stepSelectDialog = null;
	private String[] stepArray;
	private String step = "";
	private ListView infoListView;
	private LotCarrierListAdapter listItemAdapter;
	private Map<String, Map<String, Map<String, List<String>>>> infoData; // lot -> mach -> IN / OUT -> carrier name list
	private List<Map<String, String>> infoListItem;
	private final String LOT = "LOT";
	private final String MACH = "MACH";
	private final String IN = "IN";
	private final String OUT = "OUT";
	private final String serverLink = "LotCarrierServlet";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_carrier_report);
		mFormView = findViewById(R.id.lot_carrier_report_form);
		mStatusView = findViewById(R.id.lot_carrier_report_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		showProgress(false);
		stepSelect = (TextView) findViewById(R.id.lot_carrier_report_step);
		stepLine = (LinearLayout) findViewById(R.id.lot_carrier_report_ll1);
		stepLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null != stepSelectDialog) {
					stepSelectDialog.show();
				} else {
					if (null == qTask) {
						mStatusMessageView.setText(getResources().getString(R.string.loading_data));
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("getStepList");
					}
				}
			}
		});
		infoListView = (ListView) findViewById(R.id.lot_carrier_report_list);
		infoListItem = new ArrayList<Map<String, String>>();
		Map<String, String> titleLine = new HashMap<String, String>();
		titleLine.put(LOT, "物料");
		titleLine.put(MACH, "机台");
		titleLine.put(IN, "上料弹夹");
		titleLine.put(OUT, "下料弹夹");
		infoListItem.add(titleLine);
		listItemAdapter = new LotCarrierListAdapter(LotCarrierReportActivity.this);
		infoListView.setAdapter(listItemAdapter);
	}

	public class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("getStepList")) {
					getStepList();
				} else if (cmdName.equals("getAoLotCarrierByStep")) {
					getAoLotCarrierByStep();
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
				if (cmdName.equals("getStepList")) {
					getStepListAfter();
				} else if (cmdName.equals("getAoLotCarrierByStep")) {
					getAoLotCarrierByStepAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotCarrierReportActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotCarrierReportActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void getStepList() throws BaseException {
			// stepArray = new String[]{"PWEDGE_BOND", "PWIRE_BOND"};
			CommonTrans commonTrans = new CommonTrans();
			String output = commonTrans.queryFromServer(serverLink + "?action=getStepList");
			infoData = new HashMap<String, Map<String, Map<String, List<String>>>>();
			infoListItem.clear();
			Map<String, String> titleLine = new HashMap<String, String>();
			titleLine.put(LOT, "物料");
			titleLine.put(MACH, "机台");
			titleLine.put(IN, "上料弹夹");
			titleLine.put(OUT, "下料弹夹");
			infoListItem.add(titleLine);
			if (!output.startsWith("Success||")) {
				throw new RfidException(output, classname, "getAoLotCarrierByStep", step);
			}
			output = output.substring("Success||".length());
			stepArray = output.split("" + (char) 4);
		}

		private void getStepListAfter() {
			stepSelectDialog = new AlertDialog.Builder(LotCarrierReportActivity.this).setTitle("选择step").setItems(stepArray, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					step = "" + stepArray[which];
					stepSelect.setText(step);
					if (qTask == null) {
						clearUI();
						mStatusMessageView.setText(getResources().getString(R.string.loading_data));
						showProgress(true);
						qTask = new QueryTask();
						qTask.execute("getAoLotCarrierByStep");
					}
				}
			}).setNegativeButton(getResources().getString(R.string.cancel), null).create();
			stepSelectDialog.show();
		}

		private void getAoLotCarrierByStep() throws BaseException {
			CommonTrans commonTrans = new CommonTrans();
			// HttpGet httpGet = new HttpGet(URLEncoder.encode(serverLink + "?action=getAoLotCarrierByStep&stepName="+step,"UTF-8"));
			String output = commonTrans.queryFromServer(serverLink + "?action=getAoLotCarrierByStep&stepName=" + step.replaceAll(" ", "%20"));
			infoData = new HashMap<String, Map<String, Map<String, List<String>>>>();
			infoListItem.clear();
			if (!output.startsWith("Success||")) {
				throw new RfidException(output, classname, "getAoLotCarrierByStep", step);
			}
			output = output.substring("Success||".length());
			if (output.trim().isEmpty()) {
				throw new RfidException("查询结果为0", classname, "getAoLotCarrierByStep", step);
			}
			String[] arr = output.split("" + (char) 4);
			for (String line : arr) {
				String[] tempArr = line.split("" + (char) 3);
				String lot = tempArr[0];
				String mach = tempArr[1];
				String port = tempArr[2];
				String carrierName = tempArr[3];
				Map<String, Map<String, List<String>>> map1;
				if (infoData.containsKey(lot)) {
					map1 = infoData.get(lot);
				} else {
					map1 = new HashMap<String, Map<String, List<String>>>();
					infoData.put(lot, map1);
				}
				if (map1.containsKey(mach)) {
					if (port.equals(IN)) {
						map1.get(mach).get(IN).add(carrierName);
					} else if (port.equals(OUT)) {
						map1.get(mach).get(OUT).add(carrierName);
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
					map1.put(mach, machCarriers);
				}
			}
			Map<String, String> titleLine = new HashMap<String, String>();
			titleLine.put(LOT, "物料");
			titleLine.put(MACH, "机台");
			titleLine.put(IN, "上料弹夹");
			titleLine.put(OUT, "下料弹夹");
			infoListItem.add(titleLine);
			for (String lot : infoData.keySet()) {
				// String lastMach = "";
				boolean newlot = true;
				for (String mach : infoData.get(lot).keySet()) {
					List<String> in = infoData.get(lot).get(mach).get(IN);
					List<String> out = infoData.get(lot).get(mach).get(OUT);
					int size = in.size() > out.size() ? in.size() : out.size();
					for (int i = 0; i < size; i++) {
						Map<String, String> map = new HashMap<String, String>();
						if (newlot) {
							map.put(LOT, lot);
							newlot = false;
						} else {
							map.put(LOT, "");
						}
						// if (!mach.equals(lastMach)) {
						// map.put(LOT, lot);
						// } else {
						// map.put(LOT, "");
						// }
						// lastMach = mach;
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
			}
		}

		private void getAoLotCarrierByStepAfter() {
			listItemAdapter.notifyDataSetChanged();
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
				convertView = mInflater.inflate(R.layout.lot_carrier_report_item, null);
				holder.divider = convertView.findViewById(R.id.lot_carrier_report_item_divider);
				holder.lot = (TextView) convertView.findViewById(R.id.lot_carrier_report_item_lot);
				holder.mach = (TextView) convertView.findViewById(R.id.lot_carrier_report_item_mach);
				holder.in = (TextView) convertView.findViewById(R.id.lot_carrier_report_item_in);
				holder.out = (TextView) convertView.findViewById(R.id.lot_carrier_report_item_out);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.lot.setText(infoListItem.get(position).get(LOT));
			holder.mach.setText(infoListItem.get(position).get(MACH));
			holder.in.setText(infoListItem.get(position).get(IN));
			holder.out.setText(infoListItem.get(position).get(OUT));
			if (position != 0 && !infoListItem.get(position).get(LOT).isEmpty()) {
				holder.divider.setVisibility(View.VISIBLE);
			} else {
				holder.divider.setVisibility(View.GONE);
			}
			return convertView;
		}
	}

	class ViewHolder {
		public View divider;
		public TextView lot;
		public TextView mach;
		public TextView in;
		public TextView out;
	}

	private void clearUI() {
		infoListItem.clear();
		Map<String, String> titleLine = new HashMap<String, String>();
		titleLine.put(LOT, "物料");
		titleLine.put(MACH, "机台");
		titleLine.put(IN, "上料弹夹");
		titleLine.put(OUT, "下料弹夹");
		infoListItem.add(titleLine);
		listItemAdapter.notifyDataSetChanged();
	}
}
