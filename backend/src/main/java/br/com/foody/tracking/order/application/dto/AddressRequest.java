package br.com.foody.tracking.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Delivery address; lat and lng are optional but must come together")
@LatLngTogether
public record AddressRequest(
    @Schema(description = "Street name", example = "Rua José Neves") @NotBlank @Size(max = 200)
        String street,
    @Schema(description = "Street number", example = "590") @NotBlank @Size(max = 20) String number,
    @Schema(description = "Optional complement", example = "sala 03") @Size(max = 100)
        String complement,
    @Schema(description = "Neighborhood", example = "Vila São Paulo") @NotBlank @Size(max = 100)
        String neighborhood,
    @Schema(description = "City", example = "São Paulo") @NotBlank @Size(max = 100) String city,
    @Schema(description = "State (UF), 2 uppercase letters", example = "SP")
        @NotNull
        @Pattern(regexp = "^[A-Z]{2}$", message = "deve ser a sigla da UF com 2 letras maiúsculas")
        String state,
    @Schema(description = "Postal code (CEP), 00000-000 or 00000000", example = "04650-140")
        @NotNull
        @Pattern(
            regexp = "^\\d{5}-?\\d{3}$",
            message = "deve estar no formato 00000-000 ou 00000000")
        String postalCode,
    @Schema(description = "Optional latitude, required when lng is present", example = "-23.6578")
        @DecimalMin("-90")
        @DecimalMax("90")
        Double lat,
    @Schema(description = "Optional longitude, required when lat is present", example = "-46.6743")
        @DecimalMin("-180")
        @DecimalMax("180")
        Double lng) {}
