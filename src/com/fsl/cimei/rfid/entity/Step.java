package com.fsl.cimei.rfid.entity;

public class Step {
	// procName,stepSeq,stepName,startQty,startTime,startUserId,lotStatus,devcNumber,pkgCode

	private String procName;
	private String stepSeq;
	private String stepName;
	private String startQty;
	private String startTime;
	private String startUserId;
	private String lotStatus;
	private String devcNumber;
	private String pkgCode;
	private String trakOper;
	private String prodLine;
	private String rejQty;

	public String getRejQty() {
		return rejQty;
	}

	public void setRejQty(String rejQty) {
		this.rejQty = rejQty;
	}

	public String getProdLine() {
		return prodLine;
	}

	public void setProdLine(String prodLine) {
		this.prodLine = prodLine;
	}

	public String getTrakOper() {
		return trakOper;
	}

	public void setTrakOper(String trakOper) {
		this.trakOper = trakOper;
	}

	public String getProcName() {
		return procName;
	}

	public void setProcName(String procName) {
		this.procName = procName;
	}

	public String getStepSeq() {
		return stepSeq;
	}

	public void setStepSeq(String stepSeq) {
		this.stepSeq = stepSeq;
	}

	public String getStepName() {
		return stepName;
	}

	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	public String getStartQty() {
		return startQty;
	}

	public void setStartQty(String startQty) {
		this.startQty = startQty;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getStartUserId() {
		return startUserId;
	}

	public void setStartUserId(String startUserId) {
		this.startUserId = startUserId;
	}

	public String getLotStatus() {
		return lotStatus;
	}

	public void setLotStatus(String lotStatus) {
		this.lotStatus = lotStatus;
	}

	public String getDevcNumber() {
		return devcNumber;
	}

	public void setDevcNumber(String devcNumber) {
		this.devcNumber = devcNumber;
	}

	public String getPkgCode() {
		return pkgCode;
	}

	public void setPkgCode(String pkgCode) {
		this.pkgCode = pkgCode;
	}

}
