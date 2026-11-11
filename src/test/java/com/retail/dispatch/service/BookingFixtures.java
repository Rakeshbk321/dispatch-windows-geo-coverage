package com.retail.dispatch.service;

import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;

class BookingFixtures {

	private final JdbcTemplate jdbc;

	BookingFixtures(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	BookCommand command(String orderId, String commune, LocalDate date, String blockStart) {
		long blockId = jdbc.queryForObject("SELECT id FROM time_block WHERE start_time = ?::time", Long.class,
				blockStart);
		return new BookCommand(orderId, "Monjitas", "2334", commune, date, blockId);
	}

	int bookedCount(String zoneCode, LocalDate date, String blockStart) {
		return jdbc.queryForObject("""
				SELECT w.booked_count FROM dispatch_window w
				JOIN zone z ON z.id = w.zone_id JOIN time_block tb ON tb.id = w.time_block_id
				WHERE z.code = ? AND w.delivery_date = ? AND tb.start_time = ?::time
				""", Integer.class, zoneCode, date, blockStart);
	}

	int bookingCount() {
		return jdbc.queryForObject("SELECT count(*) FROM booking", Integer.class);
	}

	int confirmedBookingCount() {
		return jdbc.queryForObject("SELECT count(*) FROM booking WHERE status = 'CONFIRMED'", Integer.class);
	}

	/** Windows whose booked_count doesn't match their confirmed bookings. Should always be zero. */
	int windowsWithCounterDrift() {
		return jdbc.queryForObject("""
				SELECT count(*) FROM dispatch_window w
				WHERE w.booked_count <> (SELECT count(*) FROM booking b WHERE b.window_id = w.id AND b.status = 'CONFIRMED')
				""", Integer.class);
	}

	void setCapacity(String zoneCode, LocalDate date, String blockStart, int capacity) {
		jdbc.update("""
				UPDATE dispatch_window w SET capacity = ?
				FROM zone z, time_block tb
				WHERE z.id = w.zone_id AND tb.id = w.time_block_id
				  AND z.code = ? AND w.delivery_date = ? AND tb.start_time = ?::time
				""", capacity, zoneCode, date, blockStart);
	}

	void reset() {
		jdbc.update("DELETE FROM booking");
		jdbc.update("""
				UPDATE dispatch_window w SET booked_count = 0, capacity = z.window_capacity
				FROM zone z WHERE z.id = w.zone_id
				""");
	}

}
