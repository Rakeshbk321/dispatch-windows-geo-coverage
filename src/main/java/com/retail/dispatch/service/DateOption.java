package com.retail.dispatch.service;

import com.retail.dispatch.domain.AvailabilityStatus;
import java.time.LocalDate;

public record DateOption(LocalDate date, AvailabilityStatus status) {
}
