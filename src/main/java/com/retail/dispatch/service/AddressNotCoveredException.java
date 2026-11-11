package com.retail.dispatch.service;

public class AddressNotCoveredException extends RuntimeException {

	public AddressNotCoveredException(String communeCode) {
		super("No dispatch zone covers commune " + communeCode);
	}

}
