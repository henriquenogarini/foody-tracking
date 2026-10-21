package br.com.foody.tracking.modules.auth.enums;

/**
 * Access level of a {@code User}. Registration always yields {@link #USER}; the single {@link
 * #ADMIN} is provisioned at startup and sees every order.
 */
public enum Role {
  USER,
  ADMIN
}
