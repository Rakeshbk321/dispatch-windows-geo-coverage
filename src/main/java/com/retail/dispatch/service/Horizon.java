package com.retail.dispatch.service;

import com.retail.dispatch.config.DispatchProperties;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The bookable dates: today plus the following days, {@code dispatch.horizon-days} in total.
 */
@Component
public class Horizon {

	private final Clock clock;

	private final int days;

	public Horizon(Clock clock, DispatchProperties properties) {
		this.clock = clock;
		this.days = properties.horizonDays();
	}

	public List<LocalDate> dates() {
		LocalDate today = LocalDate.now(clock);
		return today.datesUntil(today.plusDays(days)).toList();
	}

	public boolean contains(LocalDate date) {
		LocalDate today = LocalDate.now(clock);
		return !date.isBefore(today) && date.isBefore(today.plusDays(days));
	}

}
