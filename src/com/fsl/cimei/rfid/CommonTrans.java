package com.fsl.cimei.rfid;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;

import app.entity.DataCollection;

import com.freescale.api.BaseApiExecutor;
import com.freescale.api.Constants;
import com.fsl.cimei.rfid.entity.Carrier;
import com.fsl.cimei.rfid.exception.ApiException;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;
import com.fsl.cimei.rfid.exception.RfidWifiException;

public class CommonTrans {

	private String classname = "CommonTrans";

	/**
	 * machName, machId, machModel, machType
	 * 
	 * @param APIExecutor
	 * @param lotNumber
	 * @param stepName
	 * @return DataCollection
	 * @throws ApiException
	 * @throws RfidWifiException
	 */
	public DataCollection getMachine(BaseApiExecutor APIExecutor, String lotNumber, String stepName) throws ApiException, RfidWifiException {
		String API = "getCurrentMachineContext(attributes='machName, machId, machModel, machType',lotNumber='" + lotNumber + "')";
		DataCollection machListDC = APIExecutor.query("CommonTrans", "getMachine", API);
		return machListDC;
	}

	/**
	 * machId
	 * 
	 * @param apiExecutor
	 * @param global
	 * @return DataCollection
	 * @throws ApiException
	 * @throws RfidWifiException
	 */
	public DataCollection getAssignedMachDC(BaseApiExecutor apiExecutor, GlobalVariable global) throws ApiException, RfidWifiException {
		// String api = "execSql('select t.mach_id,m.MACH_MODEL,m.MACH_MTYP_TYPE,m.MACH_NAME from MACH_OPERATOR_HISTS t, machines m where t.user_id=\\\\'"
		// + global.getUser().getUserID() + "\\\\' and user_logout_time is null and (hostname=\\\\'android-" + global.getAndroidSecureID()
		// + ".ap.freescale.net\\\\' or hostname is null) and t.MACH_ID=m.MACH_ID')";
		String api = "getCurrentUserMachAssignment(attributes='machId',userId='" + global.getUser().getUserID() + "')";
		DataCollection assignedMachDC = apiExecutor.query(classname, "getAssignedMachDC", api);
		return assignedMachDC;
	}

	/**
	 * machId
	 * 
	 * @param apiExecutor
	 * @param global
	 * @return String[]
	 * @throws ApiException
	 * @throws RfidWifiException
	 */
	public String[] getAssignedMachArray(BaseApiExecutor apiExecutor, GlobalVariable global) throws ApiException, RfidWifiException {
		// String api = "execSql('select t.mach_id,m.MACH_MODEL,m.MACH_MTYP_TYPE,m.MACH_NAME from MACH_OPERATOR_HISTS t, machines m where t.user_id=\\\\'"
		// + global.getUser().getUserID() + "\\\\' and user_logout_time is null and (hostname=\\\\'android-" + global.getAndroidSecureID()
		// + ".ap.freescale.net\\\\' or hostname is null) and t.MACH_ID=m.MACH_ID')";
		String api = "getCurrentUserMachAssignment(attributes='machId',userId='" + global.getUser().getUserID() + "')";
		DataCollection assignedMachDC = apiExecutor.query(classname, "getAssignedMachArray", api);
		if (!CommonUtility.isEmpty(assignedMachDC)) {
			String[] assignedMachArray = new String[assignedMachDC.size()];
			for (int i = 0; i < assignedMachDC.size(); i++) {
				ArrayList<String> temp = assignedMachDC.get(i);
				assignedMachArray[i] = temp.get(0);
			}
			return assignedMachArray;
		}
		return null;
	}

	/**
	 * lotNumber,devcNumber
	 * 
	 * @param apiExecutor
	 * @param global
	 * @param machID
	 * @return DataCollection
	 * @throws ApiException
	 * @throws RfidWifiException
	 */
	public DataCollection getLotsByMach(BaseApiExecutor apiExecutor, GlobalVariable global, String machID) throws ApiException, RfidWifiException {
		String api = "getCurrentMachineContext(attributes='lotNumber,devcNumber,endTime',machId = '" + machID + "')";
		DataCollection lots = apiExecutor.query(classname, "getLotsByMach", api);
		return lots;
	}

