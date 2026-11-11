package com.retail.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class SchemaMigrationTests {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void seedsZonesWithTheirCommunes() {
		assertThat(zoneOf("la-serena")).isEqualTo("LS-CQ");
		assertThat(zoneOf("coquimbo")).isEqualTo("LS-CQ");
		assertThat(zoneOf("providencia")).isEqualTo("SCL-CO");
		assertThat(zoneOf("vina-del-mar")).isEqualTo("VAL-VDM");
		assertThat(jdbc.queryForObject("SELECT count(*) FROM commune WHERE zone_id IS NULL", Integer.class))
				.isEqualTo(5);
	}

	@Test
	void seedsSevenBackToBackTwoHourBlocksAtTheSameCost() {
		assertThat(jdbc.queryForList("SELECT start_time::text FROM time_block ORDER BY start_time", String.class))
				.containsExactly("08:00:00", "10:00:00", "12:00:00", "14:00:00", "16:00:00", "18:00:00", "20:00:00");
		assertThat(jdbc.queryForObject("SELECT count(*) FROM time_block WHERE end_time - start_time <> interval '2 hours'",
				Integer.class))
			.isZero();
		assertThat(jdbc.queryForList("SELECT DISTINCT cost::int FROM time_block", Integer.class))
				.containsExactly(2990);
	}

	@Test
	void databaseRejectsBookedCountAboveCapacity() {
		long windowId = insertWindow(1);

		assertThatThrownBy(() -> jdbc.update("UPDATE dispatch_window SET booked_count = 2 WHERE id = ?", windowId))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_window_booked_within_capacity");
	}

	@Test
	void orderCanBeBookedAgainOnlyAfterCancelling() {
		long windowId = insertWindow(5);
		insertBooking(windowId, "order-1", "CANCELLED");
		insertBooking(windowId, "order-1", "CONFIRMED");

		assertThatThrownBy(() -> insertBooking(windowId, "order-1", "CONFIRMED"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("uq_booking_confirmed_order");
	}

	private String zoneOf(String commune) {
		return jdbc.queryForObject(
				"SELECT z.code FROM commune c JOIN zone z ON z.id = c.zone_id WHERE c.code = ?", String.class, commune);
	}

	private long insertWindow(int capacity) {
		return jdbc.queryForObject("""
				INSERT INTO dispatch_window (zone_id, delivery_date, time_block_id, capacity)
				SELECT z.id, DATE '2030-01-01', (SELECT min(id) FROM time_block), ?
				FROM zone z WHERE z.code = 'LS-CQ'
				RETURNING id
				""", Long.class, capacity);
	}

	private void insertBooking(long windowId, String orderId, String status) {
		jdbc.update("""
				INSERT INTO booking (id, window_id, order_id, street, street_number, commune_code, price, status, created_at)
				VALUES (?, ?, ?, 'Monjitas', '2334', 'la-serena', 2990, ?, now())
				""", UUID.randomUUID(), windowId, orderId, status);
	}

}
