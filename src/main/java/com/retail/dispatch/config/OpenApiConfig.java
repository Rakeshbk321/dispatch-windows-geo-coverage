package com.retail.dispatch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI dispatchWindowsApi() {
		return new OpenAPI().info(new Info().title("Dispatch Windows API")
			.version("v1")
			.description("""
					Book a delivery date and time block for a retail order, based on the address's coverage zone \
					and each window's remaining capacity. Errors are application/problem+json with a stable `code`."""));
	}

}
