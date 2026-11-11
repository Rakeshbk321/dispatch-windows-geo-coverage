package com.retail.dispatch.api;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.retail.dispatch.api.ZoneResponse.CommuneRef;
import com.retail.dispatch.domain.Commune;
import com.retail.dispatch.service.CoverageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Coverage", description = "Geographic zones and the communes they deliver to")
class ZoneController {

	private final CoverageService coverage;

	ZoneController(CoverageService coverage) {
		this.coverage = coverage;
	}

	@GetMapping("/zones")
	@Operation(summary = "List zones with their communes and window capacity")
	List<ZoneResponse> zones() {
		Map<String, List<CommuneRef>> communesByZone = coverage.listCommunes()
			.stream()
			.filter(c -> c.getZone() != null)
			.collect(groupingBy(c -> c.getZone().getCode(), mapping(ZoneController::ref, toList())));
		return coverage.listZones()
			.stream()
			.map(z -> new ZoneResponse(z.getCode(), z.getName(), z.getWindowCapacity(),
					communesByZone.getOrDefault(z.getCode(), List.of())))
			.toList();
	}

	@GetMapping("/communes")
	@Operation(summary = "List every known commune with the zone that covers it",
			description = "`zone` is null for communes that are known but not covered. Use `code` as the address commune.")
	List<CommuneResponse> communes() {
		return coverage.listCommunes().stream().map(CommuneResponse::from).toList();
	}

	private static CommuneRef ref(Commune commune) {
		return new CommuneRef(commune.getCode(), commune.getName());
	}

}
