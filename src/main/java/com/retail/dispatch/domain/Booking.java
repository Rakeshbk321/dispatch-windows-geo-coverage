package com.retail.dispatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Booking {

	@Id
	@GeneratedValue
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "window_id")
	private DispatchWindow window;

	private String orderId;

	private String street;

	private String streetNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "commune_code")
	private Commune commune;

	// Copied from the time block when booking, so a later price change doesn't rewrite what the customer paid.
	@Column(precision = 10, scale = 0)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	private BookingStatus status;

	private Instant createdAt;

	private Instant cancelledAt;

	protected Booking() {
	}

	public Booking(DispatchWindow window, String orderId, String street, String streetNumber, Commune commune,
			BigDecimal price, Instant createdAt) {
		this.window = window;
		this.orderId = orderId;
		this.street = street;
		this.streetNumber = streetNumber;
		this.commune = commune;
		this.price = price;
		this.status = BookingStatus.CONFIRMED;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public DispatchWindow getWindow() {
		return window;
	}

	public String getOrderId() {
		return orderId;
	}

	public String getStreet() {
		return street;
	}

	public String getStreetNumber() {
		return streetNumber;
	}

	public Commune getCommune() {
		return commune;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public BookingStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getCancelledAt() {
		return cancelledAt;
	}

}
