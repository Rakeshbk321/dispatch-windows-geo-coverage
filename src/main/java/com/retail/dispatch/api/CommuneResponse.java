package com.retail.dispatch.api;

import com.retail.dispatch.domain.Commune;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommuneResponse(String code, String name, String region,
		@Schema(description = "Zone code, or null when the commune is not covered", nullable = true) String zone) {

	static CommuneResponse from(Commune commune) {
		return new CommuneResponse(commune.getCode(), commune.getName(), commune.getRegion(),
				commune.getZone() == null ? null : commune.getZone().getCode());
	}

}
