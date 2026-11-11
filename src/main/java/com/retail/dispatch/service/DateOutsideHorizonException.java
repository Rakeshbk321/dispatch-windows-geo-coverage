package com.retail.dispatch.service;

import java.time.LocalDate;

public class DateOutsideHorizonException extends WindowNotBookableException {

	public DateOutsideHorizonException(LocalDate date) {
		super("Date " + date + " is outside the booking horizon");
	}

}
