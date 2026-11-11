package com.retail.dispatch.service;

import static com.retail.dispatch.FixedClockConfiguration.TODAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.retail.dispatch.FixedClockConfiguration;
import com.retail.dispatch.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
@SpringBootTest
class WindowSeederTests {

	@Autowired
	WindowSeeder seeder;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void startupCreatesAWindowPerZoneBlockAndDayWithTheZoneCapacity() {
		// 3 zones x 7 blocks x 7 days
		assertThat(countWindowsInHorizon()).isEqualTo(147);
		assertThat(jdbc.queryForList("""
				SELECT DISTINCT w.capacity = z.window_capacity FROM dispatch_window w JOIN zone z ON z.id = w.zone_id
				""", Boolean.class)).containsExactly(true);
	}

	@Test
	void runningAgainCreatesNothingNew() {
		seeder.ensureHorizon();
		seeder.ensureHorizon();

		assertThat(countWindowsInHorizon()).isEqualTo(147);
	}

	private int countWindowsInHorizon() {
		return jdbc.queryForObject("SELECT count(*) FROM dispatch_window WHERE delivery_date BETWEEN ? AND ?",
				Integer.class, TODAY, TODAY.plusDays(6));
	}

}
