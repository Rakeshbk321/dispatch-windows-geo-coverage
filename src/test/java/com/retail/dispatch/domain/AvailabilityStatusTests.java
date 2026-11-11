package com.retail.dispatch.domain;

import static com.retail.dispatch.domain.AvailabilityStatus.AVAILABLE;
import static com.retail.dispatch.domain.AvailabilityStatus.CLOSED;
import static com.retail.dispatch.domain.AvailabilityStatus.SOLD_OUT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AvailabilityStatusTests {

	static final LocalDateTime START = LocalDateTime.of(2030, 3, 4, 17, 0);

	@Test
	void blockIsAvailableBeforeItStartsWhileCapacityRemains() {
		assertThat(AvailabilityStatus.ofBlock(START, 1, START.minusMinutes(1))).isEqualTo(AVAILABLE);
	}

	@Test
	void blockIsSoldOutWhenNoCapacityRemains() {
		assertThat(AvailabilityStatus.ofBlock(START, 0, START.minusHours(3))).isEqualTo(SOLD_OUT);
	}

	@Test
	void blockIsClosedFromTheMomentItStarts() {
		assertThat(AvailabilityStatus.ofBlock(START, 5, START)).isEqualTo(CLOSED);
		assertThat(AvailabilityStatus.ofBlock(START, 0, START.plusDays(1))).isEqualTo(CLOSED);
	}

	@Test
	void dateIsAvailableIfAnyBlockIs() {
		assertThat(AvailabilityStatus.ofDate(List.of(CLOSED, SOLD_OUT, AVAILABLE))).isEqualTo(AVAILABLE);
	}

	@Test
	void dateIsSoldOutWhenEveryOpenBlockIsFull() {
		assertThat(AvailabilityStatus.ofDate(List.of(CLOSED, SOLD_OUT, SOLD_OUT))).isEqualTo(SOLD_OUT);
	}

	@Test
	void dateIsClosedWhenEveryBlockHasStartedOrThereAreNone() {
		assertThat(AvailabilityStatus.ofDate(List.of(CLOSED, CLOSED))).isEqualTo(CLOSED);
		assertThat(AvailabilityStatus.ofDate(List.of())).isEqualTo(CLOSED);
	}

}
