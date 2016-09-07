package com.fsl.cimei.rfid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.AOLot;
import com.fsl.cimei.rfid.entity.Step;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class LotInquiryActivity extends BaseActivity {

	private QueryTask qTask;
	private ListView lotInfoListView;
	private List<HashMap<String, Object>> lotInfoListItem;
	private SimpleAdapter listItemAdapter;
	private String lotNumber = "";
	private LinearLayout magLine;
	private TextView magText;
	private String bondingDiagram = "";
	private String bondingDiagramRevision = "";
	private final String classname = "LotInquiry";
	private Set<String> specialDevList = new HashSet<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lot_inquiry);
		mFormView = findViewById(R.id.inquiry_form);
		mStatusView = findViewById(R.id.lot_inquiry_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.lot_inquiry_tb_fragment);
		super.initTagBarcodeInput();
		this.alotNumberTextView.setText(R.string.title_lot_information);
		specialDevList.add("99GC33926PNB");
		lotInfoListView = (ListView) findViewById(R.id.inquiry_lot_info_list);
		lotInfoListItem = new ArrayList<HashMap<String, Object>>();
		listItemAdapter = new SimpleAdapter(LotInquiryActivity.this, lotInfoListItem, R.layout.lot_inquiry_list_item, new String[] { Constants.ITEM_TITLE,
				Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });
		lotInfoListView.setAdapter(listItemAdapter);
		magLine = (LinearLayout) findViewById(R.id.lot_inquiry_mag_line);
		magLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!CommonUtility.isEmpty(lotNumber)) {
					global.setAoLot(new AOLot(lotNumber));
					global.setScanTarget(Constants.SCAN_TARGET_INIT);
					Intent intent = new Intent(LotInquiryActivity.this, CarrierAssignActivity.class);
					startActivity(intent);
				}
			}
		});
		magText = (TextView) findViewById(R.id.lot_inquiry_mag);
		if (!CommonUtility.isEmpty(getIntent().getStringExtra("alotNumber"))) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("lotInquiry", getIntent().getStringExtra("alotNumber"));
		}
	}

	private void clearUI() {
		lotInfoListItem.clear();
		listItemAdapter.notifyDataSetChanged();
		lotNumber = "";
		global.setAoLot(null);
		magText.setText("");
		bondingDiagram = "";
		bondingDiagramRevision = "";
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
				if (cmdName.equals("lotInquiry")) {
					if (params.length == 2) {
						String alotNumber = params[1];
						lotInquiry(alotNumber);
					} else {
						lotInquiry(global.getAoLot().getAlotNumber());
					}
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
				} else if (cmdName.equals("getEBD")) {
					URL url;
					try {
						url = new URL("http://tjnebd.ap.freescale.net:8080/Grails-EBD/generatePDF/queryEbondingPDF?bdnum=" + bondingDiagram + "&revision="
								+ bondingDiagramRevision);
						HttpURLConnection c = (HttpURLConnection) url.openConnection();
						c.setRequestMethod("GET");
						c.setDoOutput(true);
						c.connect();

						String PATH = Environment.getExternalStorageDirectory() + "/download/";
						File file = new File(PATH);
						file.mkdirs();
						File outputFile = new File(file, "1.pdf");
						FileOutputStream fos = new FileOutputStream(outputFile);

						InputStream is = c.getInputStream();

						byte[] buffer = new byte[1024];
						int len1 = 0;
						while ((len1 = is.read(buffer)) != -1) {
							fos.write(buffer, 0, len1);
						}
						fos.close();
						is.close();
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
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
				if (cmdName.equals("lotInquiry")) {
					lotInquiryAfter();
				}
			} else {
				logf(e.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(LotInquiryActivity.this, e.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(LotInquiryActivity.this, e.getErrorMsg());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void lotInquiry(String alotNumber) throws BaseException {
			lotNumber = alotNumber;
			bondingDiagram = "";
			bondingDiagramRevision = "";
			CommonTrans commonTrans = new CommonTrans();
			if (null != global.getUser()) {
				commonTrans.checkUserInfo(apiExecutorQuery, global);
			} else {
				throw new RfidException("用户未登录", classname, "lotInquiry", "");
			}

			// String API = "getUserAttributes(attributes='operation',userId='" + global.getUser().getUID() + "')";
			// DataCollection operation = APIExecutor.query(API);
			// if (!CommonUtility.isEmpty(APIExecutor.getMessage())) {
			// errorMsg = APIExecutor.getMessage();
			// }
			// if (CommonUtility.isEmpty(errorMsg) && operation.size() > 0) {
			// String OperationResult = operation.get(0).get(0).trim();
			// global.getUser().setOperation(OperationResult);
			// }

			// String API = "getCurrentStepContext(attributes='procName,stepSeq,stepName,trakOper',lotNumber='" + lotNumber + "')";

			DataCollection currentStepContext = commonTrans.getCurrentStepContext(apiExecutorQuery, lotNumber);// apiExecutor.query(API);
			AOLot aoLot = new AOLot();// global.getAoLot();
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
				global.setAoLot(aoLot);
			} else {
				throw new RfidException("Lot [" + lotNumber + "] 不在step", classname, "lotInquiry", "lotNumber: " + lotNumber);
			}
			ArrayList<String> stepResult = new ArrayList<String>();
			DataCollection lotValues = new DataCollection();
			String api = "getLotAttributes(attributes='lotNumber,lotStatus,mpqFactor,"
					+ "devcNumber,maskSet,mooNumber,pkgCode,traceCode,traceCode2,bakeExpDate,startDate,endDate,lotQty,endQty,origTrakLotClass,assyLotNumber,previousLotNumber,wipRack,partialRack,holdRack,bondingDiagram,bondingDiagramRevision',"
					+ "operationList=" + global.getUserOperationList() + ", lotNumber='" + lotNumber + "')";
			DataCollection lotResult = apiExecutorQuery.query(classname, "lotInquiry", api);
			if (CommonUtility.isEmpty(lotResult)) {
				throw new RfidException("Lot [" + lotNumber + "] 不存在", classname, "lotInquiry", api);
			}
			if (lotResult.size() == 1) {
				bondingDiagram = lotResult.get(0).get(20).trim();
				bondingDiagramRevision = lotResult.get(0).get(21).trim();
				if (CommonUtility.isEmpty(bondingDiagram) || "None".equalsIgnoreCase(bondingDiagram)) {
					bondingDiagram = "";
				}
				if (CommonUtility.isEmpty(bondingDiagramRevision) || "None".equalsIgnoreCase(bondingDiagramRevision)) {
					bondingDiagramRevision = "";
				}
				String wipRack = lotResult.get(0).get(17).trim();
				String partialRack = lotResult.get(0).get(18).trim();
				String holdRack = lotResult.get(0).get(19).trim();
				String rackInfo;
				if (!"None".equals(wipRack)) {
					rackInfo = wipRack;
					for (int i = 0; i < lotResult.size(); i++) {
						stepResult.add(currentStepContext.get(i).get(2));
						stepResult.add(currentStepContext.get(i).get(3).replace(",", " "));

						for (int j = lotResult.get(i).size() - 1; j >= 17; j--) {
							lotResult.get(i).remove(j);
						}
						for (int l = 0; l < lotResult.get(0).size(); l++) {
							stepResult.add(lotResult.get(i).get(l));
						}
						stepResult.add(rackInfo);
						lotValues.add(stepResult);
					}
				} else if (!"None".equals(partialRack)) {
					rackInfo = partialRack;
					for (int i = 0; i < lotResult.size(); i++) {
						stepResult.add(currentStepContext.get(i).get(2));
						stepResult.add(currentStepContext.get(i).get(3).replace(",", " "));

						for (int j = lotResult.get(i).size() - 1; j >= 17; j--) {
							lotResult.get(i).remove(j);
						}
						for (int l = 0; l < lotResult.get(0).size(); l++) {
							stepResult.add(lotResult.get(i).get(l));
						}
						stepResult.add(rackInfo);
						lotValues.add(stepResult);
					}
				} else if (!"None".equals(holdRack)) {
					rackInfo = holdRack;
					for (int i = 0; i < lotResult.size(); i++) {
						stepResult.add(currentStepContext.get(i).get(2));
						stepResult.add(currentStepContext.get(i).get(3).replace(",", " "));

						for (int j = lotResult.get(i).size() - 1; j >= 17; j--) {
							lotResult.get(i).remove(j);
						}
						for (int l = 0; l < lotResult.get(0).size(); l++) {
							stepResult.add(lotResult.get(i).get(l));
						}
						stepResult.add(rackInfo);
						lotValues.add(stepResult);
					}
				} else if (wipRack.equals("None") && partialRack.equals("None") && holdRack.equals("None")) {
					rackInfo = "None";
					for (int i = 0; i < lotResult.size(); i++) {
						stepResult.add(currentStepContext.get(i).get(2));
						stepResult.add(currentStepContext.get(i).get(3).replace(",", " "));

						for (int j = lotResult.get(i).size() - 1; j >= 17; j--) {
							lotResult.get(i).remove(j);
						}
						for (int l = 0; l < lotResult.get(0).size(); l++) {
							stepResult.add(lotResult.get(i).get(l));
						}
						stepResult.add(rackInfo);
						lotValues.add(stepResult);
					}
				}
			}

			// AOLot object
			if (lotValues.size() > 0) {
				ArrayList<String> arrayContainer = lotValues.get(0);
				// lotNumber,lotStatus,mpqFactor,devcNumber,maskSet,mooNumber,pkgCode,traceCode,traceCode2,bakeExpDate,startDate,endDate,
				// lotQty,endQty,origTrakLotClass,assyLotNumber,previousLotNumber,wipack,partialRack,holdRack
				// currentStep.setStepName(arrayContainer.get(0));
				currentStep.setTrakOper(arrayContainer.get(1));
				// aoLot.setAlotNumber(arrayContainer.get(2));
				aoLot.setLotStatus(arrayContainer.get(3));
				aoLot.setMpqFactor(arrayContainer.get(4));
				aoLot.setDevcNumber(arrayContainer.get(5));
				aoLot.setMaskset(arrayContainer.get(6));
				aoLot.setMooNumber(arrayContainer.get(7));
				aoLot.setPkgCode(arrayContainer.get(8));
				aoLot.setTraceCode(arrayContainer.get(9));
				aoLot.setTraceCode2(arrayContainer.get(10));
				aoLot.setBakeExpireDate(arrayContainer.get(11));
				aoLot.setStartDate(arrayContainer.get(12));
				aoLot.setEndDate(arrayContainer.get(13));
				aoLot.setStartQty(arrayContainer.get(14));
				aoLot.setEndQty(arrayContainer.get(15));
				aoLot.setOriginalTrakLotClass(arrayContainer.get(16));
				aoLot.setAssemblyLotNumber(arrayContainer.get(17));
				aoLot.setPreviousLotNumber(arrayContainer.get(18));
				aoLot.setRackInfo(arrayContainer.get(19));
			}

			api = "getCarrierAttributes(lotNumber='" + lotNumber + "', attributes='carrierId,carrierName')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "lotInquiry", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				StringBuilder sb = new StringBuilder();
				for (ArrayList<String> temp : queryResult) {
					sb.append(",").append(temp.get(1));
				}
				aoLot.setMagazines(sb.toString().substring(1));
			}

			// aoLot.setStripNumber(commonTrans.getStripNumber(apiExecutorQuery, lotNumber));
			aoLot.setStripNumber(commonTrans.getLatestStripNumber(apiExecutorQuery, lotNumber));

			// stepDurationControlForPQ.py
			api = "execSql('select P.PROCESS_END_TIME+120/24 from aolot_hists p where P.ALOT_NUMBER=\\\\'" + lotNumber
					+ "\\\\' and P.STEP_NAME=\\\\'PTAPING\\\\' and rownum=1 ORDER BY P.PROCESS_END_TIME DESC')";
			queryResult = apiExecutorQuery.query(classname, "lotInquiry", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				aoLot.setPtapingExpireTime(queryResult.get(0).get(0));
			}
			api = "execSql('select P.PROCESS_END_TIME+" + (specialDevList.contains(aoLot.getDevcNumber()) ? "24" : "4")
					+ "/24 from aolot_hists p where P.ALOT_NUMBER=\\\\'" + lotNumber
					+ "\\\\' and P.STEP_NAME=\\\\'PRE-MOLD PLASMA\\\\' and rownum=1 ORDER BY P.PROCESS_END_TIME DESC')";
			queryResult = apiExecutorQuery.query(classname, "lotInquiry", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				aoLot.setPremoldPlasmaExpireTime(queryResult.get(0).get(0));
			}
			api = "execSql('select distinct d.MTRL_TYPE from complot cl,devices d where cl.COMPONENT_ITEM=d.devc_number and cl.LOT_CLASS=\\\\'"
					+ lotNumber.substring(0, 2) + "\\\\' and cl.lot_nbr=\\\\'" + lotNumber.substring(2)
					+ "\\\\' and d.MTRL_TYPE in (\\\\'GOLDWIRE\\\\',\\\\'COPPER_WIRE\\\\')')";
			queryResult = apiExecutorQuery.query(classname, "lotInquiry", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				String matlType = queryResult.get(0).get(0);
				if (matlType.equals("COPPER_WIRE")) {
					api = "execSql('select P.PROCESS_END_TIME+48/24 from aolot_hists p where P.ALOT_NUMBER=\\\\'" + lotNumber
							+ "\\\\' and P.STEP_NAME=\\\\'PWIRE_BOND\\\\' and rownum = 1 ORDER BY P.PROCESS_END_TIME DESC')";
					queryResult = apiExecutorQuery.query(classname, "lotInquiry", api);
					if (!CommonUtility.isEmpty(queryResult)) {
						aoLot.setPwirebondExpireTime(queryResult.get(0).get(0));
					}
				}
			}

		}

		private void lotInquiryAfter() {
			lotInfoListItem.clear();
			formListItem(lotInfoListItem, Constants.ITEM_TITLE, Constants.ITEM_TEXT);
			listItemAdapter.notifyDataSetChanged();
			magText.setText(global.getAoLot().getMagazines());
			tagBarcodeInput.setVisibility(View.VISIBLE);
			tagBarcodeInput.setText("");
			tagBarcodeInput.requestFocus();
		}
	}

	private void formListItem(List<HashMap<String, Object>> lotInfoListItem, String itemTitle, String itemText) {
		// HashMap<String, Object> m21 = new HashMap<String, Object>();
		// m21.put(itemTitle, getResources().getString(R.string.magazine));
		// m21.put(itemText, global.getAoLot().getMagazines());
		// lotInfoListItem.add(m21);
		if (global.getAoLot() == null) {
			return;
		}
		HashMap<String, Object> m21 = new HashMap<String, Object>();
		m21.put(itemTitle, getResources().getString(R.string.ptaping_expire_time));
		m21.put(itemText, global.getAoLot().getPtapingExpireTime());
		lotInfoListItem.add(m21);
		HashMap<String, Object> m22 = new HashMap<String, Object>();
		m22.put(itemTitle, getResources().getString(R.string.premold_plasma_expire_time));
		m22.put(itemText, global.getAoLot().getPremoldPlasmaExpireTime());
		lotInfoListItem.add(m22);
		HashMap<String, Object> m23 = new HashMap<String, Object>();
		m23.put(itemTitle, getResources().getString(R.string.pwirebond_expire_time));
		m23.put(itemText, global.getAoLot().getPwirebondExpireTime());
		lotInfoListItem.add(m23);
		HashMap<String, Object> m0 = new HashMap<String, Object>();
		m0.put(itemTitle, getResources().getString(R.string.strip_number));
		m0.put(itemText, global.getAoLot().getStripNumber());
		lotInfoListItem.add(m0);
		HashMap<String, Object> m1 = new HashMap<String, Object>();
		m1.put(itemTitle, getResources().getString(R.string.step_name));
		m1.put(itemText, global.getAoLot().getCurrentStep() == null ? "" : global.getAoLot().getCurrentStep().getStepName());
		lotInfoListItem.add(m1);
		HashMap<String, Object> m2 = new HashMap<String, Object>();
		m2.put(itemTitle, getResources().getString(R.string.trak_oper));
		m2.put(itemText, global.getAoLot().getCurrentStep() == null ? "" : global.getAoLot().getCurrentStep().getTrakOper());
		lotInfoListItem.add(m2);
		HashMap<String, Object> m3 = new HashMap<String, Object>();
		m3.put(itemTitle, getResources().getString(R.string.lot_number));
		m3.put(itemText, global.getAoLot().getAlotNumber());
		lotInfoListItem.add(m3);
		HashMap<String, Object> m4 = new HashMap<String, Object>();
		m4.put(itemTitle, getResources().getString(R.string.lot_status));
		m4.put(itemText, global.getAoLot().getLotStatus());
		lotInfoListItem.add(m4);
		HashMap<String, Object> m5 = new HashMap<String, Object>();
		m5.put(itemTitle, getResources().getString(R.string.mpq_factor));
		m5.put(itemText, global.getAoLot().getMpqFactor());
		lotInfoListItem.add(m5);
		HashMap<String, Object> m6 = new HashMap<String, Object>();
		m6.put(itemTitle, getResources().getString(R.string.device_number));
		m6.put(itemText, global.getAoLot().getDevcNumber());
		lotInfoListItem.add(m6);
		HashMap<String, Object> m7 = new HashMap<String, Object>();
		m7.put(itemTitle, getResources().getString(R.string.maskset));
		m7.put(itemText, global.getAoLot().getMaskset());
		lotInfoListItem.add(m7);
		HashMap<String, Object> m8 = new HashMap<String, Object>();
		m8.put(itemTitle, getResources().getString(R.string.moo_number));
		m8.put(itemText, global.getAoLot().getMooNumber());
		lotInfoListItem.add(m8);
		HashMap<String, Object> m9 = new HashMap<String, Object>();
		m9.put(itemTitle, getResources().getString(R.string.package_code));
		m9.put(itemText, global.getAoLot().getPkgCode());
		lotInfoListItem.add(m9);
		HashMap<String, Object> m10 = new HashMap<String, Object>();
		m10.put(itemTitle, getResources().getString(R.string.trace_code));
		m10.put(itemText, global.getAoLot().getTraceCode());
		lotInfoListItem.add(m10);
		HashMap<String, Object> m11 = new HashMap<String, Object>();
		m11.put(itemTitle, getResources().getString(R.string.trace_code2));
		m11.put(itemText, global.getAoLot().getTraceCode2());
		lotInfoListItem.add(m11);
		HashMap<String, Object> m12 = new HashMap<String, Object>();
		m12.put(itemTitle, getResources().getString(R.string.bake_expire_date));
		m12.put(itemText, global.getAoLot().getBakeExpireDate());
		lotInfoListItem.add(m12);
		HashMap<String, Object> m13 = new HashMap<String, Object>();
		m13.put(itemTitle, getResources().getString(R.string.lot_start_date));
		m13.put(itemText, global.getAoLot().getStartDate());
		lotInfoListItem.add(m13);
		HashMap<String, Object> m14 = new HashMap<String, Object>();
		m14.put(itemTitle, getResources().getString(R.string.lot_end_date));
		m14.put(itemText, global.getAoLot().getEndDate());
		lotInfoListItem.add(m14);
		HashMap<String, Object> m15 = new HashMap<String, Object>();
		m15.put(itemTitle, getResources().getString(R.string.lot_start_qty));
		m15.put(itemText, global.getAoLot().getStartQty());
		lotInfoListItem.add(m15);
		HashMap<String, Object> m16 = new HashMap<String, Object>();
		m16.put(itemTitle, getResources().getString(R.string.lot_end_qty));
		m16.put(itemText, global.getAoLot().getEndQty());
		lotInfoListItem.add(m16);
		HashMap<String, Object> m17 = new HashMap<String, Object>();
		m17.put(itemTitle, getResources().getString(R.string.original_trak_lot_class));
		m17.put(itemText, global.getAoLot().getOriginalTrakLotClass());
		lotInfoListItem.add(m17);
		HashMap<String, Object> m18 = new HashMap<String, Object>();
		m18.put(itemTitle, getResources().getString(R.string.assembly_lot_number));
		m18.put(itemText, global.getAoLot().getAssemblyLotNumber());
		lotInfoListItem.add(m18);
		HashMap<String, Object> m19 = new HashMap<String, Object>();
		m19.put(itemTitle, getResources().getString(R.string.previous_lot_number));
		m19.put(itemText, global.getAoLot().getPreviousLotNumber());
		lotInfoListItem.add(m19);
		HashMap<String, Object> m20 = new HashMap<String, Object>();
		m20.put(itemTitle, getResources().getString(R.string.rack_info));
		m20.put(itemText, global.getAoLot().getRackInfo());
		lotInfoListItem.add(m20);
		
	}

	@Override
	protected void onResume() {
		magLine.setVisibility(View.VISIBLE);
		if (null != global.getAoLot() && null == qTask) {
			String alotNumber = global.getAoLot().getAlotNumber();
			clearUI();
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("lotInquiry", alotNumber);
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
		// global.setAoLot(new AOLot(alotNumber));
		if (null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("lotInquiry", alotNumber);
		}
	}

	@Override
	public void setTagId(String tagId) {
		clearUI();
		super.setTagId(tagId);
	}

	public void startScanBarcode() {
		log("LotInquiry startScanBarcode");
		if (qTask == null) {
			tagBarcodeInput.setText("");
			qTask = new QueryTask();
			qTask.execute("scanBarcode");
		}
	}

	@Override
	public void getBondingDiagram() {
		super.getBondingDiagram();
		if (!CommonUtility.isEmpty(bondingDiagram)) {
			Intent intent = new Intent();
			intent.setAction("android.intent.action.VIEW");
			Uri content_url = Uri.parse("http://tjnebd.ap.freescale.net:8080/Grails-EBD/generatePDF/queryEbondingPDF?bdnum=" + bondingDiagram + "&revision="
					+ bondingDiagramRevision);
			intent.setData(content_url);
			startActivity(intent);

			// if (qTask == null) {
			// qTask = new QueryTask();
			// qTask.execute("getEBD");
			// }

		}
	}

	@Override
	public void getAgile() {
		super.getAgile();
		if (!CommonUtility.isEmpty(bondingDiagram)) {
			Intent intent = new Intent();
			intent.setAction("android.intent.action.VIEW");
			Uri content_url = Uri.parse("http://tjnebd.ap.freescale.net:8080/Grails-EBD/generatePDF/queryAgilePDF?bdnum=" + bondingDiagram + "&revision="
					+ bondingDiagramRevision);
			intent.setData(content_url);
			startActivity(intent);
		}
	}
	
	@Override
	public void getOnepage() {
		super.getAgile();
		// http://zmy02hp3:8383/GEN2SPEC/servlet/SoQuery.do?reqtype=onepage&soType=AS&lotNumb=TJMEA1DU4Y21 lotNumber="TJMEA1DU4Y21";
		if (!CommonUtility.isEmpty(lotNumber)) {
			Intent intent = new Intent();
			intent.setAction("android.intent.action.VIEW");
			Uri content_url = Uri.parse("http://zmy02hp3.ap.freescale.net:8383/GEN2SPEC/servlet/SoQuery.do?reqtype=onepage&soType=AS&lotNumb=" + lotNumber);
			intent.setData(content_url);
			startActivity(intent);
		}
	}

}
