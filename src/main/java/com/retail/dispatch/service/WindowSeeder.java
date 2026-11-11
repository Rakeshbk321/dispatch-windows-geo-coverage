package com.retail.dispatch.service;

import com.retail.dispatch.config.DispatchConfig;
import com.retail.dispatch.repository.DispatchWindowRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WindowSeeder {

	private static final Logger log = LoggerFactory.getLogger(WindowSeeder.class);

	private final DispatchWindowRepository windows;

	private final Horizon horizon;

	public WindowSeeder(DispatchWindowRepository windows, Horizon horizon) {
		this.windows = windows;
		this.horizon = horizon;
	}

	// Startup fills the whole horizon; the nightly run adds the day that just came into it.
	@EventListener(ApplicationReadyEvent.class)
	@Order(1)
	@Scheduled(cron = "0 5 0 * * *", zone = DispatchConfig.TIME_ZONE)
	public void ensureHorizon() {
		List<LocalDate> dates = horizon.dates();
		int created = windows.insertHorizon(dates.getFirst(), dates.getLast());
		log.info("Dispatch windows for {} to {}: {} created", dates.getFirst(), dates.getLast(), created);
	}

}
