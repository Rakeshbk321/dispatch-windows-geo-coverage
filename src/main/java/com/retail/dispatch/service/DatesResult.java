package com.retail.dispatch.service;

import java.util.List;

public record DatesResult(boolean covered, List<DateOption> dates) {

	static DatesResult notCovered() {
		return new DatesResult(false, List.of());
	}

}
