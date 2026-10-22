package br.com.foody.tracking.modules.auth.service;

import br.com.foody.tracking.modules.auth.dto.LoginRequestDTO;
import br.com.foody.tracking.modules.auth.dto.PasswordResetRequestDTO;
import br.com.foody.tracking.modules.auth.dto.RegisterRequestDTO;
import br.com.foody.tracking.modules.auth.dto.TokenResponseDTO;
import br.com.foody.tracking.modules.auth.dto.UserResponseDTO;
import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.helper.EmailNormalizer;
import br.com.foody.tracking.modules.auth.helper.PasswordPolicy;
import br.com.foody.tracking.modules.auth.mapper.UserMapper;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import br.com.foody.tracking.modules.auth.security.AuthenticatedUser;
import br.com.foody.tracking.modules.auth.security.TokenService;
import br.com.foody.tracking.shared.exception.type.InvalidCredentialsException;
import br.com.foody.tracking.shared.exception.type.InvalidTokenException;
import br.com.foody.tracking.shared.exception.type.UserEmailAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserMapper userMapper;
    private final Clock clock;

    // Matched for unknown e-mails so every failed login costs one BCrypt check (timing).
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

    // Not transactional (nor is the reset): BCrypt must not hold the single SQLite connection.
    public TokenResponseDTO login(LoginRequestDTO request) {
        if (PasswordPolicy.exceedsMaxBytes(request.password())) {
            log.debug("Login rejected: password longer than the BCrypt limit");
            throw new InvalidCredentialsException();
        }
        Optional<User> user =
                userRepository.findByEmail(EmailNormalizer.normalize(request.email()));
        String hash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !passwordMatches) {
            log.warn("Login rejected: invalid credentials");
            throw new InvalidCredentialsException();
        }
        log.info("User {} logged in", user.get().getPublicId());
        return tokenService.issue(user.get());
    }

    public void resetPassword(PasswordResetRequestDTO request) {
        String hash = passwordEncoder.encode(request.newPassword());
        int updated =
                userRepository.updatePasswordHashForUserRole(
                        EmailNormalizer.normalize(request.email()), hash);
        if (updated > 0) {
            log.info("Password reset applied");
        } else {
            log.debug("Password reset no-op");
        }
    }

    @Transactional(readOnly = true)
    public UserResponseDTO currentUser(AuthenticatedUser principal) {
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
