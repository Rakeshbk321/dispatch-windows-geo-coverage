package com.retail.dispatch.service;

import java.util.UUID;

public class BookingNotFoundException extends RuntimeException {

	public BookingNotFoundException(UUID id) {
		super("Booking " + id + " not found");
	}

}