	/**
	 * procName,stepSeq,stepName,trakOper
	 * 
	 * @param apiExecutor
	 * @param alotNumber
	 * @return DataCollection
	 * @throws ApiException
	 * @throws RfidWifiException
	 */
	public DataCollection getCurrentStepContext(BaseApiExecutor apiExecutor, String alotNumber) throws ApiException, RfidWifiException {
		if (CommonUtility.isEmpty(alotNumber)) {
			return new DataCollection();
		}
		String api = "getCurrentStepContext(attributes='procName,stepSeq,stepName,trakOper',lotNumber='" + alotNumber + "')";
		DataCollection lotCurrentStepContext = apiExecutor.query(classname, "getCurrentStepContext", api);
		return lotCurrentStepContext;
	}

	/**
	 * multiple API
	 * 
	 * @param transUserId
	 * @param apiCommandList
	 * @return String
	 */
	public String getMultipleAPI(String transUserId, List<String> apiCommandList) {
		String multipleAPICommand = "processMultipleApis(transUserId='" + transUserId + "',cmdList=[";
		for (String a : apiCommandList) {
			multipleAPICommand = multipleAPICommand + "'" + a.replaceAll("'", "\\\\\\\\'") + "',";
		}
		if (apiCommandList.size() > 0) {
			multipleAPICommand = multipleAPICommand.substring(0, multipleAPICommand.length() - 1);
		}
		multipleAPICommand = multipleAPICommand + "])";
		return multipleAPICommand;
	}

	/**
	 * operation otherValidOper
	 * 
	 * @param apiExecutor
	 * @param global
	 * @throws ApiException
	 * @throws RfidWifiException
	 */
	public void checkUserInfo(BaseApiExecutor apiExecutor, GlobalVariable global) throws ApiException, RfidWifiException {
		if (CommonUtility.isEmpty(global.getUser().getOperation())) {
			String API = "getUserAttributes(attributes='operation',userId='" + global.getUser().getUserID() + "')";
			DataCollection operation = apiExecutor.query(classname, "checkUserInfo", API);
			if (!CommonUtility.isEmpty(operation)) {
				global.getUser().setOperation(operation.get(0).get(0).trim());
			}
		}
		if (CommonUtility.isEmpty(global.getUserOperationList())) {
			String API = "getMESParmValues(attributes='parmValue',parmOwnerType='OPER',parmName='otherValidOper',parmOwner='" + global.getUser().getOperation() + "')";
			DataCollection department = apiExecutor.query(classname, "checkUserInfo", API);
			if (!CommonUtility.isEmpty(department)) {
				List<String> departmentResult = new ArrayList<String>();
				for (int i = 0; i < department.size(); i++) {
					departmentResult.add("'" + department.get(i).get(0).replace(",", "','") + "'");
				}
				departmentResult.add("'" + global.getUser().getOperation() + "'");
				global.setUserOperationList(departmentResult.toString());
			}
		}
	}

	/**
	 * transRoles TROL:SFC level
	 * 
	 * @param apiExecutor
	 * @param userId
	 * @param transType
	 * @return level integer
	 * @throws ApiException
	 * @throws RfidWifiException
	 */
	public int checkUserPrivilege(BaseApiExecutor apiExecutor, String userId, String transType) throws ApiException, RfidWifiException {
		int level = 0;
		String api = "getMESParmValues(attributes='parmValue',parmOwnerType='USER',parmName='transRoles',parmOwner='" + userId + "')";
		DataCollection queryResult = apiExecutor.query(classname, "checkUserPrivilege", api);
		if (!CommonUtility.isEmpty(queryResult)) {
			String role = queryResult.get(0).get(0).trim();
			String[] roleArr;
			if (role.contains(",")) {
				roleArr = role.split(",");
			} else {
				roleArr = new String[1];
				roleArr[0] = role;
			}

			for (String r : roleArr) {
				api = "getMESParmValues(attributes='parmValue',parmOwnerType='TROL:SFC',parmName='level',parmOwner='" + r + ":" + transType + "')";
				queryResult = apiExecutor.query(classname, "checkUserPrivilege", api);
				if (!CommonUtility.isEmpty(queryResult)) {
					String levelStr = queryResult.get(0).get(0).trim();
					if (CommonUtility.isValidNumber(levelStr)) {
						int temp = Integer.parseInt(levelStr);
						if (temp > level) {
							level = temp;
						}
					}
				}
			}
		}
		return level;
	}

