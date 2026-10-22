package br.com.foody.tracking.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResponseDTOTest {

    @Test
    void shouldMapEveryField_whenCreatedFromPage() {
        // Given
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);

        // When
        PageResponseDTO<String> response = PageResponseDTO.from(page);

        // Then
        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void shouldExposeUnmodifiableContent_whenSourceListChanges() {
        // Given
        List<String> source = new ArrayList<>(List.of("a"));
        PageResponseDTO<String> response = new PageResponseDTO<>(source, 0, 1, 1, 1);

        // When
        source.add("b");

        // Then
        assertThat(response.content()).containsExactly("a");
        assertThatThrownBy(() -> response.content().add("c"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
