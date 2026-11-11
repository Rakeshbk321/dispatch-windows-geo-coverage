package com.retail.dispatch.service;

import java.time.LocalDate;
import java.time.LocalTime;

public class WindowSoldOutException extends RuntimeException {

	public WindowSoldOutException(LocalDate date, LocalTime start) {
		super("The " + start + " block on " + date + " is sold out");
	}

}
