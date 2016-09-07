package com.fsl.cimei.rfid.entity;

public class AOLot {
	// devcNumber,pkgCode,waferLotNumber,traceCode
	private String alotNumber;
	private String devcNumber;
	private String pkgCode;
	private String waferLotNumber;
	private String traceCode;
	private String lotStatus;
	private String mpqFactor;
	private String maskset;
	private String mooNumber;
	private String traceCode2;
	private String bakeExpireDate;
	private String startDate;
	private String endDate;
	private String startQty;
	private String endQty;
	private String originalTrakLotClass;
	private String assemblyLotNumber;
	private String previousLotNumber;
	private String rackInfo;
	private String spvID;
	private String magazines;
	private String stripNumber;
	private String ptapingExpireTime;
	private String premoldPlasmaExpireTime;
	private String pwirebondExpireTime;

	public String getPwirebondExpireTime() {
		return pwirebondExpireTime;
	}

	public void setPwirebondExpireTime(String pwirebondExpireTime) {
		this.pwirebondExpireTime = pwirebondExpireTime;
	}

	public String getPtapingExpireTime() {
		return ptapingExpireTime;
	}

	public void setPtapingExpireTime(String ptapingExpireTime) {
		this.ptapingExpireTime = ptapingExpireTime;
	}

	public String getPremoldPlasmaExpireTime() {
		return premoldPlasmaExpireTime;
	}

	public void setPremoldPlasmaExpireTime(String premoldPlasmaExpireTime) {
		this.premoldPlasmaExpireTime = premoldPlasmaExpireTime;
	}

	public String getStripNumber() {
		return stripNumber;
	}

	public void setStripNumber(String stripNumber) {
		this.stripNumber = stripNumber;
	}

	public String getMagazines() {
		return magazines;
	}

	public void setMagazines(String magazines) {
		this.magazines = magazines;
	}

	public AOLot() {
	}

	public AOLot(String alotNumber) {
		this.alotNumber = alotNumber;
	}

	public String getSpvID() {
		return spvID;
	}

	public void setSpvID(String spvID) {
		this.spvID = spvID;
	}

	public String getLotStatus() {
		return lotStatus;
	}

	public void setLotStatus(String lotStatus) {
		this.lotStatus = lotStatus;
	}

	public String getMpqFactor() {
		return mpqFactor;
	}

	public void setMpqFactor(String mpqFactor) {
		this.mpqFactor = mpqFactor;
	}

	public String getMaskset() {
		return maskset;
	}

	public void setMaskset(String maskset) {
		this.maskset = maskset;
	}

	public String getMooNumber() {
		return mooNumber;
	}

	public void setMooNumber(String mooNumber) {
		this.mooNumber = mooNumber;
	}

	public String getTraceCode2() {
		return traceCode2;
	}

	public void setTraceCode2(String traceCode2) {
		this.traceCode2 = traceCode2;
	}

	public String getBakeExpireDate() {
		return bakeExpireDate;
	}

	public void setBakeExpireDate(String bakeExpireDate) {
		this.bakeExpireDate = bakeExpireDate;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getStartQty() {
		return startQty;
	}

	public void setStartQty(String startQty) {
		this.startQty = startQty;
	}

	public String getEndQty() {
		return endQty;
	}

	public void setEndQty(String endQty) {
		this.endQty = endQty;
	}

	public String getOriginalTrakLotClass() {
		return originalTrakLotClass;
	}

	public void setOriginalTrakLotClass(String originalTrakLotClass) {
		this.originalTrakLotClass = originalTrakLotClass;
	}

	public String getAssemblyLotNumber() {
		return assemblyLotNumber;
	}

	public void setAssemblyLotNumber(String assemblyLotNumber) {
		this.assemblyLotNumber = assemblyLotNumber;
	}

	public String getPreviousLotNumber() {
		return previousLotNumber;
	}

	public void setPreviousLotNumber(String previousLotNumber) {
		this.previousLotNumber = previousLotNumber;
	}

	public String getRackInfo() {
		return rackInfo;
	}

	public void setRackInfo(String rackInfo) {
		this.rackInfo = rackInfo;
	}

	public String getAlotNumber() {
		return alotNumber;
	}

	public void setAlotNumber(String alotNumber) {
		this.alotNumber = alotNumber;
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

	public String getWaferLotNumber() {
		return waferLotNumber;
	}

	public void setWaferLotNumber(String waferLotNumber) {
		this.waferLotNumber = waferLotNumber;
	}

	public String getTraceCode() {
		return traceCode;
	}

	public void setTraceCode(String traceCode) {
		this.traceCode = traceCode;
	}

	public Step getCurrentStep() {
		return currentStep;
	}

	public void setCurrentStep(Step currentStep) {
		this.currentStep = currentStep;
	}

	private Step currentStep;

}
