package com.retail.dispatch.api;

import com.retail.dispatch.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/availability")
@Tag(name = "Availability", description = "Bookable dates and time blocks for an address")
class AvailabilityController {

	private final AvailabilityService availability;

	AvailabilityController(AvailabilityService availability) {
		this.availability = availability;
	}

	@GetMapping("/dates")
	@Operation(summary = "List the dates in the 7-day horizon for an address",
			description = "Each date is AVAILABLE, SOLD_OUT (every open block is full) or CLOSED (every block has "
					+ "started). An address that isn't covered returns `covered: false` and no dates.")
	@ApiResponse(responseCode = "200", description = "Dates for the address")
	@ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: missing or unknown commune",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	AvailableDatesResponse dates(
			@Parameter(description = "Commune code of the delivery address", example = "la-serena") @RequestParam String commune) {
		return AvailableDatesResponse.from(commune, availability.dates(commune));
	}

	@GetMapping("/dates/{date}")
	@Operation(summary = "List the time blocks for one date, with cost and remaining capacity",
			description = "Each block is AVAILABLE, SOLD_OUT or CLOSED (already started).")
	@ApiResponse(responseCode = "200", description = "Blocks for the date")
	@ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: missing or unknown commune, or malformed date",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "422", description = "WINDOW_NOT_BOOKABLE: date outside the 7-day horizon",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	TimeBlocksResponse blocks(
			@Parameter(description = "ISO date, e.g. 2030-03-05") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@Parameter(description = "Commune code of the delivery address", example = "la-serena") @RequestParam String commune) {
		return TimeBlocksResponse.from(commune, date, availability.blocks(commune, date));
	}

}
