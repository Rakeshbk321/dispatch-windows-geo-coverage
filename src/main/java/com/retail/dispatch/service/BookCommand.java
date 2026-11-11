package com.retail.dispatch.service;

import java.time.LocalDate;

public record BookCommand(String orderId, String street, String streetNumber, String communeCode, LocalDate date,
		long timeBlockId) {
}
