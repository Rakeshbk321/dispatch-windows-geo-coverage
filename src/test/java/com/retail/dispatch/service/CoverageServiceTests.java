package com.retail.dispatch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.retail.dispatch.TestcontainersConfiguration;
import com.retail.dispatch.domain.Commune;
import com.retail.dispatch.domain.Zone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CoverageServiceTests {

	@Autowired
	CoverageService coverage;

	@Test
	void resolvesCoveredCommuneToItsZone() {
		assertThat(coverage.resolveZone("la-serena")).map(Zone::getCode).contains("LS-CQ");
		assertThat(coverage.resolveZone("nunoa")).map(Zone::getCode).contains("SCL-CO");
		assertThat(coverage.resolveZone("vina-del-mar")).map(Zone::getCode).contains("VAL-VDM");
	}

	@Test
	void returnsEmptyForKnownButUncoveredCommune() {
		assertThat(coverage.resolveZone("vicuna")).isEmpty();
		assertThat(coverage.resolveZone("maipu")).isEmpty();
	}

	@Test
	void rejectsUnknownCommune() {
		assertThatThrownBy(() -> coverage.resolveZone("atlantis"))
			.isInstanceOf(UnknownCommuneException.class)
			.hasMessageContaining("atlantis");
	}

	@Test
	void listsEveryCommuneWithItsZoneLoaded() {
		var communes = coverage.listCommunes();

		assertThat(communes).extracting(Commune::getCode)
			.containsExactly("concon", "coquimbo", "independencia", "la-reina", "la-serena", "las-condes",
					"lo-barnechea", "maipu", "nunoa", "ovalle", "providencia", "puente-alto", "quillota", "quilpue",
					"recoleta", "santiago", "valparaiso", "vicuna", "villa-alemana", "vina-del-mar", "vitacura");
		assertThat(communes).filteredOn(c -> c.getZone() != null)
			.extracting(c -> c.getZone().getCode())
			.containsOnly("LS-CQ", "SCL-CO", "VAL-VDM");
	}

}
