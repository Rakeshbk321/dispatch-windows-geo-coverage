package com.retail.dispatch.web;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** Labels shown on the booking page: "Today", "Wed", "27/1", "4:00pm-6:00pm", "$2,990". */
final class UiFormat {

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mma", Locale.US);

	private UiFormat() {
	}

	static String weekday(LocalDate date, LocalDate today) {
		return date.equals(today) ? "Today" : shortWeekday(date);
	}

	static String dayMonth(LocalDate date) {
		return date.getDayOfMonth() + "/" + date.getMonthValue();
	}

	static String dateLabel(LocalDate date) {
		return shortWeekday(date) + " " + dayMonth(date);
	}

	static String timeRange(LocalTime start, LocalTime end) {
		return (TIME.format(start) + "-" + TIME.format(end)).toLowerCase(Locale.ROOT);
	}

	// Whole Chilean pesos with an English thousands separator.
	static String price(BigDecimal amount) {
		return new DecimalFormat("$#,##0", DecimalFormatSymbols.getInstance(Locale.US)).format(amount);
	}

	private static String shortWeekday(LocalDate date) {
		return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.US);
	}

}
