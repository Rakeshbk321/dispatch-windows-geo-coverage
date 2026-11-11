package com.retail.dispatch.service;

public class OrderAlreadyBookedException extends RuntimeException {

	public OrderAlreadyBookedException(String orderId) {
		super("Order " + orderId + " already has a confirmed booking");
	}

}
