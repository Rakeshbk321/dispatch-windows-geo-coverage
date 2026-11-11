package com.retail.dispatch.service;

public class UnknownCommuneException extends RuntimeException {

	private final String communeCode;

	public UnknownCommuneException(String communeCode) {
		super("Unknown commune: " + communeCode);
		this.communeCode = communeCode;
	}

	public String getCommuneCode() {
		return communeCode;
	}

}
