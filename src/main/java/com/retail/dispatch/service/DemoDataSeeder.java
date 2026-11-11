package com.retail.dispatch.service;

import com.retail.dispatch.repository.BookingRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Fills every block on the last day of the horizon in La Serena - Coquimbo, so a fresh install shows a sold-out
 * day like the reference screen. Bookings go through {@link BookingService}, so booked_count always matches
 * real bookings.
 */
@Component
@ConditionalOnProperty(name = "dispatch.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder {

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

	static final String DEMO_COMMUNE = "la-serena";

	private final BookingRepository bookingRepository;

	private final BookingService bookingService;

	private final AvailabilityService availability;

	private final Horizon horizon;

	public DemoDataSeeder(BookingRepository bookingRepository, BookingService bookingService,
			AvailabilityService availability, Horizon horizon) {
		this.bookingRepository = bookingRepository;
		this.bookingService = bookingService;
		this.availability = availability;
		this.horizon = horizon;
	}

	// Runs after WindowSeeder (order 1) has created the windows, and only on an empty database so restarts don't
	// keep adding demo bookings.
	@EventListener(ApplicationReadyEvent.class)
	@Order(2)
	public void seed() {
		if (bookingRepository.count() > 0) {
			return;
		}
		LocalDate lastDay = horizon.dates().getLast();
		int booked = 0;
		for (BlockOption block : availability.blocks(DEMO_COMMUNE, lastDay).blocks()) {
			for (int seat = 1;; seat++) {
				try {
					bookingService.book(new BookCommand("DEMO-" + lastDay + "-" + block.start() + "-" + seat, "Demo",
							String.valueOf(seat), DEMO_COMMUNE, lastDay, block.timeBlockId()));
					booked++;
				}
				catch (WindowSoldOutException soldOut) {
					break;
				}
			}
		}
		log.info("Demo data: {} bookings fill every block in {} on {}", booked, DEMO_COMMUNE, lastDay);
	}

}
