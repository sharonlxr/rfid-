package com.fsl.cimei.rfid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import app.entity.DataCollection;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.AOLot;
import com.fsl.cimei.rfid.entity.Step;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;

public class DivLotActivity extends BaseActivity {
	private QueryTask qTask;
	private Map<String, List<Carrier>> alotNumbers;

	public void getAlotNumbers(ArrayList<String> progress,ArrayList<String> magScan,ArrayList<String>magNotScan) {
		for (String key : alotNumbers.keySet()) {
			int count = 0;
			magazineScan= "";
			magazineNotScan = "";
			for (Carrier c : alotNumbers.get(key)) {
				if (c.isChecked) {
					count++;
					magazineScan += c.name + "\n";
				}else{
					magazineNotScan += c.name + "\n";
				}
			}
			
			String stepName = "";
			if (!global.getAoLot().getCurrentStep().getStepName().equals(stepChooseName) && !stepChooseName.equals("任意步")){//step不匹配
				stepName = "Step不匹配";
			}
			
			String lotStatus = "";	
			if ( global.getAoLot().getLotStatus().equals("HO")){//lot HOLD
				lotStatus = "物料 HOLD";
			}
			
			progress.add("位置:" + (alotLocations.indexOf(key) + 1) + " [" + key + "] 分料进度" + count + "/" + alotNumbers.get(key).size()
					+ "  "+ stepName + "  "+ lotStatus);
			magScan.add(magazineScan);
			magNotScan.add(magazineNotScan);
		}
	}
	

	private List<String> alotLocations;
	private String alotNumber;
	String magazineScan;
	String magazineNotScan;
	private TextView magStaView;
	private TextView divLocView;
	private TextView divStringView;
	private Button checkFlagButton;
	private LinearLayout assignedCarrierListView;
	private List<Carrier> carriers = new ArrayList<Carrier>();

	private List<HashMap<String, Object>> lotInfoListItem;
	private int count;
	
