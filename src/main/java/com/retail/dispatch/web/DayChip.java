package com.retail.dispatch.web;

import com.retail.dispatch.domain.AvailabilityStatus;
import java.time.LocalDate;

public record DayChip(LocalDate date, String weekday, String dayMonth, AvailabilityStatus status, boolean selected) {

	public boolean selectable() {
		return status == AvailabilityStatus.AVAILABLE;
	}

	public String note() {
		return switch (status) {
			case AVAILABLE -> "";
			case SOLD_OUT -> "Sold out";
			case CLOSED -> "Closed";
		};
	}

}
