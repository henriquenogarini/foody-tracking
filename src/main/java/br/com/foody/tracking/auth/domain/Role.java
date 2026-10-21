package br.com.foody.tracking.auth.domain;

/**
 * Access level of a {@link User}. Registration always yields {@link #USER}; the single {@link
 * #ADMIN} is provisioned at startup and sees every order.
 */
public enum Role {
  USER,
  ADMIN
}
