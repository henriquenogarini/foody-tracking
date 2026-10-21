package br.com.foody.tracking.shared.error;

public record ValidationError(String field, String message) {}
