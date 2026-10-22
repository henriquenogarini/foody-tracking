package br.com.foody.tracking.modules.auth.helper;

import java.nio.charset.StandardCharsets;

// One password rule for the DTOs, the login pre-check and the admin bootstrap. BCrypt reads only
// 72 bytes and Spring's encoder throws above it, so the upper bound is in UTF-8 bytes.
public final class PasswordPolicy {

  public static final int MIN_LENGTH = 8;
  public static final int MAX_BYTES = 72;

  private PasswordPolicy() {}

  public static boolean exceedsMaxBytes(String password) {
    return password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
  }
}
