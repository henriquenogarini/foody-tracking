package br.com.foody.tracking.modules.order.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** The single money scale of the order domain: two decimal places, rounded half-up. */
public final class Money {

  private Money() {}

  public static BigDecimal scaled(BigDecimal amount) {
    return amount.setScale(2, RoundingMode.HALF_UP);
  }
}
