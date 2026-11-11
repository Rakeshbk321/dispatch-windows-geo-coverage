package com.retail.dispatch.service;

import com.retail.dispatch.domain.Commune;
import com.retail.dispatch.domain.Zone;
import com.retail.dispatch.repository.CommuneRepository;
import com.retail.dispatch.repository.ZoneRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CoverageService {

	private final CommuneRepository communes;

	private final ZoneRepository zones;

	public CoverageService(CommuneRepository communes, ZoneRepository zones) {
		this.communes = communes;
		this.zones = zones;
	}

	/**
	 * Finds the zone that delivers to a commune. Coverage depends only on the commune, not on street or number.
	 * @return the zone, or empty if the commune is known but not covered
	 * @throws UnknownCommuneException if the commune code doesn't exist
	 */
	public Optional<Zone> resolveZone(String communeCode) {
		return Optional.ofNullable(getCommune(communeCode).getZone());
	}

	/**
	 * @return the commune with its zone (null if not covered) loaded
	 * @throws UnknownCommuneException if the commune code doesn't exist
	 */
	public Commune getCommune(String communeCode) {
		return communes.findWithZoneByCode(communeCode).orElseThrow(() -> new UnknownCommuneException(communeCode));
	}

	public List<Commune> listCommunes() {
		return communes.findAllWithZoneByOrderByCode();
	}

	public List<Zone> listZones() {
		return zones.findAllByOrderByCode();
	}

}
