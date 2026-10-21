package br.com.foody.tracking.order.domain;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Who receives the order. Plain data, not a user account: only the name is required; phone and
 * e-mail formats are validated by the request DTO. Column names are set by {@link Order}.
 */
@Embeddable
public class Customer {

  private String name;

  private String phone;

  private String email;

  protected Customer() {}

  public Customer(String name, String phone, String email) {
    this.name = Objects.requireNonNull(name, "name");
    this.phone = phone;
    this.email = email;
  }

  public String getName() {
    return name;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }
}
