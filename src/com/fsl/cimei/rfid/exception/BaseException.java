package com.fsl.cimei.rfid.exception;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;

public abstract class BaseException extends Exception {
	private static final long serialVersionUID = 5354462472178598713L;
	private String classname = "";
	private String methodname = "";
	private String api = "";
	private String errorMsg = "";
	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Override
	public String toString() {
		return sdf.format(new Date()) + " -- " + errorMsg + " " + classname + " " + methodname + " " + api;
	}

	public BaseException(String msg) {
		super(msg);
		this.errorMsg = msg;
	}

	public BaseException(String msg, String classname, String methodname, String api) {
		super(msg);
		this.classname = classname;
		this.methodname = methodname;
		this.api = api;
		this.errorMsg = msg;
	}

	public String getClassname() {
		return classname;
	}

	public void setClassname(String classname) {
		this.classname = classname;
	}

	public String getMethodname() {
		return methodname;
	}

	public void setMethodname(String methodname) {
		this.methodname = methodname;
	}

	public String getApi() {
		return api;
	}

	public void setApi(String api) {
		this.api = api;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

}
