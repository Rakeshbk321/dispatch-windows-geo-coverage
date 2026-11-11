package com.retail.dispatch.web;

import com.retail.dispatch.domain.AvailabilityStatus;
import com.retail.dispatch.domain.Commune;
import com.retail.dispatch.service.AvailabilityService;
import com.retail.dispatch.service.BookCommand;
import com.retail.dispatch.service.BookingDetails;
import com.retail.dispatch.service.BookingService;
import com.retail.dispatch.service.CoverageService;
import com.retail.dispatch.service.DateOption;
import com.retail.dispatch.service.DatesResult;
import com.retail.dispatch.service.OrderAlreadyBookedException;
import com.retail.dispatch.service.UnknownCommuneException;
import com.retail.dispatch.service.WindowNotBookableException;
import com.retail.dispatch.service.WindowSoldOutException;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Server-rendered booking screen. Every interaction is an HTMX request that returns a fragment of
 * {@code fragments.html} to swap into {@code #app}. Uses the same services as the REST API.
 */
@Controller
class BookingPageController {

	private final CoverageService coverage;

	private final AvailabilityService availability;

	private final BookingService bookings;

	private final Clock clock;

	BookingPageController(CoverageService coverage, AvailabilityService availability, BookingService bookings,
			Clock clock) {
		this.coverage = coverage;
		this.availability = availability;
		this.bookings = bookings;
		this.clock = clock;
	}

	@GetMapping("/")
	String page(Model model) {
		addressForm(AddressForm.empty(), null, model);
		return "index";
	}

	@GetMapping("/ui/address")
	String editAddress(AddressForm form, Model model) {
		return addressForm(form, null, model);
	}

	@GetMapping("/ui/schedule")
	String schedule(@Valid AddressForm form, BindingResult errors,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {
		if (errors.hasErrors()) {
			return addressForm(form, "Fill in the order number, street, number and commune.", model);
		}
		return schedule(form, date, null, null, model);
	}

	@PostMapping("/ui/bookings")
	String book(@Valid AddressForm form, BindingResult errors,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) Long timeBlockId, Model model) {
		if (errors.hasErrors()) {
			return addressForm(form, "Fill in the order number, street, number and commune.", model);
		}
		if (timeBlockId == null) {
			return schedule(form, date, null, "Choose a delivery time.", model);
		}
		try {
			BookingDetails booking = bookings.book(new BookCommand(form.orderId(), form.street(), form.number(),
					form.commune(), date, timeBlockId));
			model.addAttribute("booking", confirmation(booking, coverage.getCommune(booking.communeCode())));
			return "fragments :: booked";
		}
		catch (WindowSoldOutException ex) {
			return schedule(form, date, null, "This time just sold out. Please choose another one.", model);
		}
		catch (WindowNotBookableException ex) {
			return schedule(form, date, null, "This time is no longer available. Please choose another one.", model);
		}
		catch (OrderAlreadyBookedException ex) {
			return schedule(form, date, null, "Order " + form.orderId() + " already has a confirmed booking.",
					model);
		}
	}

	@PostMapping("/ui/bookings/{id}/cancel")
	String cancel(@PathVariable UUID id, Model model) {
		BookingDetails booking = bookings.cancel(id);
		AddressForm form = new AddressForm(booking.orderId(), booking.street(), booking.streetNumber(),
				booking.communeCode());
		return schedule(form, booking.date(), "Booking cancelled. The time is available again.", null,
				model);
	}

	private String addressForm(AddressForm form, String error, Model model) {
		model.addAttribute("form", form);
		model.addAttribute("communes", coverage.listCommunes());
		model.addAttribute("error", error);
		return "fragments :: addressForm";
	}

	private String schedule(AddressForm form, LocalDate requestedDate, String info, String error, Model model) {
		Commune commune;
		try {
			commune = coverage.getCommune(form.commune());
		}
		catch (UnknownCommuneException ex) {
			return addressForm(form, "Choose a commune from the list.", model);
		}
		model.addAttribute("form", form);
		model.addAttribute("address", addressLine(form.street(), form.number(), commune));
		model.addAttribute("info", info);
		model.addAttribute("error", error);

		DatesResult dates = availability.dates(form.commune());
		model.addAttribute("covered", dates.covered());
		if (!dates.covered()) {
			return "fragments :: schedule";
		}
		LocalDate today = LocalDate.now(clock);
		LocalDate selected = pickDate(requestedDate, dates.dates());
		model.addAttribute("selectedDate", selected);
		model.addAttribute("days", dates.dates()
			.stream()
			.map(d -> new DayChip(d.date(), UiFormat.weekday(d.date(), today), UiFormat.dayMonth(d.date()),
					d.status(), d.date().equals(selected)))
			.toList());
		model.addAttribute("blocks", selected == null ? List.of()
				: availability.blocks(form.commune(), selected)
					.blocks()
					.stream()
					// Blocks that have started are hidden rather than shown disabled.
					.filter(b -> b.status() != AvailabilityStatus.CLOSED)
					.map(b -> new BlockRow(b.timeBlockId(), UiFormat.timeRange(b.start(), b.end()),
							UiFormat.price(b.cost()), b.status() == AvailabilityStatus.SOLD_OUT))
					.toList());
		return "fragments :: schedule";
	}

	// Keep the date the user picked unless every block on it has started; otherwise open the first bookable date.
	private static LocalDate pickDate(LocalDate requested, List<DateOption> dates) {
		return dates.stream()
			.filter(d -> d.date().equals(requested) && d.status() != AvailabilityStatus.CLOSED)
			.findFirst()
			.or(() -> dates.stream().filter(d -> d.status() == AvailabilityStatus.AVAILABLE).findFirst())
			.map(DateOption::date)
			.orElse(null);
	}

	private static ConfirmationView confirmation(BookingDetails booking, Commune commune) {
		return new ConfirmationView(booking.id(), booking.orderId(),
				addressLine(booking.street(), booking.streetNumber(), commune), UiFormat.dateLabel(booking.date()),
				UiFormat.timeRange(booking.start(), booking.end()), UiFormat.price(booking.price()));
	}

	// Same shape as the reference screen: "Monjitas, 2334, La Serena, Coquimbo".
	private static String addressLine(String street, String number, Commune commune) {
		return street + ", " + number + ", " + commune.getName() + ", " + commune.getRegion();
	}

}
