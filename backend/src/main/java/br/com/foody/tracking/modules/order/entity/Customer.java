package br.com.foody.tracking.modules.order.entity;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Who receives the order. Plain data, not a user account: only the name is required; phone and
 * e-mail formats are validated by the request DTO. Column names are set by {@link Order}.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

  private String name;

  private String phone;

  private String email;

  public Customer(String name, String phone, String email) {
    this.name = Objects.requireNonNull(name, "name");
    this.phone = phone;
    this.email = email;
  }
}
