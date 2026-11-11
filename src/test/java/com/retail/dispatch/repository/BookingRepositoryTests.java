package com.retail.dispatch.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.retail.dispatch.TestcontainersConfiguration;
import com.retail.dispatch.domain.Booking;
import com.retail.dispatch.domain.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class BookingRepositoryTests {

	@Autowired
	BookingRepository bookings;

	@Autowired
	DispatchWindowRepository windows;

	@Autowired
	CommuneRepository communes;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void newBookingIsStoredConfirmedWithGeneratedIdAndPriceSnapshot() {
		long windowId = jdbc.queryForObject("""
				INSERT INTO dispatch_window (zone_id, delivery_date, time_block_id, capacity)
				SELECT z.id, DATE '2030-01-01', (SELECT min(id) FROM time_block), 5
				FROM zone z WHERE z.code = 'LS-CQ'
				RETURNING id
				""", Long.class);
		Instant createdAt = Instant.parse("2030-01-01T12:00:00Z");

		Booking saved = bookings.saveAndFlush(new Booking(windows.getReferenceById(windowId), "order-1", "Monjitas",
				"2334", communes.getReferenceById("la-serena"), new BigDecimal("2990"), createdAt));

		Map<String, Object> row = jdbc.queryForMap(
				"SELECT status, price::int AS price, created_at = ? AS created_matches FROM booking WHERE id = ?",
				java.sql.Timestamp.from(createdAt), saved.getId());
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
		assertThat(row).containsEntry("status", "CONFIRMED")
			.containsEntry("price", 2990)
			.containsEntry("created_matches", true);
	}

}
