package br.com.foody.tracking.modules.auth.service;

import br.com.foody.tracking.modules.auth.dto.UserResponseDTO;
import br.com.foody.tracking.modules.auth.mapper.UserMapper;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import br.com.foody.tracking.shared.web.PageResponseDTO;
import br.com.foody.tracking.shared.web.SortWhitelist;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryService {

  private static final Set<String> SORTABLE_PROPERTIES = Set.of("createdAt", "name", "email");

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  // Admin-only at the endpoint, so the query is unscoped by design.
  @Transactional(readOnly = true)
  public PageResponseDTO<UserResponseDTO> list(Pageable pageable) {
    SortWhitelist.requireAllowed(pageable.getSort(), SORTABLE_PROPERTIES);
    return PageResponseDTO.from(userRepository.findAll(pageable).map(userMapper::toResponse));
  }
}
