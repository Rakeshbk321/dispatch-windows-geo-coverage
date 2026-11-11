package com.retail.dispatch.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
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
class CoverageApiTests {

	@Autowired
	MockMvc mvc;

	@Test
	void zonesListTheirCommunesAndWindowCapacity() throws Exception {
		mvc.perform(get("/api/v1/zones"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(3))
			.andExpect(jsonPath("$[0].code").value("LS-CQ"))
			.andExpect(jsonPath("$[0].windowCapacity").value(5))
			.andExpect(jsonPath("$[0].communes[*].code", contains("coquimbo", "la-serena")))
			.andExpect(jsonPath("$[1].code").value("SCL-CO"))
			.andExpect(jsonPath("$[1].windowCapacity").value(8))
			.andExpect(jsonPath("$[2].code").value("VAL-VDM"))
			.andExpect(jsonPath("$[2].communes.length()").value(5));
	}

	@Test
	void communesShowTheirZoneOrNullWhenNotCovered() throws Exception {
		mvc.perform(get("/api/v1/communes"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(21))
			.andExpect(jsonPath("$[?(@.code == 'la-serena')].zone", contains("LS-CQ")))
			.andExpect(jsonPath("$[?(@.code == 'vina-del-mar')].zone", contains("VAL-VDM")))
			.andExpect(jsonPath("$[?(@.code == 'vicuna')].zone", contains(nullValue())));
	}

}
