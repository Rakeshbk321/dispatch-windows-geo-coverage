package com.retail.dispatch.web;

import static com.retail.dispatch.FixedClockConfiguration.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.retail.dispatch.FixedClockConfiguration;
import com.retail.dispatch.TestcontainersConfiguration;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** The clock is pinned to Monday 2030-03-04 16:30, so today only the 18:00 and 20:00 blocks are still shown. */
@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class BookingPageTests {

	static final LocalDate TOMORROW = TODAY.plusDays(1);

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@AfterEach
	void cleanUp() {
		jdbc.update("DELETE FROM booking");
		jdbc.update("UPDATE dispatch_window SET booked_count = 0");
	}

	@Test
	void pageShowsTheAddressFormWithEveryCommuneAndLoadsHtmxLocally() throws Exception {
		mvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Book a delivery slot")))
			.andExpect(content().string(containsString("La Serena (Coquimbo)")))
			.andExpect(content().string(containsString("Vicuña (Coquimbo)")))
			.andExpect(content().string(containsString("src=\"/js/htmx.min.js\"")));
		mvc.perform(get("/js/htmx.min.js")).andExpect(status().isOk());
		mvc.perform(get("/css/app.css")).andExpect(status().isOk());
	}

	@Test
	void scheduleShowsTheAddressSevenDaysAndTodaysRemainingBlocks() throws Exception {
		String html = mvc.perform(withAddress(get("/ui/schedule"), "la-serena"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Order ORD-1")))
			.andExpect(content().string(containsString("Monjitas, 2334, La Serena, Coquimbo")))
			.andExpect(content().string(containsString("Today")))
			.andExpect(content().string(containsString("4/3")))
			.andExpect(content().string(containsString("Tue")))
			.andExpect(content().string(containsString("10/3")))
			.andExpect(content().string(containsString("6:00pm-8:00pm")))
			.andExpect(content().string(containsString("8:00pm-10:00pm")))
			.andExpect(content().string(containsString("$2,990")))
			.andExpect(content().string(not(containsString("4:00pm-6:00pm"))))
			.andExpect(content().string(not(containsString("8:00am-10:00am"))))
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(html.split("aria-pressed=").length - 1).isEqualTo(7);
	}

	@Test
	void soldOutDayIsGreyedOutAndLabelledSoldOut() throws Exception {
		jdbc.update("""
				UPDATE dispatch_window w SET booked_count = capacity FROM zone z
				WHERE z.id = w.zone_id AND z.code = 'LS-CQ' AND w.delivery_date = ?
				""", TODAY.plusDays(6));

		mvc.perform(withAddress(get("/ui/schedule"), "la-serena"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("day unavailable")))
			.andExpect(content().string(containsString("Sold out")));
	}

	@Test
	void uncoveredAddressSaysWeDontDeliverThere() throws Exception {
		mvc.perform(withAddress(get("/ui/schedule"), "vicuna"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("We don't deliver to this address")))
			.andExpect(content().string(not(containsString("Book</button>"))));
	}

	@Test
	void incompleteAddressReturnsTheFormWithAMessage() throws Exception {
		mvc.perform(get("/ui/schedule").param("orderId", "ORD-1").param("commune", "la-serena"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Fill in the order number, street, number and commune.")))
			.andExpect(content().string(containsString("See dates and times")));
	}

	@Test
	void bookingShowsTheConfirmation() throws Exception {
		mvc.perform(bookTomorrowAt6pm())
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Booking confirmed!")))
			.andExpect(content().string(containsString("Monjitas, 2334, La Serena, Coquimbo")))
			.andExpect(content().string(containsString("Tue 5/3")))
			.andExpect(content().string(containsString("6:00pm-8:00pm")))
			.andExpect(content().string(containsString("$2,990")))
			.andExpect(content().string(containsString("Cancel booking")));

		assertThat(jdbc.queryForObject("SELECT count(*) FROM booking WHERE status = 'CONFIRMED'", Integer.class))
			.isEqualTo(1);
	}

	@Test
	void losingTheRaceForTheLastSeatSaysItJustSoldOut() throws Exception {
		jdbc.update("UPDATE dispatch_window SET booked_count = capacity WHERE delivery_date = ?", TOMORROW);

		mvc.perform(bookTomorrowAt6pm())
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("This time just sold out. Please choose another one.")))
			.andExpect(content().string(containsString("Sold out")));
	}

	@Test
	void cancellingFromTheConfirmationGivesTheSeatBack() throws Exception {
		mvc.perform(bookTomorrowAt6pm()).andExpect(status().isOk());
		String id = jdbc.queryForObject("SELECT id::text FROM booking", String.class);

		mvc.perform(post("/ui/bookings/{id}/cancel", id))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Booking cancelled.")))
			.andExpect(content().string(containsString("6:00pm-8:00pm")));

		assertThat(jdbc.queryForObject("SELECT status FROM booking", String.class)).isEqualTo("CANCELLED");
		assertThat(jdbc.queryForObject("SELECT sum(booked_count) FROM dispatch_window", Integer.class)).isZero();
	}

	private MockHttpServletRequestBuilder bookTomorrowAt6pm() {
		long blockId = jdbc.queryForObject("SELECT id FROM time_block WHERE start_time = '18:00'", Long.class);
		return withAddress(post("/ui/bookings"), "la-serena").param("date", TOMORROW.toString())
			.param("timeBlockId", String.valueOf(blockId));
	}

	private static MockHttpServletRequestBuilder withAddress(MockHttpServletRequestBuilder request, String commune) {
		return request.param("orderId", "ORD-1")
			.param("street", "Monjitas")
			.param("number", "2334")
			.param("commune", commune);
	}

}
