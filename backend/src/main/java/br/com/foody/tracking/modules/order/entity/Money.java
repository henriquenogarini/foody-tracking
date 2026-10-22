package br.com.foody.tracking.modules.order.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

  private Money() {}

  public static BigDecimal scaled(BigDecimal amount) {
    return amount.setScale(2, RoundingMode.HALF_UP);
  }
}
