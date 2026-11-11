package com.retail.dispatch.api;

import com.retail.dispatch.domain.BookingStatus;
import com.retail.dispatch.service.BookingDetails;
import com.retail.dispatch.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Book and cancel dispatch windows")
class BookingController {

	private final BookingService bookings;

	BookingController(BookingService bookings) {
		this.bookings = bookings;
	}

	@PostMapping
	@Operation(summary = "Book a dispatch window",
			description = "Takes one seat in the window for the address's zone, date and time block. The price is "
					+ "copied from the time block. Concurrent requests for the last seat get exactly one 201.")
	@ApiResponse(responseCode = "201", description = "Booked",
			headers = @Header(name = "Location", description = "URL of the new booking"))
	@ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: invalid body or unknown commune",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "WINDOW_SOLD_OUT or ORDER_ALREADY_BOOKED",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "422",
			description = "ADDRESS_NOT_COVERED, or WINDOW_NOT_BOOKABLE (outside the horizon, block started, unknown block)",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
		BookingDetails booking = bookings.book(request.toCommand());
		URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
			.path("/{id}")
			.buildAndExpand(booking.id())
			.toUri();
		return ResponseEntity.created(location).body(BookingResponse.from(booking));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a booking", description = "Cancelled bookings are still returned, with status CANCELLED.")
	@ApiResponse(responseCode = "200", description = "The booking")
	@ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: id is not a UUID",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "BOOKING_NOT_FOUND",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	BookingResponse get(@PathVariable UUID id) {
		return BookingResponse.from(bookings.get(id));
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Cancel a booking", description = "Send `{\"status\": \"CANCELLED\"}`. The seat goes back to "
			+ "the window. Cancelling an already cancelled booking returns 200 and changes nothing.")
	@ApiResponse(responseCode = "200", description = "The cancelled booking")
	@ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: invalid body or id",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "BOOKING_NOT_FOUND",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "422", description = "INVALID_STATUS_TRANSITION: status other than CANCELLED",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	BookingResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateBookingRequest request) {
		if (request.status() != BookingStatus.CANCELLED) {
			throw new InvalidStatusTransitionException(request.status());
		}
		return BookingResponse.from(bookings.cancel(id));
	}

}
