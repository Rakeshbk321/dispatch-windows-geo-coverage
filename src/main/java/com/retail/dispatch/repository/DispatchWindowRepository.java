package com.retail.dispatch.repository;

import com.retail.dispatch.domain.DispatchWindow;
import com.retail.dispatch.domain.Zone;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface DispatchWindowRepository extends JpaRepository<DispatchWindow, Long> {

	/**
	 * Creates a window for every zone and time block on every date in the range, skipping any that already exist.
	 * ON CONFLICT on the unique key makes this idempotent, so restarts and several instances running it at once are safe.
	 * @return the number of windows created
	 */
	@Transactional
	@Modifying
	@Query(nativeQuery = true, value = """
			INSERT INTO dispatch_window (zone_id, delivery_date, time_block_id, capacity)
			SELECT z.id, CAST(:firstDate AS date) + d.n, tb.id, z.window_capacity
			FROM zone z
			CROSS JOIN time_block tb
			CROSS JOIN generate_series(0, CAST(:lastDate AS date) - CAST(:firstDate AS date)) AS d(n)
			ON CONFLICT (zone_id, delivery_date, time_block_id) DO NOTHING
			""")
	int insertHorizon(LocalDate firstDate, LocalDate lastDate);

	@Query("""
			SELECT w FROM DispatchWindow w JOIN FETCH w.timeBlock tb
			WHERE w.zone = :zone AND w.deliveryDate BETWEEN :firstDate AND :lastDate
			ORDER BY w.deliveryDate, tb.startTime
			""")
	List<DispatchWindow> findWithBlockByZoneAndDateBetween(Zone zone, LocalDate firstDate, LocalDate lastDate);

	@Query("""
			SELECT w FROM DispatchWindow w JOIN FETCH w.timeBlock tb
			WHERE w.zone = :zone AND w.deliveryDate = :date AND tb.id = :timeBlockId
			""")
	Optional<DispatchWindow> findWithBlock(Zone zone, LocalDate date, long timeBlockId);

	/**
	 * Takes one seat if any is left. Check and increment are a single statement: Postgres locks the row, so concurrent
	 * bookings queue here and each re-evaluates {@code booked_count < capacity} against the committed value.
	 * @return 1 if a seat was taken, 0 if the window is full
	 */
	@Modifying
	@Query(nativeQuery = true, value = """
			UPDATE dispatch_window SET booked_count = booked_count + 1
			WHERE id = :windowId AND booked_count < capacity
			""")
	int tryReserve(long windowId);

	@Modifying
	@Query(nativeQuery = true, value = """
			UPDATE dispatch_window SET booked_count = booked_count - 1
			WHERE id = (SELECT window_id FROM booking WHERE id = :bookingId)
			""")
	int releaseSeatOf(UUID bookingId);

}
