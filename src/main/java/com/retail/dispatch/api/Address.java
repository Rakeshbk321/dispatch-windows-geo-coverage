package com.retail.dispatch.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Address(
		@Schema(example = "Monjitas") @NotBlank @Size(max = 200) String street,
		@Schema(example = "2334") @NotBlank @Size(max = 20) String number,
		@Schema(description = "Commune code, see GET /api/v1/communes", example = "la-serena") @NotBlank String commune) {
}
