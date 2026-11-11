package com.retail.dispatch.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.retail.dispatch.FixedClockConfiguration;
import com.retail.dispatch.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTests {

	@Autowired
	MockMvc mvc;

	@Test
	void specDocumentsEveryEndpointWithItsErrorResponses() throws Exception {
		mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.info.title").value("Dispatch Windows API"))
			.andExpect(jsonPath("$.paths['/api/v1/zones'].get").exists())
			.andExpect(jsonPath("$.paths['/api/v1/communes'].get").exists())
			.andExpect(jsonPath("$.paths['/api/v1/availability/dates'].get.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/availability/dates/{date}'].get.responses['422']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/bookings'].post.responses['201'].headers.Location").exists())
			.andExpect(jsonPath("$.paths['/api/v1/bookings'].post.responses['409']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/bookings/{id}'].get.responses['404']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/bookings/{id}'].patch.responses['422']").exists());
	}

	@Test
	void swaggerUiIsServed() throws Exception {
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
	}

}
