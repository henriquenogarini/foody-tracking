package br.com.foody.tracking.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "One page of results with the paging metadata")
public record PageResponseDTO<T>(
    @Schema(description = "Items of this page") List<T> content,
    @Schema(description = "Zero-based page index", example = "0") int page,
    @Schema(description = "Requested page size", example = "20") int size,
    @Schema(description = "Total number of items across all pages", example = "57")
        long totalElements,
    @Schema(description = "Total number of pages", example = "3") int totalPages) {

  public PageResponseDTO {
    content = List.copyOf(content);
  }

  public static <T> PageResponseDTO<T> from(Page<T> page) {
    return new PageResponseDTO<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
