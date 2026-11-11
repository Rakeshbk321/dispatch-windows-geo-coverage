package com.retail.dispatch.api;

import com.retail.dispatch.domain.BookingStatus;
import com.retail.dispatch.service.BookingDetails;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingResponse(UUID id, String orderId, Address address, LocalDate date, LocalTime start,
		LocalTime end, BigDecimal price, String currency, BookingStatus status, Instant createdAt,
		Instant cancelledAt) {

	static BookingResponse from(BookingDetails booking) {
		return new BookingResponse(booking.id(), booking.orderId(),
				new Address(booking.street(), booking.streetNumber(), booking.communeCode()), booking.date(),
				booking.start(), booking.end(), booking.price(), Money.CURRENCY, booking.status(),
				booking.createdAt(), booking.cancelledAt());
	}

}
