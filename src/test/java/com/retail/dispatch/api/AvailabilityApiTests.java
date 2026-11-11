package com.retail.dispatch.api;

import static com.retail.dispatch.FixedClockConfiguration.TODAY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.retail.dispatch.FixedClockConfiguration;
import com.retail.dispatch.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class AvailabilityApiTests {

	@Autowired
	MockMvc mvc;

	@Test
	void datesForACoveredAddressCoverTheWholeHorizon() throws Exception {
		mvc.perform(get("/api/v1/availability/dates").param("commune", "la-serena"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.commune").value("la-serena"))
			.andExpect(jsonPath("$.covered").value(true))
			.andExpect(jsonPath("$.dates.length()").value(7))
			.andExpect(jsonPath("$.dates[0].date").value(TODAY.toString()))
			.andExpect(jsonPath("$.dates[0].status").value("AVAILABLE"))
			.andExpect(jsonPath("$.dates[6].date").value(TODAY.plusDays(6).toString()));
	}

	@Test
	void datesForAnUncoveredAddressSayNotCovered() throws Exception {
		mvc.perform(get("/api/v1/availability/dates").param("commune", "vicuna"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.covered").value(false))
			.andExpect(jsonPath("$.dates.length()").value(0));
	}

	@Test
	void unknownOrMissingCommuneIsAValidationError() throws Exception {
		mvc.perform(get("/api/v1/availability/dates").param("commune", "atlantis"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors[0].field").value("commune"));

		mvc.perform(get("/api/v1/availability/dates"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void blocksShowTimesCostRemainingAndStatus() throws Exception {
		mvc.perform(get("/api/v1/availability/dates/{date}", TODAY).param("commune", "la-serena"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.date").value(TODAY.toString()))
			.andExpect(jsonPath("$.covered").value(true))
			.andExpect(jsonPath("$.blocks.length()").value(7))
			.andExpect(jsonPath("$.blocks[0].start").value("08:00:00"))
			.andExpect(jsonPath("$.blocks[0].status").value("CLOSED"))
			.andExpect(jsonPath("$.blocks[4].start").value("16:00:00"))
			.andExpect(jsonPath("$.blocks[4].status").value("CLOSED"))
			.andExpect(jsonPath("$.blocks[5].start").value("18:00:00"))
			.andExpect(jsonPath("$.blocks[5].end").value("20:00:00"))
			.andExpect(jsonPath("$.blocks[5].cost").value(2990))
			.andExpect(jsonPath("$.blocks[5].currency").value("CLP"))
			.andExpect(jsonPath("$.blocks[5].remaining").value(5))
			.andExpect(jsonPath("$.blocks[5].status").value("AVAILABLE"))
			.andExpect(jsonPath("$.blocks[6].end").value("22:00:00"));
	}

	@Test
	void blocksOutsideTheHorizonAreNotBookable() throws Exception {
		mvc.perform(get("/api/v1/availability/dates/{date}", TODAY.plusDays(7)).param("commune", "la-serena"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("WINDOW_NOT_BOOKABLE"));
	}

	@Test
	void malformedDateIsAValidationError() throws Exception {
		mvc.perform(get("/api/v1/availability/dates/{date}", "2030-13-01").param("commune", "la-serena"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

}