	public void putLotIntoCarriers(BaseApiExecutor apiExecutorQuery, BaseApiExecutor apiExecutorUpdate, GlobalVariable global, String mALotNumber, String mCarrierID,
			String port, String comments, String location) throws BaseException {
		if (Constants.carrierAssignLoc && CommonUtility.isEmpty(location)) {
			throw new RfidException("请选择机台", classname, "putLotIntoCarriers", "");
		}
		String api = "getCurrentStepContext(attributes='procName,stepSeq,stepName,startTime', lotNumber='" + mALotNumber + "')";
		DataCollection lotInfo = apiExecutorQuery.query(classname, "putLotIntoCarriers", api);
		if (CommonUtility.isEmpty(lotInfo)) {
			throw new RfidException("Lot [" + mALotNumber + "] 不在step", classname, "putLotIntoCarriers", api);
		} else {
			String procName = lotInfo.get(0).get(0);
			String stepSeq = lotInfo.get(0).get(1);
			String stepName = lotInfo.get(0).get(2);
			String startTime = lotInfo.get(0).get(3);
			if ((stepName.equals(PassWindowActivity.STEP_NAME[0]) || stepName.equals(PassWindowActivity.STEP_NAME[1])) && !startTime.equalsIgnoreCase("None")) {
				throw new RfidException("Lot [" + mALotNumber + "] 在PassWindow step，不能添加删除弹夹", classname, "putLotIntoCarriers", api);
			}
			api = "putLotIntoCarriers(transUserId='" + global.getUser().getUserID() + "', lotNumber='" + mALotNumber + "', carrierIdList=['" + mCarrierID + "',])";
			apiExecutorUpdate.transact(classname, "putLotIntoCarriers", api);
			if (Constants.carrierAssignLoc) {
				api = "setAlotCarrierHists(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + mALotNumber + "', stepName='" + stepName + "', procName='"
						+ procName + "',pseqNumber=" + stepSeq + ",transId='START',port='" + port + "', carrierIdList=['" + mCarrierID + "',], comments='" + comments
						+ "', location='" + location + "')";
			} else {
				api = "setAlotCarrierHists(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + mALotNumber + "', stepName='" + stepName + "', procName='"
						+ procName + "',pseqNumber=" + stepSeq + ",transId='START',port='" + port + "', carrierIdList=['" + mCarrierID + "',], comments='" + comments
						+ "')";// , location='" + global.getAndroidSecureID() + "'
			}
			apiExecutorUpdate.transact(classname, "putLotIntoCarriers", api);
		}
	}

	public void removeLotFromCarriers(BaseApiExecutor apiExecutorQuery, BaseApiExecutor apiExecutorUpdate, GlobalVariable global, String mALotNumber, String mCarrierID,
			String port, String comments, String location) throws BaseException {
		if (Constants.carrierAssignLoc && CommonUtility.isEmpty(location)) {
			throw new RfidException("请选择机台", classname, "removeLotFromCarriers", "");
		}
		String api = "getCurrentStepContext(attributes='procName,stepSeq,stepName,startTime',lotNumber='" + mALotNumber + "')";
		DataCollection lotInfo = apiExecutorQuery.query(classname, "removeLotFromCarriers", api);
		if (!CommonUtility.isEmpty(lotInfo)) {
			String stepName = lotInfo.get(0).get(2);
			String startTime = lotInfo.get(0).get(3);
			if ((stepName.equals(PassWindowActivity.STEP_NAME[0]) || stepName.equals(PassWindowActivity.STEP_NAME[1])) && !startTime.equalsIgnoreCase("None")) {
				throw new RfidException("Lot [" + mALotNumber + "] 在PassWindow step，不能添加删除弹夹", classname, "putLotIntoCarriers", api);
			}
		}
		api = "removeLotFromCarriers(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + mALotNumber + "',carrierIdList=['" + mCarrierID + "',])";
		apiExecutorUpdate.transact(classname, "removeLotFromCarriers", api);
		if (CommonUtility.isEmpty(lotInfo)) {
			throw new RfidException("Lot [" + mALotNumber + "] 不在step", classname, "removeLotFromCarriers", api);
		} else {
			String procName = lotInfo.get(0).get(0);
			String stepSeq = lotInfo.get(0).get(1);
			String stepName = lotInfo.get(0).get(2);
			if (Constants.carrierAssignLoc) {
				api = "setAlotCarrierHists(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + mALotNumber + "',stepName='" + stepName + "',procName='"
						+ procName + "',pseqNumber=" + stepSeq + ",transId='END',port='" + port + "', carrierIdList=['" + mCarrierID + "',], comments='" + comments
						+ "', location='" + location + "')";
			} else {
				api = "setAlotCarrierHists(transUserId='" + global.getUser().getUserID() + "',lotNumber='" + mALotNumber + "',stepName='" + stepName + "',procName='"
						+ procName + "',pseqNumber=" + stepSeq + ",transId='END',port='" + port + "', carrierIdList=['" + mCarrierID + "',], comments='" + comments
						+ "')"; // , location='" + global.getAndroidSecureID() + "'
			}
			apiExecutorUpdate.transact(classname, "removeLotFromCarriers", api);
		}
	}

