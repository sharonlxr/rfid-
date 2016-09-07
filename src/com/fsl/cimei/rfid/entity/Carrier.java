package com.fsl.cimei.rfid.entity;

public class Carrier {
	private String tagID = "";
	private String tagName = "";
	private int layer = 0;
	private boolean isDefect = false;
	private String status = "";
	private String location = "";
	private String lotNumber = "";
	private String carrierType = "";

	public Carrier() {
	}

	public Carrier(String tagID, String tagName) {
		this.tagID = tagID;
		this.tagName = tagName;
	}

	public Carrier(String tagID, String tagName, boolean isDefect) {
		this.tagID = tagID;
		this.tagName = tagName;
		this.isDefect = isDefect;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getLotNumber() {
		return lotNumber;
	}

	public void setLotNumber(String lotNumber) {
		this.lotNumber = lotNumber;
	}

	public String getCarrierType() {
		return carrierType;
	}

	public void setCarrierType(String carrierType) {
		this.carrierType = carrierType;
	}

	public int getLayer() {
		return layer;
	}

	public void setLayer(int layer) {
		this.layer = layer;
	}

	public String getTagID() {
		return tagID;
	}

	public void setTagID(String tagID) {
		this.tagID = tagID;
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public boolean isDefect() {
		return isDefect;
	}

	public void setDefect(boolean isDefect) {
		this.isDefect = isDefect;
	}
}
