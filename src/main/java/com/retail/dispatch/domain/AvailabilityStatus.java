package com.retail.dispatch.domain;

import java.time.LocalDateTime;
import java.util.Collection;

public enum AvailabilityStatus {

	AVAILABLE, SOLD_OUT, CLOSED;

	// Same-day booking is allowed, but only until the block starts.
	public static AvailabilityStatus ofBlock(LocalDateTime blockStart, int remaining, LocalDateTime now) {
		if (!now.isBefore(blockStart)) {
			return CLOSED;
		}
		return remaining > 0 ? AVAILABLE : SOLD_OUT;
	}

	// A day is sold out only when every block still open is full; if every block has started it's closed instead.
	public static AvailabilityStatus ofDate(Collection<AvailabilityStatus> blockStatuses) {
		if (blockStatuses.contains(AVAILABLE)) {
			return AVAILABLE;
		}
		return blockStatuses.contains(SOLD_OUT) ? SOLD_OUT : CLOSED;
	}

}
