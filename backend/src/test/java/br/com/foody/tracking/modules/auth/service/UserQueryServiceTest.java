package br.com.foody.tracking.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.foody.tracking.modules.auth.dto.UserResponseDTO;
import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.mapper.UserMapper;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import br.com.foody.tracking.shared.exception.type.InvalidSortException;
import br.com.foody.tracking.shared.web.PageResponseDTO;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

  private static final Pageable PAGEABLE =
      PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

  @Mock private UserRepository userRepository;

  private UserQueryService userQueryService;

  @BeforeEach
  void setUp() {
    userQueryService = new UserQueryService(userRepository, Mappers.getMapper(UserMapper.class));
  }

  @Test
  void shouldReturnMappedPage_whenUsersExist() {
    // Given
    User user =
        new User(
            "Ana Souza",
            "ana@exemplo.com",
            "hash",
            Role.USER,
            OffsetDateTime.parse("2026-10-21T14:13:27-03:00"));
    when(userRepository.findAll(PAGEABLE)).thenReturn(new PageImpl<>(List.of(user), PAGEABLE, 21));

    // When
    PageResponseDTO<UserResponseDTO> page = userQueryService.list(PAGEABLE);

    // Then
    assertThat(page.content())
        .singleElement()
        .satisfies(
            response -> {
              assertThat(response.id()).isEqualTo(user.getPublicId());
              assertThat(response.email()).isEqualTo("ana@exemplo.com");
              assertThat(response.role()).isEqualTo(Role.USER);
            });
    assertThat(page.page()).isZero();
    assertThat(page.size()).isEqualTo(20);
    assertThat(page.totalElements()).isEqualTo(21);
    assertThat(page.totalPages()).isEqualTo(2);
  }

  @Test
  void shouldReturnEmptyPage_whenNoUserExists() {
    // Given
    when(userRepository.findAll(PAGEABLE)).thenReturn(Page.empty(PAGEABLE));

    // When
    PageResponseDTO<UserResponseDTO> page = userQueryService.list(PAGEABLE);

    // Then
    assertThat(page.content()).isEmpty();
    assertThat(page.totalElements()).isZero();
  }

  @Test
  void shouldRejectSortWithoutQuerying_whenPropertyIsNotWhitelisted() {
    // Given
    Pageable bySecret = PageRequest.of(0, 20, Sort.by("passwordHash"));

    // When / Then
    assertThatThrownBy(() -> userQueryService.list(bySecret))
        .isInstanceOfSatisfying(
            InvalidSortException.class,
            ex -> assertThat(ex.getProperty()).isEqualTo("passwordHash"));
    verifyNoInteractions(userRepository);
  }
}
