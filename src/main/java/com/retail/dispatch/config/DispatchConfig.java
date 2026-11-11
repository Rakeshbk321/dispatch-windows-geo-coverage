package com.retail.dispatch.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DispatchProperties.class)
public class DispatchConfig {

	public static final String TIME_ZONE = "America/Santiago";

	// Every "today" and "now" in the app comes from this clock, so tests can pin time.
	@Bean
	Clock clock() {
		return Clock.system(ZoneId.of(TIME_ZONE));
	}

}
