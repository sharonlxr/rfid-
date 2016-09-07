package com.fsl.cimei.rfid;

import interfacemgr.genesis.entity.InterfaceMgrSocketConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Application;
import app.utils.login.genesis.GenesisUser;

import com.fsl.cimei.rfid.entity.AOLot;
import com.fsl.cimei.rfid.entity.Mach;

public class GlobalVariable extends Application {
	private InterfaceMgrSocketConfig interfaceMgrSocketConfigUpdate = null;
	private InterfaceMgrSocketConfig interfaceMgrSocketConfigQuery = null;
	private Map<String, String> configTest = new HashMap<String, String>();
	private Map<String, String> configProd = new HashMap<String, String>();
	private GenesisUser user = null;
	private AOLot aoLot = null;
	private String tagUUID = null;
	private String androidSecureID = null;
	private String rackName = "";
	private String slotName = "";
	private String carrierID = "";
	private String machID = "";
	private String scanTarget = "";
	private List<Mach> selectedMachList = new ArrayList<Mach>();
	private String userOperationList = "";
	private List<HashMap<String, String>> msgListItem = new ArrayList<HashMap<String, String>>();
	private boolean firstNewMsg = false;
	private boolean shown = false;

	public boolean isShown() {
		return shown;
	}

	public void setShown(boolean shown) {
		this.shown = shown;
	}

	public boolean isFirstNewMsg() {
		return firstNewMsg;
	}

	public void setFirstNewMsg(boolean firstNewMsg) {
		this.firstNewMsg = firstNewMsg;
	}

	public List<HashMap<String, String>> getMsgListItem() {
		return msgListItem;
	}

	public void setMsgListItem(List<HashMap<String, String>> msgListItem) {
		this.msgListItem = msgListItem;
	}

	public String getUserOperationList() {
		return userOperationList;
	}

	public void setUserOperationList(String userOperationList) {
		this.userOperationList = userOperationList;
	}

	public List<Mach> getSelectedMachList() {
		return selectedMachList;
	}

	public void setSelectedMachList(List<Mach> selectedMachList) {
		this.selectedMachList = selectedMachList;
	}

	public String getScanTarget() {
		return scanTarget;
	}

	public void setScanTarget(String scanTarget) {
		this.scanTarget = scanTarget;
	}

	public String getCarrierID() {
		return carrierID;
	}

	public void setCarrierID(String carrierID) {
		this.carrierID = carrierID;
	}
	
	public String getMachID() {
		return machID;
	}
	
	public void setMachID(String machID) {
		this.machID = machID;
	}

	public String getRackName() {
		return rackName;
	}

	public void setRackName(String rackName) {
		this.rackName = rackName;
	}

	public String getSlotName() {
		return slotName;
	}

	public void setSlotName(String slotName) {
		this.slotName = slotName;
	}

	public String getAndroidSecureID() {
		return androidSecureID;
	}

	public void setAndroidSecureID(String androidSecureID) {
		this.androidSecureID = androidSecureID;
	}

	public InterfaceMgrSocketConfig getInterfaceMgrSocketConfigUpdate() {
		return interfaceMgrSocketConfigUpdate;
	}

	public void setInterfaceMgrSocketConfigUpdate(InterfaceMgrSocketConfig interfaceMgrSocketConfigUpdate) {
		this.interfaceMgrSocketConfigUpdate = interfaceMgrSocketConfigUpdate;
	}

	public InterfaceMgrSocketConfig getInterfaceMgrSocketConfigQuery() {
		return interfaceMgrSocketConfigQuery;
	}

	public void setInterfaceMgrSocketConfigQuery(InterfaceMgrSocketConfig interfaceMgrSocketConfigQuery) {
		this.interfaceMgrSocketConfigQuery = interfaceMgrSocketConfigQuery;
	}

	public GenesisUser getUser() {
		return user;
	}

	public void setUser(GenesisUser user) {
		this.user = user;
	}

	public AOLot getAoLot() {
		return aoLot;
	}

	public void setAoLot(AOLot aoLot) {
		this.aoLot = aoLot;
	}

	public String getTagUUID() {
		return tagUUID;
	}

	public void setTagUUID(String tagUUID) {
		this.tagUUID = tagUUID;
	}
	
	public Map<String, String> getConfigTest() {
		return configTest;
	}

	public void setConfigTest(Map<String, String> configTest) {
		this.configTest = configTest;
	}

	public Map<String, String> getConfigProd() {
		return configProd;
	}

	public void setConfigProd(Map<String, String> configProd) {
		this.configProd = configProd;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}
	
}
