package com.retail.dispatch.service;

import com.retail.dispatch.domain.AvailabilityStatus;
import com.retail.dispatch.domain.Booking;
import com.retail.dispatch.domain.DispatchWindow;
import com.retail.dispatch.domain.Zone;
import com.retail.dispatch.repository.BookingRepository;
import com.retail.dispatch.repository.CommuneRepository;
import com.retail.dispatch.repository.DispatchWindowRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

	private static final String ONE_CONFIRMED_BOOKING_PER_ORDER = "uq_booking_confirmed_order";

	private final CoverageService coverage;

	private final Horizon horizon;

	private final DispatchWindowRepository windows;

	private final BookingRepository bookings;

	private final CommuneRepository communes;

	private final Clock clock;

	public BookingService(CoverageService coverage, Horizon horizon, DispatchWindowRepository windows,
			BookingRepository bookings, CommuneRepository communes, Clock clock) {
		this.coverage = coverage;
		this.horizon = horizon;
		this.windows = windows;
		this.bookings = bookings;
		this.communes = communes;
		this.clock = clock;
	}

	/**
	 * Books the window for the address's zone, date and time block, taking one seat.
	 * @throws UnknownCommuneException if the commune code doesn't exist
	 * @throws AddressNotCoveredException if no zone delivers to the commune
	 * @throws WindowNotBookableException if the date is outside the horizon, the block has started, or no such block
	 * exists
	 * @throws OrderAlreadyBookedException if the order already has a confirmed booking
	 * @throws WindowSoldOutException if the window has no seats left
	 */
	@Transactional
	public BookingDetails book(BookCommand command) {
		Zone zone = coverage.resolveZone(command.communeCode())
			.orElseThrow(() -> new AddressNotCoveredException(command.communeCode()));
		if (!horizon.contains(command.date())) {
			throw new DateOutsideHorizonException(command.date());
		}
		DispatchWindow window = windows.findWithBlock(zone, command.date(), command.timeBlockId())
			.orElseThrow(() -> new WindowNotBookableException(
					"No time block " + command.timeBlockId() + " on " + command.date()));
		// Only the "already started" rule is decided from this read. Capacity is decided by tryReserve below, since
		// booked_count read here may already be stale.
		if (window.statusAt(LocalDateTime.now(clock)) == AvailabilityStatus.CLOSED) {
			throw new WindowNotBookableException("The " + window.getTimeBlock().getStartTime() + " block on "
					+ command.date() + " has already started");
		}

		// Booking row first, window row second, the same order as cancel(), so cancelling and re-booking the same
		// order at the same moment can't deadlock.
		Booking booking = insert(new Booking(window, command.orderId(), command.street(), command.streetNumber(),
				communes.getReferenceById(command.communeCode()), window.getTimeBlock().getCost(), clock.instant()));
		if (windows.tryReserve(window.getId()) == 0) {
			// Throwing rolls back the transaction, which removes the booking row inserted above.
			throw new WindowSoldOutException(command.date(), window.getTimeBlock().getStartTime());
		}
		return BookingDetails.of(booking);
	}

	/**
	 * Cancels a booking and gives its seat back. Cancelling an already cancelled booking changes nothing.
	 * @throws BookingNotFoundException if the booking doesn't exist
	 */
	@Transactional
	public BookingDetails cancel(UUID id) {
		if (bookings.cancelIfConfirmed(id, clock.instant()) == 1) {
			windows.releaseSeatOf(id);
		}
		return find(id);
	}

	/**
	 * @throws BookingNotFoundException if the booking doesn't exist
	 */
	@Transactional(readOnly = true)
	public BookingDetails get(UUID id) {
		return find(id);
	}

	private BookingDetails find(UUID id) {
		return bookings.findDetailedById(id).map(BookingDetails::of).orElseThrow(() -> new BookingNotFoundException(id));
	}

	private Booking insert(Booking booking) {
		try {
			// Flush now so the INSERT runs before the window UPDATE, and a duplicate order fails here.
			return bookings.saveAndFlush(booking);
		}
		catch (DataIntegrityViolationException ex) {
			if (violates(ex, ONE_CONFIRMED_BOOKING_PER_ORDER)) {
				throw new OrderAlreadyBookedException(booking.getOrderId());
			}
			throw ex;
		}
	}

	private static boolean violates(Throwable ex, String constraint) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation
					&& constraint.equalsIgnoreCase(violation.getConstraintName())) {
				return true;
			}
		}
		return false;
	}

}
