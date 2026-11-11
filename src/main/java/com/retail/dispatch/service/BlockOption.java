package com.retail.dispatch.service;

import com.retail.dispatch.domain.AvailabilityStatus;
import java.math.BigDecimal;
import java.time.LocalTime;

public record BlockOption(long timeBlockId, LocalTime start, LocalTime end, BigDecimal cost, int remaining,
		AvailabilityStatus status) {
}
