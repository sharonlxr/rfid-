package com.fsl.cimei.rfid.exception;

public class RfidWifiException extends BaseException {

	private static final long serialVersionUID = -4146455694785104074L;

	public RfidWifiException(String msg) {
		super(msg);
	}
	
	public RfidWifiException(String msg, String classname, String methodname, String api) {
		super(msg, classname, methodname, api);
	}

	@Override
	public String toString() {
		return "WiFi无线问题 " + super.toString();
	}
}
