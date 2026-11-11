package com.retail.dispatch.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Commune {

	@Id
	private String code;

	private String name;

	private String region;

	// Null means the commune is known but no zone delivers there.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "zone_id")
	private Zone zone;

	protected Commune() {
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getRegion() {
		return region;
	}

	public Zone getZone() {
		return zone;
	}

}
