package com.retail.dispatch.api;

import com.retail.dispatch.domain.AvailabilityStatus;
import com.retail.dispatch.service.DatesResult;
import java.time.LocalDate;
import java.util.List;

public record AvailableDatesResponse(String commune, boolean covered, List<DateItem> dates) {

	public record DateItem(LocalDate date, AvailabilityStatus status) {
	}

	static AvailableDatesResponse from(String commune, DatesResult result) {
		return new AvailableDatesResponse(commune, result.covered(),
				result.dates().stream().map(d -> new DateItem(d.date(), d.status())).toList());
	}

}
