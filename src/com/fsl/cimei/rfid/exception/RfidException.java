package com.fsl.cimei.rfid.exception;

public class RfidException extends BaseException {

	private static final long serialVersionUID = 7917834478717238610L;

	public RfidException(String msg) {
		super(msg);
	}
	
	public RfidException(String msg, String classname, String methodname, String api) {
		super(msg, classname, methodname, api);
	}

	@Override
	public String toString() {
		return "RfidException " + super.toString();
	}
}
