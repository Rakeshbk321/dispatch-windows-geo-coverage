package com.retail.dispatch.service;

import com.retail.dispatch.domain.Booking;
import com.retail.dispatch.domain.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingDetails(UUID id, String orderId, String street, String streetNumber, String communeCode,
		LocalDate date, LocalTime start, LocalTime end, BigDecimal price, BookingStatus status, Instant createdAt,
		Instant cancelledAt) {

	static BookingDetails of(Booking booking) {
		var window = booking.getWindow();
		var block = window.getTimeBlock();
		return new BookingDetails(booking.getId(), booking.getOrderId(), booking.getStreet(),
				booking.getStreetNumber(), booking.getCommune().getCode(), window.getDeliveryDate(),
				block.getStartTime(), block.getEndTime(), booking.getPrice(), booking.getStatus(),
				booking.getCreatedAt(), booking.getCancelledAt());
	}

}
