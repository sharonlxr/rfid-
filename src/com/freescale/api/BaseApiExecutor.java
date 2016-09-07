package com.freescale.api;

import interfacemgr.genesis.entity.InterfaceMgrSocketConfig;
import app.entity.DataCollection;
import app.utils.APIExecutor;

import com.fsl.cimei.rfid.CommonUtility;
import com.fsl.cimei.rfid.exception.ApiException;
import com.fsl.cimei.rfid.exception.RfidWifiException;

public class BaseApiExecutor extends APIExecutor {
	String hostname = "";

	public BaseApiExecutor(InterfaceMgrSocketConfig mInterfaceMgrSocketConfig) {
		super(mInterfaceMgrSocketConfig);
		this.hostname = mInterfaceMgrSocketConfig.getHost();
	}
	
	@Override
	public DataCollection query(String command) throws ApiException, RfidWifiException {
		return this.query("", "", command);
	}
	
	public DataCollection query(String classname, String methodname, String command) throws ApiException {
		DataCollection dataCollection = new DataCollection();
		try {
			dataCollection = super.query(command);
		} catch (Exception e) {
			throw new ApiException(e.toString(), classname, methodname, command);
		}
		if (!CommonUtility.isEmpty(getMessage())) {
			throw new ApiException(getMessage(), classname, methodname, command);
		}
		return dataCollection;
	}
	
	@Override
	public String transact(String command) throws ApiException {
		return this.transact("", "", command);
	}
	
	public String transact(String classname, String methodname, String command) throws ApiException {
		String result;
		try {
			result = super.transact(command);
		} catch (Exception e) {
			throw new ApiException(e.toString(), classname, methodname, command);
		}
		if (!CommonUtility.isEmpty(getMessage())) {
			throw new ApiException(getMessage(), classname, methodname, command);
		}
		return result;
	}

}
