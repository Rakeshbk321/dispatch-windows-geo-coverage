package com.retail.dispatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
public class TimeBlock {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private LocalTime startTime;

	private LocalTime endTime;

	@Column(precision = 10, scale = 0)
	private BigDecimal cost;

	protected TimeBlock() {
	}

	public Long getId() {
		return id;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public BigDecimal getCost() {
		return cost;
	}

}
