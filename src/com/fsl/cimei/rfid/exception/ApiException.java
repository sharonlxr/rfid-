package com.fsl.cimei.rfid.exception;

public class ApiException extends BaseException {

	private static final long serialVersionUID = -5710251313124027196L;

	@Override
	public String toString() {
		return "ApiException  " + super.toString();
	}

	public ApiException(String msg) {
		super(msg);
	}

	public ApiException(String msg, String classname, String methodname, String api) {
		super(msg, classname, methodname, api);
	}
}
