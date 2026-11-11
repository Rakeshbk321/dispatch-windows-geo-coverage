package com.retail.dispatch.service;

import static com.retail.dispatch.FixedClockConfiguration.TODAY;
import static com.retail.dispatch.domain.AvailabilityStatus.AVAILABLE;
import static com.retail.dispatch.domain.AvailabilityStatus.SOLD_OUT;
import static org.assertj.core.api.Assertions.assertThat;

import com.retail.dispatch.FixedClockConfiguration;
import com.retail.dispatch.TestcontainersConfiguration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
@SpringBootTest(properties = "dispatch.demo-data.enabled=true")
class DemoDataSeederTests {

	static final LocalDate LAST_DAY = TODAY.plusDays(6);

	@Autowired
	DemoDataSeeder seeder;

	@Autowired
	AvailabilityService availability;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void startupSellsOutEveryBlockOnTheLastDayInLaSerenaOnly() {
		BookingFixtures fixtures = new BookingFixtures(jdbc);
		for (String start : new String[] { "08:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:00" }) {
			assertThat(fixtures.bookedCount("LS-CQ", LAST_DAY, start)).isEqualTo(5);
			assertThat(fixtures.bookedCount("SCL-CO", LAST_DAY, start)).isZero();
		}
		// 7 blocks x 5 seats
		assertThat(fixtures.confirmedBookingCount()).isEqualTo(35);
		assertThat(fixtures.windowsWithCounterDrift()).isZero();

		assertThat(availability.dates("la-serena").dates()).filteredOn(d -> d.date().equals(LAST_DAY))
			.extracting(DateOption::status)
			.containsExactly(SOLD_OUT);
		assertThat(availability.dates("la-serena").dates()).filteredOn(d -> !d.date().equals(LAST_DAY))
			.extracting(DateOption::status)
			.containsOnly(AVAILABLE);
		assertThat(availability.dates("providencia").dates()).extracting(DateOption::status).containsOnly(AVAILABLE);
	}

	@Test
	void seedsNothingWhenBookingsAlreadyExist() {
		seeder.seed();

		assertThat(new BookingFixtures(jdbc).bookingCount()).isEqualTo(35);
	}

}
