package com.retail.dispatch.service;

import static com.retail.dispatch.FixedClockConfiguration.NOW;
import static com.retail.dispatch.FixedClockConfiguration.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.retail.dispatch.FixedClockConfiguration;
import com.retail.dispatch.TestcontainersConfiguration;
import com.retail.dispatch.domain.BookingStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

// Not @Transactional: these tests check what really got committed or rolled back.
@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
@SpringBootTest
class BookingServiceTests {

	static final LocalDate TOMORROW = TODAY.plusDays(1);

	@Autowired
	BookingService service;

	@Autowired
	JdbcTemplate jdbc;

	BookingFixtures fixtures;

	@BeforeEach
	void setUp() {
		fixtures = new BookingFixtures(jdbc);
	}

	@AfterEach
	void cleanUp() {
		fixtures.reset();
	}

	@Test
	void bookingTakesASeatAndSnapshotsTheBlockPrice() {
		BookingDetails booking = service.book(fixtures.command("order-1", "la-serena", TOMORROW, "18:00"));

		assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
		assertThat(booking.date()).isEqualTo(TOMORROW);
		assertThat(booking.start()).isEqualTo(LocalTime.of(18, 0));
		assertThat(booking.end()).isEqualTo(LocalTime.of(20, 0));
		assertThat(booking.price()).isEqualByComparingTo("2990");
		assertThat(booking.communeCode()).isEqualTo("la-serena");
		assertThat(booking.createdAt()).isEqualTo(NOW.toInstant());
		assertThat(fixtures.bookedCount("LS-CQ", TOMORROW, "18:00")).isEqualTo(1);
		assertThat(service.get(booking.id())).isEqualTo(booking);
	}

	@Test
	void bookingFailsWhenWindowIsSoldOutAndLeavesNoBookingBehind() {
		jdbc.update("UPDATE dispatch_window SET booked_count = capacity");

		assertThatThrownBy(() -> service.book(fixtures.command("order-1", "la-serena", TOMORROW, "18:00")))
			.isInstanceOf(WindowSoldOutException.class);
		assertThat(fixtures.bookingCount()).isZero();
		assertThat(fixtures.bookedCount("LS-CQ", TOMORROW, "18:00")).isEqualTo(5);
	}

	@Test
	void secondConfirmedBookingForTheSameOrderIsRejectedAndTakesNoSeat() {
		service.book(fixtures.command("order-1", "la-serena", TOMORROW, "18:00"));

		assertThatThrownBy(() -> service.book(fixtures.command("order-1", "la-serena", TOMORROW, "20:00")))
			.isInstanceOf(OrderAlreadyBookedException.class);
		assertThat(fixtures.bookingCount()).isEqualTo(1);
		assertThat(fixtures.bookedCount("LS-CQ", TOMORROW, "20:00")).isZero();
	}

	@Test
	void bookingIsRejectedForAnUncoveredOrUnknownCommune() {
		assertThatThrownBy(() -> service.book(fixtures.command("order-1", "vicuna", TOMORROW, "18:00")))
			.isInstanceOf(AddressNotCoveredException.class);
		assertThatThrownBy(() -> service.book(fixtures.command("order-1", "atlantis", TOMORROW, "18:00")))
			.isInstanceOf(UnknownCommuneException.class);
		assertThat(fixtures.bookingCount()).isZero();
	}

	@Test
	void bookingIsRejectedForABlockThatHasStartedOrADateOutsideTheHorizon() {
		assertThatThrownBy(() -> service.book(fixtures.command("order-1", "la-serena", TODAY, "16:00")))
			.isInstanceOf(WindowNotBookableException.class)
			.hasMessageContaining("already started");
		assertThatThrownBy(() -> service.book(fixtures.command("order-1", "la-serena", TODAY.plusDays(7), "18:00")))
			.isInstanceOf(WindowNotBookableException.class);
		assertThatThrownBy(() -> service.book(fixtures.command("order-1", "la-serena", TODAY.minusDays(1), "18:00")))
			.isInstanceOf(WindowNotBookableException.class);
	}

	@Test
	void sameDayBookingIsAllowedBeforeTheBlockStarts() {
		BookingDetails booking = service.book(fixtures.command("order-1", "la-serena", TODAY, "18:00"));

		assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
	}

	@Test
	void cancellingReleasesTheSeatOnlyOnce() {
		BookingDetails booking = service.book(fixtures.command("order-1", "la-serena", TOMORROW, "18:00"));

		BookingDetails cancelled = service.cancel(booking.id());
		BookingDetails cancelledAgain = service.cancel(booking.id());

		assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
		assertThat(cancelled.cancelledAt()).isEqualTo(NOW.toInstant());
		assertThat(cancelledAgain).isEqualTo(cancelled);
		assertThat(fixtures.bookedCount("LS-CQ", TOMORROW, "18:00")).isZero();
	}

	@Test
	void cancelledOrderCanBeBookedAgain() {
		BookingDetails first = service.book(fixtures.command("order-1", "la-serena", TOMORROW, "18:00"));
		service.cancel(first.id());

		BookingDetails second = service.book(fixtures.command("order-1", "la-serena", TOMORROW, "18:00"));

		assertThat(second.id()).isNotEqualTo(first.id());
		assertThat(fixtures.bookedCount("LS-CQ", TOMORROW, "18:00")).isEqualTo(1);
	}

	@Test
	void unknownBookingIsNotFound() {
		UUID missing = UUID.randomUUID();

		assertThatThrownBy(() -> service.get(missing)).isInstanceOf(BookingNotFoundException.class);
		assertThatThrownBy(() -> service.cancel(missing)).isInstanceOf(BookingNotFoundException.class);
	}

}
