package com.retail.dispatch.api;

import com.retail.dispatch.domain.BookingStatus;

class InvalidStatusTransitionException extends RuntimeException {

	InvalidStatusTransitionException(BookingStatus requested) {
		super("A booking can only be changed to CANCELLED, not " + requested);
	}

}
