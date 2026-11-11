package com.retail.dispatch.api;

import static com.retail.dispatch.FixedClockConfiguration.TODAY;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.retail.dispatch.FixedClockConfiguration;
import com.retail.dispatch.TestcontainersConfiguration;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTests {

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
	void bookingReturns201WithLocationAndTheBooking() throws Exception {
		String location = book("ORD-1", "la-serena", TOMORROW, "18:00").andExpect(status().isCreated())
			.andExpect(header().string("Location", matchesPattern("http://localhost/api/v1/bookings/[0-9a-f-]{36}")))
			.andExpect(jsonPath("$.orderId").value("ORD-1"))
			.andExpect(jsonPath("$.address.street").value("Monjitas"))
			.andExpect(jsonPath("$.address.number").value("2334"))
			.andExpect(jsonPath("$.address.commune").value("la-serena"))
			.andExpect(jsonPath("$.date").value(TOMORROW.toString()))
			.andExpect(jsonPath("$.start").value("18:00:00"))
			.andExpect(jsonPath("$.end").value("20:00:00"))
			.andExpect(jsonPath("$.price").value(2990))
			.andExpect(jsonPath("$.currency").value("CLP"))
			.andExpect(jsonPath("$.status").value("CONFIRMED"))
			.andExpect(jsonPath("$.createdAt").value("2030-03-04T19:30:00Z"))
			.andExpect(jsonPath("$.cancelledAt").value(nullValue()))
			.andReturn()
			.getResponse()
			.getHeader("Location");

		mvc.perform(get(location))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.orderId").value("ORD-1"))
			.andExpect(jsonPath("$.status").value("CONFIRMED"));
	}

	@Test
	void invalidBodyListsEveryBadField() throws Exception {
		mvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content("""
				{"orderId": "", "address": {"street": "Monjitas", "number": "", "commune": "la-serena"}}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors[*].field",
					containsInAnyOrder("orderId", "address.number", "date", "timeBlockId")));
	}

	@Test
	void malformedJsonIsAValidationError() throws Exception {
		mvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content("{not json"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void soldOutWindowIsAConflict() throws Exception {
		jdbc.update("UPDATE dispatch_window SET booked_count = capacity");

		book("ORD-1", "la-serena", TOMORROW, "18:00").andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("WINDOW_SOLD_OUT"))
			.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void secondBookingForTheSameOrderIsAConflict() throws Exception {
		book("ORD-1", "la-serena", TOMORROW, "18:00").andExpect(status().isCreated());

		book("ORD-1", "la-serena", TOMORROW, "20:00").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ORDER_ALREADY_BOOKED"));
	}

	@Test
	void uncoveredAddressIsUnprocessable() throws Exception {
		book("ORD-1", "vicuna", TOMORROW, "18:00").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("ADDRESS_NOT_COVERED"));
	}

	@Test
	void unknownCommuneInTheAddressIsAValidationError() throws Exception {
		book("ORD-1", "atlantis", TOMORROW, "18:00").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void startedBlockOrUnknownBlockIsNotBookable() throws Exception {
		book("ORD-1", "la-serena", TODAY, "16:00").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("WINDOW_NOT_BOOKABLE"));

		mvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON)
			.content(body("ORD-1", "la-serena", TOMORROW, 999_999)))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("WINDOW_NOT_BOOKABLE"));
	}

	@Test
	void patchToCancelledCancelsAndRepeatingItChangesNothing() throws Exception {
		String id = idOf(book("ORD-1", "la-serena", TOMORROW, "18:00"));

		cancel(id).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"))
			.andExpect(jsonPath("$.cancelledAt").value("2030-03-04T19:30:00Z"));
		cancel(id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));

		mvc.perform(get("/api/v1/bookings/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void patchToAnyOtherStatusIsRejected() throws Exception {
		String id = idOf(book("ORD-1", "la-serena", TOMORROW, "18:00"));

		mvc.perform(patch("/api/v1/bookings/{id}", id).contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\": \"CONFIRMED\"}"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

		mvc.perform(patch("/api/v1/bookings/{id}", id).contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\": \"SHIPPED\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void unknownOrMalformedBookingId() throws Exception {
		mvc.perform(get("/api/v1/bookings/{id}", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));
		cancel(UUID.randomUUID().toString()).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));

		mvc.perform(get("/api/v1/bookings/{id}", "not-a-uuid"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void frameworkErrorsUseTheSameProblemShape() throws Exception {
		mvc.perform(delete("/api/v1/bookings/{id}", UUID.randomUUID()))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
		mvc.perform(get("/api/v1/no-such-thing"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	private ResultActions book(String orderId, String commune, LocalDate date, String blockStart) throws Exception {
		long blockId = jdbc.queryForObject("SELECT id FROM time_block WHERE start_time = ?::time", Long.class,
				blockStart);
		return mvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON)
			.content(body(orderId, commune, date, blockId)));
	}

	private ResultActions cancel(String id) throws Exception {
		return mvc.perform(patch("/api/v1/bookings/{id}", id).contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\": \"CANCELLED\"}"));
	}

	private static String body(String orderId, String commune, LocalDate date, long blockId) {
		return """
				{"orderId": "%s", "address": {"street": "Monjitas", "number": "2334", "commune": "%s"},
				 "date": "%s", "timeBlockId": %d}
				""".formatted(orderId, commune, date, blockId);
	}

	private static String idOf(ResultActions created) throws Exception {
		String location = created.andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
		return location.substring(location.lastIndexOf('/') + 1);
	}

}
