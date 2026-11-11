package com.retail.dispatch.service;

import static com.retail.dispatch.FixedClockConfiguration.TODAY;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.retail.dispatch.FixedClockConfiguration;
import com.retail.dispatch.TestcontainersConfiguration;
import com.retail.dispatch.domain.BookingStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Real threads, real transactions, real Postgres row locks. Every test ends by checking the invariant that each
 * window's booked_count equals its confirmed bookings and never exceeds capacity.
 */
@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
@SpringBootTest
class BookingConcurrencyTests {

	static final LocalDate TOMORROW = TODAY.plusDays(1);

	static final String OK = "OK";

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
	void invariantHoldsThenCleanUp() {
		try {
			assertThat(fixtures.windowsWithCounterDrift()).isZero();
		}
		finally {
			fixtures.reset();
		}
	}

	@Test
	void fiftyConcurrentBookingsForFiveSeatsGiveExactlyFiveBookings() throws Exception {
		Map<String, Long> outcomes = race(50,
				i -> () -> service.book(fixtures.command("order-" + i, "la-serena", TOMORROW, "18:00")));

		assertThat(outcomes).containsOnly(entry(OK, 5L), entry("WindowSoldOutException", 45L));
		assertThat(fixtures.bookedCount("LS-CQ", TOMORROW, "18:00")).isEqualTo(5);
		assertThat(fixtures.bookingCount()).isEqualTo(5);
	}

	@Test
	void sameOrderBookedFromTenThreadsAtOnceGetsOneBooking() throws Exception {
		Map<String, Long> outcomes = race(10,
				i -> () -> service.book(fixtures.command("order-1", "la-serena", TOMORROW, "18:00")));

		assertThat(outcomes).containsOnly(entry(OK, 1L), entry("OrderAlreadyBookedException", 9L));
		assertThat(fixtures.bookedCount("LS-CQ", TOMORROW, "18:00")).isEqualTo(1);
	}

	@Test
	void twentyConcurrentCancelsOfOneBookingReleaseItsSeatOnce() throws Exception {
		UUID target = service.book(fixtures.command("order-1", "la-serena", TOMORROW, "18:00")).id();
		service.book(fixtures.command("order-2", "la-serena", TOMORROW, "18:00"));

		Map<String, Long> outcomes = race(20, i -> () -> service.cancel(target));

		assertThat(outcomes).containsOnly(entry(OK, 20L));
		assertThat(service.get(target).status()).isEqualTo(BookingStatus.CANCELLED);
		// A double release would show as 0 here, or as a CHECK violation if it went below zero.
		assertThat(fixtures.bookedCount("LS-CQ", TOMORROW, "18:00")).isEqualTo(1);
	}

	@Test
	void mixedBookingsAndCancelsOnOneWindowNeverOverbookOrDrift() throws Exception {
		List<UUID> existing = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			existing.add(service.book(fixtures.command("existing-" + i, "la-serena", TOMORROW, "18:00")).id());
		}

		// 20 new orders compete for the 2 free seats while the 3 existing bookings are cancelled.
		Map<String, Long> outcomes = race(23, i -> i < 3 ? () -> service.cancel(existing.get(i))
				: () -> service.book(fixtures.command("new-" + i, "la-serena", TOMORROW, "18:00")));

		long newBookings = outcomes.getOrDefault(OK, 0L) - 3;
		assertThat(outcomes.keySet()).isSubsetOf(OK, "WindowSoldOutException");
		assertThat(newBookings).isBetween(2L, 5L);
		assertThat(fixtures.bookedCount("LS-CQ", TOMORROW, "18:00")).isEqualTo((int) newBookings)
			.isEqualTo(fixtures.confirmedBookingCount())
			.isLessThanOrEqualTo(5);
	}

	@Test
	void cancellingAndRebookingTheSameOrdersAtOnceNeverDeadlocks() throws Exception {
		// Book and cancel both lock the booking row before the window row. If book locked the window first, this race
		// would deadlock: book holds the window and waits on cancel's booking row (unique index on order_id), while
		// cancel holds that booking row and waits on the window. The gap is a few milliseconds, so the test runs
		// 10 cancel/rebook pairs per round for 20 rounds to make the overlap near certain if the order were wrong.
		int pairs = 10;
		for (int round = 0; round < 20; round++) {
			fixtures.setCapacity("LS-CQ", TOMORROW, "18:00", 2 * pairs);
			List<UUID> originals = new ArrayList<>();
			for (int i = 0; i < pairs; i++) {
				originals.add(service.book(fixtures.command("order-" + i, "la-serena", TOMORROW, "18:00")).id());
			}

			Map<String, Long> outcomes = race(2 * pairs, i -> i < pairs ? () -> service.cancel(originals.get(i))
					: () -> service.book(fixtures.command("order-" + (i - pairs), "la-serena", TOMORROW, "18:00")));

			assertThat(outcomes.keySet()).as("round %d", round).isSubsetOf(OK, "OrderAlreadyBookedException");
			assertThat(fixtures.windowsWithCounterDrift()).as("round %d", round).isZero();
			fixtures.reset();
		}
	}

	/**
	 * Starts all tasks at the same instant and tallies the outcomes: "OK" or the simple name of the exception thrown.
	 */
	private Map<String, Long> race(int threads, IntFunction<Runnable> task) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			CountDownLatch ready = new CountDownLatch(threads);
			CountDownLatch start = new CountDownLatch(1);
			List<Future<String>> results = new ArrayList<>();
			for (int i = 0; i < threads; i++) {
				Runnable work = task.apply(i);
				results.add(pool.submit(() -> {
					ready.countDown();
					start.await();
					try {
						work.run();
						return OK;
					}
					catch (RuntimeException ex) {
						return ex.getClass().getSimpleName();
					}
				}));
			}
			ready.await();
			start.countDown();
			List<String> outcomes = new ArrayList<>();
			for (Future<String> result : results) {
				outcomes.add(result.get(30, TimeUnit.SECONDS));
			}
			return outcomes.stream().collect(groupingBy(o -> o, counting()));
		}
		finally {
			pool.shutdownNow();
		}
	}

}
