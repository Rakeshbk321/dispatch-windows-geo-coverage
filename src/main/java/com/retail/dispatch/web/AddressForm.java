package com.retail.dispatch.web;

import jakarta.validation.constraints.NotBlank;

public record AddressForm(@NotBlank String orderId, @NotBlank String street, @NotBlank String number,
		@NotBlank String commune) {

	static AddressForm empty() {
		return new AddressForm("", "", "", "");
	}

}
