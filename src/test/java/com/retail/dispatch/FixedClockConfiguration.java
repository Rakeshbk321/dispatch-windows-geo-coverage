package com.retail.dispatch;

import com.retail.dispatch.config.DispatchConfig;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Pins "now" to Monday 2030-03-04 16:30 in Santiago: the 16:00 block has started, the 17:00 to 19:00 blocks haven't.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockConfiguration {

	public static final ZonedDateTime NOW = ZonedDateTime.of(2030, 3, 4, 16, 30, 0, 0,
			ZoneId.of(DispatchConfig.TIME_ZONE));

	public static final LocalDate TODAY = NOW.toLocalDate();

	@Bean
	@Primary
	Clock fixedClock() {
		return Clock.fixed(NOW.toInstant(), NOW.getZone());
	}

}
