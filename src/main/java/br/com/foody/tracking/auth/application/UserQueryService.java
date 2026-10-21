package br.com.foody.tracking.auth.application;

import br.com.foody.tracking.auth.application.dto.UserResponse;
import br.com.foody.tracking.auth.domain.UserRepository;
import br.com.foody.tracking.shared.web.PageResponse;
import br.com.foody.tracking.shared.web.SortWhitelist;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Not final: @Transactional needs a CGLIB subclass proxy.
@Service
public class UserQueryService {

  private static final Set<String> SORTABLE_PROPERTIES = Set.of("createdAt", "name", "email");

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  public UserQueryService(UserRepository userRepository, UserMapper userMapper) {
    this.userRepository = userRepository;
    this.userMapper = userMapper;
  }

  // Admin-only at the endpoint, so the query is unscoped by design.
  @Transactional(readOnly = true)
  public PageResponse<UserResponse> list(Pageable pageable) {
    SortWhitelist.requireAllowed(pageable.getSort(), SORTABLE_PROPERTIES);
    return PageResponse.from(userRepository.findAll(pageable).map(userMapper::toResponse));
  }
}
