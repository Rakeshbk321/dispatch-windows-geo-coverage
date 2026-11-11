package com.retail.dispatch.api;

import com.retail.dispatch.domain.AvailabilityStatus;
import com.retail.dispatch.service.BlocksResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TimeBlocksResponse(String commune, LocalDate date, boolean covered, List<BlockItem> blocks) {

	public record BlockItem(long timeBlockId, LocalTime start, LocalTime end, BigDecimal cost, String currency,
			int remaining, AvailabilityStatus status) {
	}

	static TimeBlocksResponse from(String commune, LocalDate date, BlocksResult result) {
		return new TimeBlocksResponse(commune, date, result.covered(),
				result.blocks()
					.stream()
					.map(b -> new BlockItem(b.timeBlockId(), b.start(), b.end(), b.cost(), Money.CURRENCY,
							b.remaining(), b.status()))
					.toList());
	}

}
