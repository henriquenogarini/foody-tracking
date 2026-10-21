package br.com.foody.tracking.modules.auth.service;

import br.com.foody.tracking.modules.auth.dto.LoginRequestDTO;
import br.com.foody.tracking.modules.auth.dto.PasswordResetRequestDTO;
import br.com.foody.tracking.modules.auth.dto.RegisterRequestDTO;
import br.com.foody.tracking.modules.auth.dto.TokenResponseDTO;
import br.com.foody.tracking.modules.auth.dto.UserResponseDTO;
import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.helper.EmailNormalizer;
import br.com.foody.tracking.modules.auth.mapper.UserMapper;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import br.com.foody.tracking.modules.auth.security.AuthenticatedUser;
import br.com.foody.tracking.modules.auth.security.TokenService;
import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import br.com.foody.tracking.shared.exception.type.ApiException;
import br.com.foody.tracking.shared.exception.type.InvalidCredentialsException;
import br.com.foody.tracking.shared.exception.type.InvalidTokenException;
import br.com.foody.tracking.shared.exception.type.UserEmailAlreadyExistsException;
import br.com.foody.tracking.shared.validation.MaxUtf8Bytes;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Not final: @Transactional needs a CGLIB subclass proxy.
@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final UserMapper userMapper;
  private final Clock clock;

  // Compared against when the e-mail is unknown, so both login failures cost one BCrypt check
  // and response time does not reveal which accounts exist.
  private final String dummyPasswordHash;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService,
      UserMapper userMapper,
      Clock clock) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.userMapper = userMapper;
    this.clock = clock;
    this.dummyPasswordHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
  }

  // Pre-check only: pool size 1 serializes writers, and the NOCASE unique index stays the last
  // guard (SQLite surfaces it as a generic JpaSystemException, so it is not caught by type here).
  @Transactional
  public UserResponseDTO register(RegisterRequestDTO request) {
    String email = EmailNormalizer.normalize(request.email());
    if (userRepository.existsByEmail(email)) {
      log.warn("Registration rejected: e-mail already in use");
      throw new UserEmailAlreadyExistsException();
    }
    User user =
        userRepository.save(
            new User(
                request.name(),
                email,
                passwordEncoder.encode(request.password()),
                Role.USER,
                OffsetDateTime.now(clock)));
    log.info("User {} registered", user.getPublicId());
    return userMapper.toResponse(user);
  }

  // Not transactional: the pool has one connection, and BCrypt plus JWT signing must not hold it.
  public TokenResponseDTO login(LoginRequestDTO request) {
    if (request.password().getBytes(StandardCharsets.UTF_8).length
        > MaxUtf8Bytes.BCRYPT_MAX_BYTES) {
      log.debug("Login rejected: password longer than the BCrypt limit");
      throw new InvalidCredentialsException();
    }
    Optional<User> user = userRepository.findByEmail(EmailNormalizer.normalize(request.email()));
    String hash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
    boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
    if (user.isEmpty() || !passwordMatches) {
      log.warn("Login rejected: invalid credentials");
      throw new InvalidCredentialsException();
    }
    log.info("User {} logged in", user.get().getPublicId());
    return tokenService.issue(user.get());
  }

  // Demo feature, no e-mail confirmation. Not transactional, like login: BCrypt runs before the
  // single short update. The hash is computed on every path (existing USER, unknown e-mail,
  // ADMIN) so response time does not reveal which accounts exist; the affected-row count never
  // changes the outcome.
  public void resetPassword(PasswordResetRequestDTO request) {
    if (request.newPassword().getBytes(StandardCharsets.UTF_8).length
        > MaxUtf8Bytes.BCRYPT_MAX_BYTES) {
      log.debug("Password reset rejected: password longer than the BCrypt limit");
      throw new ApiException(
          ApiErrorCode.VALIDATION_ERROR, "A nova senha excede o limite de 72 bytes.");
    }
    String hash = passwordEncoder.encode(request.newPassword());
    int updated =
        userRepository.updatePasswordHashOfUser(EmailNormalizer.normalize(request.email()), hash);
    if (updated > 0) {
      log.info("Password reset applied");
    } else {
      log.debug("Password reset no-op");
    }
  }

  // A valid token whose user no longer exists (database reset) is treated as an invalid token.
  @Transactional(readOnly = true)
  public UserResponseDTO me(AuthenticatedUser principal) {
    return userRepository
        .findByPublicId(principal.id())
        .map(userMapper::toResponse)
        .orElseThrow(
            () -> {
              log.debug("Token subject {} has no matching user", principal.id());
              return new InvalidTokenException();
            });
  }
}
