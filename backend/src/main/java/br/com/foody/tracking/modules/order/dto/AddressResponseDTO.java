package br.com.foody.tracking.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Delivery address")
public record AddressResponseDTO(
        @Schema(description = "Street name", example = "Rua José Neves") String street,
        @Schema(description = "Street number", example = "590") String number,
        @Schema(description = "Complement, when informed", example = "sala 03") String complement,
        @Schema(description = "Neighborhood", example = "Vila São Paulo") String neighborhood,
        @Schema(description = "City", example = "São Paulo") String city,
        @Schema(description = "State (UF)", example = "SP") String state,
        @Schema(description = "Postal code (CEP)", example = "04650-140") String postalCode,
        @Schema(description = "Latitude, when informed", example = "-23.6578") Double lat,
        @Schema(description = "Longitude, when informed", example = "-46.6743") Double lng) {}
