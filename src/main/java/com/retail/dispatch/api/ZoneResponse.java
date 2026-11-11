package com.retail.dispatch.api;

import java.util.List;

public record ZoneResponse(String code, String name, int windowCapacity, List<CommuneRef> communes) {

	public record CommuneRef(String code, String name) {
	}

}
