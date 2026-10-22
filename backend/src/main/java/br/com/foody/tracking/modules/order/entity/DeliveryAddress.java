package br.com.foody.tracking.modules.order.entity;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAddress {

  private String street;

  private String number;

  private String complement;

  private String neighborhood;

  private String city;

  private String state;

  private String postalCode;

  private Double lat;

  private Double lng;

  public DeliveryAddress(
      String street,
      String number,
      String complement,
      String neighborhood,
      String city,
      String state,
      String postalCode,
      Double lat,
      Double lng) {
    if ((lat == null) != (lng == null)) {
      throw new IllegalArgumentException("lat and lng must be both present or both absent");
    }
    this.street = Objects.requireNonNull(street, "street");
    this.number = Objects.requireNonNull(number, "number");
    this.complement = complement;
    this.neighborhood = Objects.requireNonNull(neighborhood, "neighborhood");
    this.city = Objects.requireNonNull(city, "city");
    this.state = Objects.requireNonNull(state, "state");
    this.postalCode = Objects.requireNonNull(postalCode, "postalCode");
    this.lat = lat;
    this.lng = lng;
  }
}
