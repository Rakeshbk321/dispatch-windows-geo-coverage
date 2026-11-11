package com.retail.dispatch.api;

import com.retail.dispatch.domain.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateBookingRequest(
		@Schema(description = "Only CANCELLED is accepted", example = "CANCELLED") @NotNull BookingStatus status) {
}
