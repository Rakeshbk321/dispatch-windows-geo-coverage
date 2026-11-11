package com.retail.dispatch.api;

import com.retail.dispatch.service.BookCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateBookingRequest(
		@Schema(example = "ORD-1001") @NotBlank @Size(max = 64) String orderId,
		@NotNull @Valid Address address,
		@Schema(description = "Delivery date, within the 7-day horizon") @NotNull LocalDate date,
		@Schema(description = "From GET /api/v1/availability/dates/{date}", example = "2") @NotNull Long timeBlockId) {

	BookCommand toCommand() {
		return new BookCommand(orderId, address.street(), address.number(), address.commune(), date, timeBlockId);
	}

}