	public void takeLotOffRack(BaseApiExecutor apiExecutorQuery, BaseApiExecutor apiExecutorUpdate, GlobalVariable global, String alotNumber) throws ApiException,
			RfidWifiException, RfidException {
		String api = "getRackAttributes(content='" + alotNumber + "',attributes='rackName,rackSlot')";
		DataCollection queryResult = apiExecutorQuery.query(classname, "takeItemOffRack", api);
		if (!CommonUtility.isEmpty(queryResult)) {
			for (ArrayList<String> line : queryResult) {
				String rackName = line.get(0);
				String slotName = line.get(1);
				// global.setRackName(rackName);
				// global.setSlotName(slotName);
				if (rackName.startsWith("PQFN")) {
					api = "takeItemOffRack(transUserId='" + global.getUser().getUserID() + "',rackName='" + rackName + "',rackSlot='" + slotName + "',content='"
							+ alotNumber + "')";
					CommonUtility.logError(api, Constants.LOG_FILE_ERR);
					apiExecutorUpdate.transact(classname, "takeItemOffRack", api);
				}
			}
		} else {
			throw new RfidException("该物料[" + alotNumber + "]未被放入RACK", classname, "takeItemOffRack", api);
		}
	}

	public void putLotOnRack(BaseApiExecutor apiExecutorQuery, BaseApiExecutor apiExecutorUpdate, GlobalVariable global, String alotNumber) throws BaseException {
		String api = "getLotAttributes(attributes='lotNumber,lotStatus', lotNumber='" + alotNumber + "')";
		DataCollection dc = apiExecutorQuery.query(classname, "putLotOnRack", api);
		if (!CommonUtility.isEmpty(dc) && dc.size() == 1) {
			api = "putItemOnRack(transUserId='" + global.getUser().getUserID() + "', rackName='" + global.getRackName() + "',rackSlot='" + global.getSlotName()
					+ "',content='" + alotNumber + "')";
			apiExecutorUpdate.transact(classname, "putLotOnRack", api);
		} else {
			throw new RfidException(alotNumber + "有误", classname, "putLotOnRack", api);
		}
	}

	// public String getStripNumber(BaseApiExecutor apiExecutorQuery, String lotNumber) throws BaseException {
	// String api = "getLHSTAttributes(lotNumber='" + lotNumber + "', attributes='STRIP_NUMBER')";
	// DataCollection queryResult2 = apiExecutorQuery.query(classname, "lotInquiry", api);
	// if (!CommonUtility.isEmpty(queryResult2) && !CommonUtility.isEmpty(queryResult2.get(0))) {
	// String strip = queryResult2.get(0).get(0);
	// if (!CommonUtility.isEmpty(strip) && !"None".equalsIgnoreCase(strip)) {
	// return strip;
	// }
	// }
	// return "";
	// }

