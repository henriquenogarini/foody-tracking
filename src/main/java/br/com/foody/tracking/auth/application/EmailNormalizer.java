package br.com.foody.tracking.auth.application;

import java.util.Locale;

// One normalization for register, login and the admin bootstrap, so the same address can never
// be stored or looked up in two shapes.
final class EmailNormalizer {

  private EmailNormalizer() {}

  static String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
