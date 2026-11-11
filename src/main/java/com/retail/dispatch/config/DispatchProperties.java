package com.retail.dispatch.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("dispatch")
public record DispatchProperties(@Min(1) int horizonDays) {
}
