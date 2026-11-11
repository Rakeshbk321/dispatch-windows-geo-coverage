package com.retail.dispatch.api;

import com.retail.dispatch.service.AddressNotCoveredException;
import com.retail.dispatch.service.BookingNotFoundException;
import com.retail.dispatch.service.OrderAlreadyBookedException;
import com.retail.dispatch.service.UnknownCommuneException;
import com.retail.dispatch.service.WindowNotBookableException;
import com.retail.dispatch.service.WindowSoldOutException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into RFC 7807 application/problem+json with a stable {@code code} property clients can switch on.
 * Framework errors (bad JSON, missing parameter, wrong type...) go through {@link ResponseEntityExceptionHandler}.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	static final String VALIDATION_ERROR = "VALIDATION_ERROR";

	record InvalidField(String field, String message) {
	}

	@ExceptionHandler({ BookingNotFoundException.class, WindowSoldOutException.class,
			OrderAlreadyBookedException.class, AddressNotCoveredException.class, WindowNotBookableException.class,
			InvalidStatusTransitionException.class })
	ProblemDetail handleBookingRule(RuntimeException ex) {
		return switch (ex) {
			case BookingNotFoundException e -> problem(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Booking not found", e);
			case WindowSoldOutException e -> problem(HttpStatus.CONFLICT, "WINDOW_SOLD_OUT", "Window sold out", e);
			case OrderAlreadyBookedException e ->
				problem(HttpStatus.CONFLICT, "ORDER_ALREADY_BOOKED", "Order already booked", e);
			case AddressNotCoveredException e ->
				problem(HttpStatus.UNPROCESSABLE_CONTENT, "ADDRESS_NOT_COVERED", "Address not covered", e);
			case WindowNotBookableException e ->
				problem(HttpStatus.UNPROCESSABLE_CONTENT, "WINDOW_NOT_BOOKABLE", "Window not bookable", e);
			case InvalidStatusTransitionException e ->
				problem(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_STATUS_TRANSITION", "Invalid status transition", e);
			default -> throw new IllegalStateException("Unmapped exception", ex);
		};
	}

	// An unknown commune is bad input, the same as a malformed field, so it gets the validation shape.
	@ExceptionHandler(UnknownCommuneException.class)
	ProblemDetail handleUnknownCommune(UnknownCommuneException ex) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Invalid request", ex);
		problem.setProperty("errors", List.of(new InvalidField("commune", "unknown commune " + ex.getCommuneCode())));
		return problem;
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"Something went wrong on our side.");
		problem.setProperty("code", "INTERNAL_ERROR");
		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		// Most framework exceptions arrive with a null body and carry their own ProblemDetail; resolve it here so the
		// code and field errors can be added before it's written.
		if (body == null && ex instanceof ErrorResponse errorResponse) {
			body = errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
		}
		if (body instanceof ProblemDetail problem) {
			if (statusCode.value() == HttpStatus.BAD_REQUEST.value()) {
				problem.setProperty("code", VALIDATION_ERROR);
			}
			else {
				HttpStatus status = HttpStatus.resolve(statusCode.value());
				problem.setProperty("code", status != null ? status.name() : "ERROR");
			}
			if (ex instanceof MethodArgumentNotValidException invalid) {
				problem.setProperty("errors",
						invalid.getBindingResult()
							.getFieldErrors()
							.stream()
							.map(e -> new InvalidField(e.getField(), e.getDefaultMessage()))
							.toList());
			}
		}
		return super.handleExceptionInternal(ex, body, headers, statusCode, request);
	}

	private static ProblemDetail problem(HttpStatus status, String code, String title, RuntimeException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
		problem.setTitle(title);
		problem.setProperty("code", code);
		return problem;
	}

}
