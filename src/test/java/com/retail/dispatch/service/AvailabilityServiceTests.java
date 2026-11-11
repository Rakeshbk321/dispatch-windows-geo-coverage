package com.retail.dispatch.service;

import static com.retail.dispatch.FixedClockConfiguration.TODAY;
import static com.retail.dispatch.domain.AvailabilityStatus.AVAILABLE;
import static com.retail.dispatch.domain.AvailabilityStatus.CLOSED;
import static com.retail.dispatch.domain.AvailabilityStatus.SOLD_OUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.retail.dispatch.FixedClockConfiguration;
import com.retail.dispatch.TestcontainersConfiguration;
import com.retail.dispatch.domain.AvailabilityStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
@SpringBootTest
@Transactional
class AvailabilityServiceTests {

	static final String[] ALL_BLOCKS = { "08:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:00" };

	@Autowired
	AvailabilityService availability;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void listsEveryHorizonDateForACoveredCommune() {
		DatesResult result = availability.dates("la-serena");

		assertThat(result.covered()).isTrue();
		assertThat(result.dates()).extracting(DateOption::date)
			.containsExactlyElementsOf(TODAY.datesUntil(TODAY.plusDays(7)).toList());
		assertThat(result.dates()).extracting(DateOption::status).containsOnly(AVAILABLE);
	}

	@Test
	void dateIsSoldOutOnlyForTheZoneWhoseBlocksAreAllFull() {
		LocalDate friday = TODAY.plusDays(4);
		for (String start : ALL_BLOCKS) {
			setBooked("LS-CQ", friday, start, 5);
		}

		assertThat(statusOf(availability.dates("la-serena"), friday)).isEqualTo(SOLD_OUT);
		assertThat(statusOf(availability.dates("coquimbo"), friday)).isEqualTo(SOLD_OUT);
		assertThat(statusOf(availability.dates("providencia"), friday)).isEqualTo(AVAILABLE);
	}

	@Test
	void todayIsSoldOutWhenItsOpenBlocksAreFullEvenThoughOthersHaveStarted() {
		// At 16:30 only the 18:00 and 20:00 blocks are still open today.
		for (String start : new String[] { "18:00", "20:00" }) {
			setBooked("LS-CQ", TODAY, start, 5);
		}

		assertThat(statusOf(availability.dates("la-serena"), TODAY)).isEqualTo(SOLD_OUT);
	}

	@Test
	void blocksShowCostRemainingCapacityAndStatus() {
		setBooked("LS-CQ", TODAY, "18:00", 5);
		setBooked("LS-CQ", TODAY, "20:00", 2);

		BlocksResult result = availability.blocks("la-serena", TODAY);

		assertThat(result.covered()).isTrue();
		assertThat(result.blocks())
			.extracting(BlockOption::start, BlockOption::end, b -> b.cost().intValue(), BlockOption::remaining,
					BlockOption::status)
			.containsExactly(
					tuple(LocalTime.of(8, 0), LocalTime.of(10, 0), 2990, 5, CLOSED),
					tuple(LocalTime.of(10, 0), LocalTime.of(12, 0), 2990, 5, CLOSED),
					tuple(LocalTime.of(12, 0), LocalTime.of(14, 0), 2990, 5, CLOSED),
					tuple(LocalTime.of(14, 0), LocalTime.of(16, 0), 2990, 5, CLOSED),
					tuple(LocalTime.of(16, 0), LocalTime.of(18, 0), 2990, 5, CLOSED),
					tuple(LocalTime.of(18, 0), LocalTime.of(20, 0), 2990, 0, SOLD_OUT),
					tuple(LocalTime.of(20, 0), LocalTime.of(22, 0), 2990, 3, AVAILABLE));
	}

	@Test
	void uncoveredCommuneGetsNoDatesOrBlocks() {
		assertThat(availability.dates("vicuna")).isEqualTo(new DatesResult(false, List.of()));
		assertThat(availability.blocks("vicuna", TODAY)).isEqualTo(new BlocksResult(false, List.of()));
	}

	@Test
	void unknownCommuneIsRejected() {
		assertThatThrownBy(() -> availability.dates("atlantis")).isInstanceOf(UnknownCommuneException.class);
		assertThatThrownBy(() -> availability.blocks("atlantis", TODAY)).isInstanceOf(UnknownCommuneException.class);
	}

	@Test
	void blocksOutsideTheHorizonAreRejected() {
		assertThatThrownBy(() -> availability.blocks("la-serena", TODAY.minusDays(1)))
			.isInstanceOf(DateOutsideHorizonException.class);
		assertThatThrownBy(() -> availability.blocks("la-serena", TODAY.plusDays(7)))
			.isInstanceOf(DateOutsideHorizonException.class);
	}

	private AvailabilityStatus statusOf(DatesResult result, LocalDate date) {
		return result.dates().stream().filter(d -> d.date().equals(date)).findFirst().orElseThrow().status();
	}

	private void setBooked(String zoneCode, LocalDate date, String blockStart, int bookedCount) {
		int updated = jdbc.update("""
				UPDATE dispatch_window w SET booked_count = ?
				FROM zone z, time_block tb
				WHERE z.id = w.zone_id AND tb.id = w.time_block_id
				  AND z.code = ? AND w.delivery_date = ? AND tb.start_time = ?::time
				""", bookedCount, zoneCode, date, blockStart);
		assertThat(updated).isEqualTo(1);
	}

}