	public String queryFromServer(String link) throws RfidException {
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), 3000);
			HttpConnectionParams.setSoTimeout(httpClient.getParams(), 3000);
			HttpGet httpGet = new HttpGet(Constants.serverName + link);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			String output = EntityUtils.toString(httpEntity);
			return output;
		} catch (UnsupportedEncodingException e) {
			throw new RfidException("连接服务器出现错误 " + e.toString(), classname, "queryFromServer", Constants.serverName + link);
		} catch (ClientProtocolException e) {
			throw new RfidException("连接服务器出现错误 " + e.toString(), classname, "queryFromServer", Constants.serverName + link);
		} catch (IOException e) {
			throw new RfidException("连接服务器出现错误 " + e.toString(), classname, "queryFromServer", Constants.serverName + link);
		}
	}

	public String[] checkBarcodeInput(BaseApiExecutor apiExecutorQuery, String input, String department) throws BaseException {
		String resultCode = "";
		String lot = input; // default this is lot number
		String carrierId = "";
		String carrierName = "";
		if (!department.startsWith("F/E")) {
			String api = "getCarrierAttributes(carrierName='" + input + "', attributes='status,location,lotNumber,carrierId,carrierType')";
			DataCollection queryResult = apiExecutorQuery.query(classname, "checkBarcodeInput", api);
			if (CommonUtility.isEmpty(queryResult)) { // maybe input is lot number
				lot = input;
				resultCode = "lot";
			} else if (queryResult.size() == 1) { // input is carrier name
				resultCode = "carrier";
				carrierName = input;
				carrierId = queryResult.get(0).get(3);
				if (!CommonUtility.isEmpty(queryResult.get(0).get(2)) && !queryResult.get(0).get(2).equalsIgnoreCase("None")) { // this carrier is occupied magazine
					lot = queryResult.get(0).get(2);
				} else { // this carrier is NOT occupied magazine
					lot = "";
				}
			} else { // multi carrier ID
				lot = "";
				carrierName = input;
				resultCode = "multiCarrier";
			}
		}
		return new String[] { resultCode, lot, carrierId, carrierName };
	}
	
	public List<Carrier> checkBarcodeInput(BaseApiExecutor apiExecutorQuery, String input) throws ApiException {
		List<Carrier> carrierList = new ArrayList<Carrier>();
		String api = "getCarrierAttributes(carrierName='" + input + "', attributes='status,location,lotNumber,carrierId,carrierType')";
		DataCollection queryResult = apiExecutorQuery.query(classname, "checkBarcodeInput", api);
		if (!CommonUtility.isEmpty(queryResult)) { // not carrier name
			for (ArrayList<String> temp : queryResult) {
				if (!CommonUtility.isEmpty(temp.get(0)) && (temp.get(0).equalsIgnoreCase("good") || temp.get(0).equalsIgnoreCase("occupied"))) {
					Carrier carrier = new Carrier(temp.get(3), input);
					carrier.setStatus(temp.get(0));
					carrier.setLocation(temp.get(1));
					carrier.setCarrierType(temp.get(4));
					if (!CommonUtility.isEmpty(temp.get(2)) && !temp.get(2).equalsIgnoreCase("none")) {
						carrier.setLotNumber(temp.get(2));
					}
					carrierList.add(carrier);
				}
			}
		}
		return carrierList;
	}

	public String[] getAoLotAssignedCarrier(BaseApiExecutor apiExecutorQuery, String aoLotNumber) throws BaseException {
//		if (aoLotNumber.equalsIgnoreCase("test1")) {return new String[]{"testType1","testCarrierId1","testCarrierName1,testCarrierName11,testCarrierName111"};}
//		if (aoLotNumber.equalsIgnoreCase("test2")) {return new String[]{"testType2","testCarrierId2","testCarrierName2,testCarrierName22,testCarrierName222"};}
		String lotCarrierType = "";
		String carrierIdList = "";
		String carrierIdNameList = "";
		if (CommonUtility.isEmpty(aoLotNumber)) {
			return new String[] { lotCarrierType, carrierIdList, carrierIdNameList };
		}
		String api = "getCarrierAttributes(lotNumber='" + aoLotNumber + "',attributes='carrierId,carrierName')";
		DataCollection dc = apiExecutorQuery.query(classname, "getAoLotAssignedCarrier", api);
		if (!CommonUtility.isEmpty(dc)) {
			lotCarrierType = "MagazineAO";
			StringBuilder builder1 = new StringBuilder();
			StringBuilder builder2 = new StringBuilder();
			for (ArrayList<String> temp : dc) {
				builder1.append(",'").append(temp.get(0)).append("'");
				builder2.append(",'").append(temp.get(1)).append("'");
			}
			carrierIdList = builder1.toString().substring(1);
			carrierIdNameList = builder2.toString().substring(1);
		}

		api = "getCarrierAttributes(cassetteLotNumber='" + aoLotNumber + "',attributes='carrierId,carrierName')";
		dc = apiExecutorQuery.query(classname, "getAoLotAssignedCarrier", api);
		if (!CommonUtility.isEmpty(dc)) {
			lotCarrierType = "CassetteAO";
			StringBuilder builder1 = new StringBuilder();
			StringBuilder builder2 = new StringBuilder();
			for (ArrayList<String> temp : dc) {
				builder1.append(",'").append(temp.get(0)).append("'");
				builder2.append(",'").append(temp.get(1)).append("'");
			}
			carrierIdList = builder1.toString().substring(1);
			carrierIdNameList = builder2.toString().substring(1);
		}
		return new String[] { lotCarrierType, carrierIdList, carrierIdNameList };
	}

	public void lotDeassign(BaseApiExecutor apiExecutorUpdate, String lotNumber, String carrierIdList, String type, String userId) throws BaseException {
		if (type.equals("MagazineAO")) {
			String api = "removeLotFromCarriers(transUserId='" + userId + "',lotNumber='" + lotNumber + "',carrierIdList=[" + carrierIdList + "], logCarrierHist='Y')";
			apiExecutorUpdate.query(classname, "lotDeassign", api);
		} else if (type.equals("CassetteAO")) {
			String api = "removeBatchLotsFromCarriers(transUserId='" + userId + "',lotNumberList=['" + lotNumber + "'], carrierIdList=[" + carrierIdList
					+ "], logCarrierHist='Y')";
			apiExecutorUpdate.query(classname, "lotDeassign", api);
		}
	}

	public void validateStripNumber(BaseApiExecutor apiExecutorUpdate, BaseApiExecutor apiExecutorQuery, String lotNumber, String stepName, String procName,
			String stepSeq, String transUserId, String eventCode, String hostname) throws BaseException {
		String methodName = "validateStripNumber";
		RfidException rfidException = null;
		// STEP PASS_WINDOW validateStripNumberAtEnd 1
		// String parmValue = getMESParmValue(apiExecutorQuery, "STEP", stepName, "validateStripNumberAtEnd");
		String comments = "VALIDATE FAILED:CODE(RFID_STRIPNUMBER).";
		boolean commentFlag = false;
		String stripNumber = getLatestStripNumber(apiExecutorQuery, lotNumber);
		int stripNumberValue = 0;
		if (CommonUtility.isEmpty(stripNumber) || stripNumber.equalsIgnoreCase("None")) {
			rfidException = new RfidException("Get strip number failed", classname, methodName, lotNumber);
			comments = comments + "Get strip number failed;";
			commentFlag = true;
		} else {
			try {
				stripNumberValue = Integer.parseInt(stripNumber);
				if (stripNumberValue <= 0) {
					rfidException = new RfidException("Strip number is 0", classname, methodName, lotNumber);
					comments = comments + "Strip number is 0;";
					commentFlag = true;
				}
			} catch (NumberFormatException e) {
				rfidException = new RfidException("Strip number is " + stripNumber, classname, methodName, lotNumber);
				comments = comments + "Strip number is 0;";
				commentFlag = true;
			}
		}
		int totalLayers = 0;
		int eachLayer = 0;
		if (!commentFlag) { // Lot have strip number in database
			String api = "getCarrierAttributes(attributes='carrierId,carrierName,carrierLayer',lotNumber='" + lotNumber + "')";
			DataCollection assignedCarriersList = apiExecutorQuery.query(classname, methodName, api);
			if (CommonUtility.isEmpty(assignedCarriersList)) {
				throw new RfidException("No assigned carriers found, please assign carriers!", classname, methodName, api);
			}
			for (ArrayList<String> temp : assignedCarriersList) {
				String carrierName = temp.get(1);
				String carrierLayer = temp.get(2);
				int carrierLayerValue = -1;
				if (CommonUtility.isEmpty(carrierLayer) || carrierLayer.equalsIgnoreCase("None")) {
					comments = comments + "The layers of carrier " + carrierName + " is empty;";
					commentFlag = true;
				} else {
					try {
						carrierLayerValue = Integer.parseInt(carrierLayer);
						totalLayers += carrierLayerValue;
						eachLayer = carrierLayerValue;
					} catch (NumberFormatException e) {
						comments = comments + "The layers of carrier " + carrierName + " is " + carrierLayer + ";";
						commentFlag = true;
					}
				}
				if (carrierLayerValue <= 0) {
					comments = comments + "The layers of carrier " + carrierName + " is 0;";
					commentFlag = true;
				}
			}
			if (commentFlag) {
				rfidException = new RfidException(comments, classname, methodName, api);
			}
		}
		// strip number<=total layers
		if (!commentFlag) {
			if (totalLayers < stripNumberValue) {
				throw new RfidException("Not enough carriers assgined,total strip number is " + stripNumber + ", assigned carriers is " + totalLayers
						+ ", please assign more carriers", classname, methodName, lotNumber);
			}
			if ((totalLayers - stripNumberValue) >= eachLayer) {
				throw new RfidException("One or more extra carriers assgined,total strip number is " + stripNumber + "1, assigned carriers is " + totalLayers
						+ ", please de-assign the extra carriers", classname, methodName, lotNumber);
			}
		} else {
			try {
				insertLotProcEventHist(apiExecutorUpdate, transUserId, lotNumber, procName, stepSeq, stepName, eventCode, comments, hostname);
			} catch (ApiException e) {
				CommonUtility.logError(e.toString(), Constants.LOG_FILE_ERR);
			}
			if (null != rfidException) {
				throw rfidException;
			}
		}
	}

	public String getMESParmValue(BaseApiExecutor apiExecutorQuery, String ownerType, String owner, String name) throws ApiException {
		String api = "getMESParmValues(attributes='parmValue',parmOwnerType='" + ownerType + "',parmOwner='" + owner + "',parmName='" + name + "')";
		DataCollection result = apiExecutorQuery.query(classname, "getMESParmValues", api);
		if (!CommonUtility.isEmpty(result)) {
			return result.get(0).get(0);
		}
		return "";
	}

	public String getLatestStripNumber(BaseApiExecutor apiExecutorQuery, String lotNumber) throws ApiException {
		if (CommonUtility.isEmpty(lotNumber)) {
			return "";
		}
		String api = "execSql('select to_number(LHST.ATTR_VALUE) FROM lhst_attribute_values lhst LEFT JOIN aolot_hists aolot "
				+ "ON LHST.ALOT_NUMBER=AOLOT.ALOT_NUMBER AND LHST.STEP_NAME=AOLOT.STEP_NAME WHERE LHST.ATTR_NAME=\\\\'STRIP_NUMBER\\\\' AND LHST.ALOT_NUMBER=\\\\'"
				+ lotNumber + "\\\\' ORDER BY AOLOT.QUEUE_START_TIME DESC')";
		DataCollection result = apiExecutorQuery.query(classname, "getLatestStripNumber", api);
		if (!CommonUtility.isEmpty(result)) {
			return result.get(0).get(0);
		}
		return "";
	}

	public void insertLotProcEventHist(BaseApiExecutor apiExecutorUpdate, String transUserId, String lotNumber, String procName, String stepSeq, String stepName,
			String eventCode, String comments, String hostname) throws ApiException {
		String api = "insertLotProcEventHist(transUserId='" + transUserId + "',lotNumber='" + lotNumber + "',stepName='" + stepName + "',eventCode='" + eventCode
				+ "',procName='" + procName + "',comments='" + comments + "', stepSeq=" + stepSeq + ", operatorId='" + transUserId + "', hostname='" + hostname + "')";
		apiExecutorUpdate.transact(classname, "insertLotProcEventHist", api);
	}

	public String checkCarrierIdByName(BaseApiExecutor apiExecutorQuery, String mCarrierName, String alotNumber) throws BaseException {
		String api = "getRFIDCarrierIDByCarrierName(carrierName='" + mCarrierName + "',lotNumber='" + alotNumber + "')";
		DataCollection queryResult = apiExecutorQuery.query(classname, "checkCarrierIdByName", api);
		if (CommonUtility.isEmpty(queryResult)) {
			throw new RfidException("此弹夹不存在。", classname, "checkCarrierIdByName", api);
		} else {
			String carrierId = queryResult.get(0).get(0);
			return carrierId;
		}
	}
	
	public DataCollection validateLotNumber(BaseApiExecutor apiExecutorQuery, String lotNumber) throws BaseException {
		String api = "getLotAttributes(attributes='lotStatus,lotNumber,lotQty',lotNumber='" + lotNumber + "')";
		DataCollection dc = apiExecutorQuery.query(classname, "getAoLotInfo", api);
		if (CommonUtility.isEmpty(dc)) {
			throw new RfidException("Lot " + lotNumber + "不存在", classname, "validateLotNumber", api);
		}
		return dc;
	}
}
