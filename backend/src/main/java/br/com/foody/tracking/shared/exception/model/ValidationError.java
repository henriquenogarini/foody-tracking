package br.com.foody.tracking.shared.exception.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single invalid field")
public record ValidationError(
    @Schema(description = "Path of the invalid field", example = "items") String field,
    @Schema(
            description = "What is wrong with it (Portuguese)",
            example = "deve conter ao menos 1 item")
        String message) {}
