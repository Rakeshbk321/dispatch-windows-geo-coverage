package com.retail.dispatch.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One bookable slot: a zone's capacity for one time block on one date.
 * {@code bookedCount} is only changed by conditional UPDATE queries, never through this entity.
 */
@Entity
public class DispatchWindow {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "zone_id")
	private Zone zone;

	private LocalDate deliveryDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "time_block_id")
	private TimeBlock timeBlock;

	private int capacity;

	private int bookedCount;

	protected DispatchWindow() {
	}

	public Long getId() {
		return id;
	}

	public Zone getZone() {
		return zone;
	}

	public LocalDate getDeliveryDate() {
		return deliveryDate;
	}

	public TimeBlock getTimeBlock() {
		return timeBlock;
	}

	public int getCapacity() {
		return capacity;
	}

	public int getBookedCount() {
		return bookedCount;
	}

	public int getRemaining() {
		return capacity - bookedCount;
	}

	public AvailabilityStatus statusAt(LocalDateTime now) {
		return AvailabilityStatus.ofBlock(LocalDateTime.of(deliveryDate, timeBlock.getStartTime()), getRemaining(), now);
	}

}
