package br.com.foody.tracking.modules.auth.service;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.helper.EmailNormalizer;
import br.com.foody.tracking.modules.auth.helper.PasswordPolicy;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Provisions the single ADMIN at startup; the API never creates or promotes one.
@Slf4j
@Component
@Order(AdminBootstrap.RUNNER_ORDER)
public final class AdminBootstrap implements ApplicationRunner {

  public static final int RUNNER_ORDER = 0;

  private static final String ADMIN_NAME = "Admin";

  private final String adminEmail;
  private final String adminPassword;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  public AdminBootstrap(
      @Value("${app.security.admin.email:}") String adminEmail,
      @Value("${app.security.admin.password:}") String adminPassword,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    this.adminEmail = adminEmail;
    this.adminPassword = adminPassword;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    // application.yml defaults both to empty, so blank means absent.
    boolean hasEmail = adminEmail != null && !adminEmail.isBlank();
    boolean hasPassword = adminPassword != null && !adminPassword.isBlank();
    if (!hasEmail && !hasPassword) {
      log.warn(
          "No bootstrap admin configured (APP_SECURITY_ADMIN_EMAIL/PASSWORD absent); skipping");
      return;
    }
    if (hasEmail != hasPassword) {
      throw new IllegalStateException(
          "APP_SECURITY_ADMIN_EMAIL and APP_SECURITY_ADMIN_PASSWORD must be set together");
    }
    if (adminPassword.length() < PasswordPolicy.MIN_LENGTH
        || PasswordPolicy.exceedsMaxBytes(adminPassword)) {
      throw new IllegalStateException(
          "APP_SECURITY_ADMIN_PASSWORD must have at least 8 characters and at most 72 bytes");
    }

    String normalizedEmail = EmailNormalizer.normalize(adminEmail);
    Optional<User> existing = userRepository.findByEmail(normalizedEmail);
    if (existing.isPresent()) {
      User user = existing.get();
      // Never promote: anyone could have pre-registered the admin e-mail through the API.
      if (user.getRole() == Role.ADMIN) {
        log.info("Bootstrap admin {} already exists", user.getPublicId());
      } else {
        log.warn("Bootstrap admin e-mail belongs to USER {}; not promoting it", user.getPublicId());
      }
      return;
    }

    User admin =
        userRepository.save(
            new User(
                ADMIN_NAME,
                normalizedEmail,
                passwordEncoder.encode(adminPassword),
                Role.ADMIN,
                OffsetDateTime.now(clock)));
    log.info("Bootstrap admin {} created", admin.getPublicId());
  }
}
