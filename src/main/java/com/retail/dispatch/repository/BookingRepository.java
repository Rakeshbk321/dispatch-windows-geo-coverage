package com.retail.dispatch.repository;

import com.retail.dispatch.domain.Booking;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

	@Query("""
			SELECT b FROM Booking b JOIN FETCH b.window w JOIN FETCH w.timeBlock JOIN FETCH b.commune
			WHERE b.id = :id
			""")
	Optional<Booking> findDetailedById(UUID id);

	/**
	 * Cancels only a CONFIRMED booking. A concurrent second cancel waits on the row lock, re-checks the status after
	 * the first commits and changes nothing, so the caller can release the seat exactly once.
	 * @return 1 if this call cancelled the booking, 0 if it was already cancelled or doesn't exist
	 */
	@Modifying
	@Query(nativeQuery = true, value = """
			UPDATE booking SET status = 'CANCELLED', cancelled_at = :now
			WHERE id = :id AND status = 'CONFIRMED'
			""")
	int cancelIfConfirmed(UUID id, Instant now);

}