	private String stepChooseName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_div_lot);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		mFormView = findViewById(R.id.check_div_form);
		mStatusView = findViewById(R.id.check_div_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		lotInfoListItem = new ArrayList<HashMap<String, Object>>();
		this.tagBarcodeInputFragment = (TagBarcodeInputFragment) getFragmentManager().findFragmentById(R.id.check_div_tb_fragment);
		super.initTagBarcodeInput();
		
		//去掉回退到主界面的功能
		ActionBar actionBar = getActionBar();
		actionBar.setHomeButtonEnabled(false);
		
		checkFlagButton = (Button) findViewById(R.id.check_div_check_flag);
		checkFlagButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (global.getScanTarget().equals(Constants.SCAN_TARGET_DIV_LOT_INIT)) {
					global.setScanTarget(Constants.SCAN_TARGET_DIV_LOT);
					checkFlagButton.setText("分料完成");
				} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_DIV_LOT)) {
					
					AlertDialog.Builder builder = new AlertDialog.Builder(DivLotActivity.this);
					builder.setMessage("确认所有的lot都分料完了吗?");
					builder.setTitle("提示");
					builder.setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							global.setScanTarget(Constants.SCAN_TARGET_DIV_LOT_INIT);
							global.setAoLot(null);
							checkFlagButton.setVisibility(View.INVISIBLE);
							assignedCarrierListView.removeAllViews();
							magStaView.setText("");
							alotNumberTextView.setText("");
							divLocView.setText("");
							divStringView.setText("");
							alotNumbers.remove(alotNumber);
							alotLocations = new ArrayList<String>();//testing 
						}
					});
					builder.setNegativeButton("取消", new android.content.DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
					builder.create().show();
				}
			}
		});
		assignedCarrierListView = (LinearLayout) findViewById(R.id.check_div_list);

		magStaView = (TextView) findViewById(R.id.check_div_sta);
		divLocView = (TextView) findViewById(R.id.div_lot_location);
		divStringView = (TextView) findViewById(R.id.div_string_location);
		alotNumbers = new HashMap<String, List<Carrier>>();
		alotLocations = new ArrayList<String>();

		
		//add step choose
		AlertDialog stepDialog;
		final String[] arrayStep = new String[] {"PLATE", "DEFLASH","VM_INSPECT","任意步" };   
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请选择分料的步骤");
        builder.setSingleChoiceItems(arrayStep, -1, new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int item) {       		
        		stepChooseName = arrayStep[item];
        		Toast.makeText(DivLotActivity.this, arrayStep[item], Toast.LENGTH_SHORT).show();           
        	}
        });
        stepDialog = builder.create();
        stepDialog.show();
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.lot_div, menu);
		return true;
	}

	private class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";

		@Override
		protected BaseException doInBackground(String... params) {
			BaseException exception = null;
			cmdName = params[0];
			try {
				if (cmdName.equals("loadAssignedCarriers")) {
					String mLotNumber = params[1];
					String mCarrierId;
					if (params.length == 3) {
						mCarrierId = params[2];
					} else {
						mCarrierId = "";
					}
					loadAssignedCarriers(mLotNumber, mCarrierId);
					lotInquiry();
				} else if (cmdName.equals("scanBarcode")) {
					scanBarcode(alotNumberInputHandler);
				}

			} catch (BaseException e) {
				exception = e;
			}
			return exception;
		}

		@Override
		protected void onPostExecute(BaseException exception) {
			qTask = null;
			showProgress(false);
			tagBarcodeInput.requestFocus();
			if (exception == null) {
				if (cmdName.equals("loadAssignedCarriers")) {
					loadAssignedCarriersAfter();
				}
			} else {
				logf(exception.toString());
				if (Constants.configFileName.equals("Testing.properties")) {
					showError(DivLotActivity.this, exception.toString());
				} else if (Constants.configFileName.equals("Production.properties")) {
					showError(DivLotActivity.this, exception.toString());
				}
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void loadAssignedCarriers(String mLotNumber, String mCarrierId) throws BaseException {

			if (alotNumbers.get(mLotNumber) == null) {
				alotNumbers.put(mLotNumber, new ArrayList<Carrier>());
				
				//防止扫重复弹夹时location增加
				if(alotLocations.indexOf(mLotNumber)==-1){
					alotLocations.add(mLotNumber);
				}
			}
			alotNumbers.get(mLotNumber).clear();
			String api = "getCarrierAttributes(lotNumber='" + mLotNumber + "', attributes='carrierId,carrierName')";
			DataCollection queryResult = apiExecutorQuery.query("CarrierAssign", "loadAssignedCarriers", api);
			if (!CommonUtility.isEmpty(queryResult)) {
				for (ArrayList<String> temp : queryResult) {
					Carrier c = new Carrier();
					c.id = temp.get(0);
					c.name = temp.get(1);
					c.isChecked = (c.id.equals(mCarrierId));
					alotNumbers.get(mLotNumber).add(c);
				}
			}
			carriers = alotNumbers.get(mLotNumber);
		}

		@SuppressLint("InflateParams")
		private void loadAssignedCarriersAfter() {
			alotNumberTextView.setText(alotNumber);
			assignedCarrierListView.removeAllViews();

			for (Carrier c : carriers) {
				LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.check_mag_list_item, null);
				c.tagNameTextView = (TextView) l.findViewById(R.id.check_mag_item_name);
				c.tagNameTextView.setText(c.name);
				c.tagCheckedFlag = (ImageView) l.findViewById(R.id.check_mag_item_flag);

				if (c.isChecked) {
					c.tagCheckedFlag.setVisibility(View.VISIBLE);
				} else {
					c.tagCheckedFlag.setVisibility(View.INVISIBLE);
				}
				assignedCarrierListView.addView(l);
			}
			checkFlagButton.setText("开始分料");
			checkFlagButton.setVisibility(View.VISIBLE);

			lotInfoListItem = new ArrayList<HashMap<String, Object>>();
			formListItem(lotInfoListItem, Constants.ITEM_TITLE, Constants.ITEM_TEXT);
			SimpleAdapter listItemAdapter = new SimpleAdapter(DivLotActivity.this, lotInfoListItem, R.layout.lot_inquiry_list_item, new String[] { Constants.ITEM_TITLE,
					Constants.ITEM_TEXT }, new int[] { R.id.itemTitle, R.id.itemText });

			final ListView detailView = new ListView(DivLotActivity.this);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			lp.setMargins(6, 6, 6, 6);
			detailView.setLayoutParams(lp);
			detailView.setPadding(6, 6, 6, 6);	
			detailView.setAdapter(listItemAdapter);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(DivLotActivity.this);
			builder.setTitle("Lot Information").setIcon(android.R.drawable.ic_dialog_info).setView(detailView);
			builder.setPositiveButton(getResources().getString(R.string.cancel_div_lot), new DialogInterface.OnClickListener() {  
                @Override  
                public void onClick(DialogInterface dialog,int which) {  
                	alotNumbers.remove(alotNumber);
                	alotLocations.remove(alotNumber);
                }  
            });// 取消分料，删除下载数据
			builder.setNegativeButton(getResources().getString(R.string.ok_div_lot), null);
			builder.show();
		}

		private void lotInquiry() throws BaseException {
			alotNumber = global.getAoLot().getAlotNumber();

			CommonTrans commonTrans = new CommonTrans();
			if (null != global.getUser()) {
				commonTrans.checkUserInfo(apiExecutorQuery, global);
			} else {
				throw new RfidException("用户未登录", "LotInquiry", "lotInquiry", "");
			}

			DataCollection currentStepContext = commonTrans.getCurrentStepContext(apiExecutorQuery, alotNumber);// apiExecutor.query(API);
			AOLot aoLot = global.getAoLot();
			Step currentStep = new Step();
			if (currentStepContext.size() > 0) {
				String ProcNameResult = currentStepContext.get(0).get(0).trim();
				String StepSeqResult = currentStepContext.get(0).get(1).trim();
				String stepNameSession = currentStepContext.get(0).get(2).trim();
				currentStep.setProcName(ProcNameResult);
				currentStep.setStepSeq(StepSeqResult);
				currentStep.setStepName(stepNameSession);
				aoLot.setAlotNumber(alotNumber);
				aoLot.setCurrentStep(currentStep);
			} else {
				throw new RfidException("Lot [" + alotNumber + "] 不在step", "LotInquiry", "lotInquiry", "");
			}
			ArrayList<String> stepResult = new ArrayList<String>();
			DataCollection lotValues = new DataCollection();
			String api = "getLotAttributes(attributes='lotNumber,lotStatus,mpqFactor,"
					+ "devcNumber,maskSet,mooNumber,pkgCode,traceCode,traceCode2,bakeExpDate,startDate,endDate,lotQty,endQty,origTrakLotClass,assyLotNumber,previousLotNumber,wipRack,partialRack,holdRack',"
					+ "lotNumber='" + alotNumber + "')";
			DataCollection lotResult = apiExecutorQuery.query("LotInquiry", "lotInquiry", api);
			if (CommonUtility.isEmpty(lotResult)) {
				throw new RfidException("Lot [" + alotNumber + "] 不存在", "LotInquiry", "lotInquiry", "");
			}
			if (lotResult.size() == 1) {
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

			// strip info
			api = "getLHSTAttributes(lotNumber='" + alotNumber + "', attributes='STRIP_NUMBER')";
			DataCollection queryResult2 = apiExecutorQuery.query("LotInquiry", "lotInquiry", api);
			if (!CommonUtility.isEmpty(queryResult2) && !CommonUtility.isEmpty(queryResult2.get(0))) {
				String strip = queryResult2.get(0).get(0);
				if (!CommonUtility.isEmpty(strip) && !"None".equalsIgnoreCase(strip)) {
					aoLot.setStripNumber(strip);
				} else {
					aoLot.setStripNumber("");
				}
			}
		}
	}

	@Override
	protected void onResume() {
		if (global.getScanTarget().equals(Constants.SCAN_TARGET_DIV_LOT_INIT)) {
			if (null != global.getAoLot()) {
				alotNumber = global.getAoLot().getAlotNumber();
				List<Carrier> carr = new ArrayList<Carrier>();
				alotNumbers.put(alotNumber, carr);
				
				if(alotLocations.indexOf(alotNumber)==-1){
					alotLocations.add(alotNumber);
				}
				alotNumberTextView.setText(alotNumber);
				if (null == qTask) {
					qTask = new QueryTask();
					qTask.execute("loadAssignedCarriers", alotNumber, global.getCarrierID());
				}
			} else {
				checkFlagButton.setVisibility(View.GONE);
			}
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_DIV_LOT)) {
			// checkNewTag(global.getCarrierID());
		}
		super.onResume();
	}

	private void checkNewTag(String mCarrierId) {

		boolean contained = false;
		String targetAlotNum = null;

		for (String alotNum : alotNumbers.keySet()) {
			for (Carrier c : alotNumbers.get(alotNum)) {
				if (c.id.equals(mCarrierId)) {
					contained = true;
					targetAlotNum = alotNum;
					c.isChecked = true;
					c.tagCheckedFlag.setVisibility(View.VISIBLE);
					if (alotLocations.indexOf(alotNum) == -1) {
						alotLocations.add(alotNum);
					}
					divLocView.setText("Location:" + (alotLocations.indexOf(targetAlotNum) + 1) +  "条数:" + global.getAoLot().getStripNumber()+ "\n" + "弹夹号:" + c.name);
					
					StringBuffer divString = new StringBuffer();
					String stepName = "";
					if (!global.getAoLot().getCurrentStep().getStepName().equals(stepChooseName) && !stepChooseName.equals("任意步")){//step不匹配
						stepName = "Step不匹配";
					}
					
					String lotStatus = "";	
					if ( global.getAoLot().getLotStatus().equals("HO")){//lot HOLD
						lotStatus = "物料 HOLD";
					}
					
					divString.append(stepName).append("\n")
							 .append(lotStatus);
					
					divStringView.setText(divString);
					break;
				}
			}
		}
		if (!contained) {
			
//			Toast toast = Toast.makeText(DivLotActivity.this, "错误的弹夹", Toast.LENGTH_SHORT);
//			toast.show();			
			//showError(DivLotActivity.this, "错误的弹夹");
			divLocView.setText("Location: ERROR");
		} else {
			carriers = alotNumbers.get(targetAlotNum);
			int checkedNumber = 0;
			checkedNumber = checkedMagzineNum(carriers);

			magStaView.setText("共" + carriers.size() + "个弹夹" + ",进度:" + checkedNumber + "/" + carriers.size());

			if (checkedNumber == carriers.size()) {
				AlertDialog.Builder builder = new AlertDialog.Builder(DivLotActivity.this);
				builder.setTitle("提示").setIcon(android.R.drawable.ic_dialog_info);
				builder.setMessage("Location：  " + (alotLocations.indexOf(targetAlotNum) + 1) + "\n"+"(" + targetAlotNum + ")" + " 分料完成"
				                   + "\n"+"条数：  "+ global.getAoLot().getStripNumber());
				builder.setNegativeButton(getResources().getString(R.string.know_div_lot), null);
				AlertDialog dialog = builder.show();	
				TextView textView = (TextView) dialog.findViewById(android.R.id.message);
			    textView.setTextSize(35); 
			}

			alotNumberTextView.setText(targetAlotNum);
			assignedCarrierListView.removeAllViews();
			for (Carrier c : carriers) {
				LinearLayout l = (LinearLayout) getLayoutInflater().inflate(R.layout.check_mag_list_item, null);
				c.tagNameTextView = (TextView) l.findViewById(R.id.check_mag_item_name);
				c.tagNameTextView.setText(c.name);
				c.tagCheckedFlag = (ImageView) l.findViewById(R.id.check_mag_item_flag);
				if (c.isChecked) {
					c.tagCheckedFlag.setVisibility(View.VISIBLE);
				} else {
					c.tagCheckedFlag.setVisibility(View.INVISIBLE);
				}
				assignedCarrierListView.addView(l);
			}
		}
	}

	class Carrier {
		String id = "";
		String name = "";
		boolean isChecked = false;
		String lotNumber = "";
		TextView tagNameTextView = null;
		ImageView tagCheckedFlag = null;
	}

	public void setBarcodeInput(String alotNumber) {
		log("DivLot setLotNumber");

		AOLot aoLot = new AOLot();
		aoLot.setAlotNumber(alotNumber);
		global.setAoLot(aoLot);

		this.alotNumber = alotNumber;
		if (null == qTask) {
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("loadAssignedCarriers", alotNumber);
		}
	}

	private int checkedMagzineNum(List<Carrier> carriers) {
		count = 0;
		for (Carrier c : carriers) {
			if (c.isChecked == true) {
				count++;
			}
		}
		return count;
	}

	private void formListItem(List<HashMap<String, Object>> lotInfoListItem, String itemTitle, String itemText) {
		if (global.getAoLot() == null) {
			return;
		}
		HashMap<String, Object> m0 = new HashMap<String, Object>();
		m0.put(itemTitle, getResources().getString(R.string.lot_number));
		m0.put(itemText, global.getAoLot().getAlotNumber());
		lotInfoListItem.add(m0);
		HashMap<String, Object> m1 = new HashMap<String, Object>();
		m1.put(itemTitle, getResources().getString(R.string.strip_number));
		m1.put(itemText, global.getAoLot().getStripNumber());
		lotInfoListItem.add(m1);
		HashMap<String, Object> m2 = new HashMap<String, Object>();
		m2.put(itemTitle, getResources().getString(R.string.step_name));
		m2.put(itemText, global.getAoLot().getCurrentStep() == null ? "" : global.getAoLot().getCurrentStep().getStepName());
		lotInfoListItem.add(m2);	
		HashMap<String, Object> m3 = new HashMap<String, Object>();
		m3.put(itemTitle, getResources().getString(R.string.lot_status));
		m3.put(itemText, global.getAoLot().getLotStatus());
		lotInfoListItem.add(m3);
		HashMap<String, Object> m4 = new HashMap<String, Object>();
		m4.put(itemTitle, getResources().getString(R.string.package_code));
		m4.put(itemText, global.getAoLot().getPkgCode());
		lotInfoListItem.add(m4);
		HashMap<String, Object> m5 = new HashMap<String, Object>();
		m5.put(itemTitle, getResources().getString(R.string.lot_start_qty));
		m5.put(itemText, global.getAoLot().getStartQty());
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
		m9.put(itemTitle, getResources().getString(R.string.trak_oper));
		m9.put(itemText, global.getAoLot().getCurrentStep() == null ? "" : global.getAoLot().getCurrentStep().getTrakOper());
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
		m15.put(itemTitle, getResources().getString(R.string.mpq_factor));
		m15.put(itemText, global.getAoLot().getMpqFactor());
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

	public void startScanBarcode() {
		log("DivLot startScanBarcode");
		if (null == qTask) {
			tagBarcodeInput.setText("");
			qTask = new QueryTask();
			qTask.execute("scanBarcode");
		}
	}

	@Override
	public void setTagId(String tagId) {
		log("DivLot setTagId");
		if (global.getScanTarget().equals(Constants.SCAN_TARGET_DIV_LOT_INIT)) {
			super.setTagId(tagId);
				
		} else if (global.getScanTarget().equals(Constants.SCAN_TARGET_DIV_LOT)) {
			checkNewTag(tagId);
		}
	}

	// key back
	protected void dialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(DivLotActivity.this);
		builder.setMessage("退出将清除已扫描的Lot信息，确定要退出吗?");
		builder.setTitle("提示");
		builder.setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				DivLotActivity.this.finish();
			}
		});
		builder.setNegativeButton("取消", new android.content.DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			dialog();
			return false;
		}
		return false;
	}

}
