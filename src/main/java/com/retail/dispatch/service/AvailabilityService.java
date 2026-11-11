package com.retail.dispatch.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.retail.dispatch.domain.AvailabilityStatus;
import com.retail.dispatch.domain.DispatchWindow;
import com.retail.dispatch.domain.Zone;
import com.retail.dispatch.repository.DispatchWindowRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {

	private final CoverageService coverage;

	private final DispatchWindowRepository windows;

	private final Horizon horizon;

	private final Clock clock;

	public AvailabilityService(CoverageService coverage, DispatchWindowRepository windows, Horizon horizon,
			Clock clock) {
		this.coverage = coverage;
		this.windows = windows;
		this.horizon = horizon;
		this.clock = clock;
	}

	/**
	 * Every date in the horizon with its status, for the zone serving the commune.
	 * @throws UnknownCommuneException if the commune code doesn't exist
	 */
	public DatesResult dates(String communeCode) {
		Optional<Zone> zone = coverage.resolveZone(communeCode);
		if (zone.isEmpty()) {
			return DatesResult.notCovered();
		}
		List<LocalDate> dates = horizon.dates();
		LocalDateTime now = LocalDateTime.now(clock);
		Map<LocalDate, List<AvailabilityStatus>> blockStatusesByDate = windows
			.findWithBlockByZoneAndDateBetween(zone.get(), dates.getFirst(), dates.getLast())
			.stream()
			.collect(groupingBy(DispatchWindow::getDeliveryDate, mapping(w -> w.statusAt(now), toList())));

		List<DateOption> options = dates.stream()
			.map(date -> new DateOption(date,
					AvailabilityStatus.ofDate(blockStatusesByDate.getOrDefault(date, List.of()))))
			.toList();
		return new DatesResult(true, options);
	}

	/**
	 * The time blocks for one date, with cost, remaining capacity and status.
	 * @throws UnknownCommuneException if the commune code doesn't exist
	 * @throws DateOutsideHorizonException if the date is before today or past the horizon
	 */
	public BlocksResult blocks(String communeCode, LocalDate date) {
		Optional<Zone> zone = coverage.resolveZone(communeCode);
		if (zone.isEmpty()) {
			return BlocksResult.notCovered();
		}
		if (!horizon.contains(date)) {
			throw new DateOutsideHorizonException(date);
		}
		LocalDateTime now = LocalDateTime.now(clock);
		List<BlockOption> options = windows.findWithBlockByZoneAndDateBetween(zone.get(), date, date)
			.stream()
			.map(w -> new BlockOption(w.getTimeBlock().getId(), w.getTimeBlock().getStartTime(),
					w.getTimeBlock().getEndTime(), w.getTimeBlock().getCost(), w.getRemaining(), w.statusAt(now)))
			.toList();
		return new BlocksResult(true, options);
	}

}
