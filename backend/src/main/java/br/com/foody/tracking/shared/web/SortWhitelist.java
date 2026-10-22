package br.com.foody.tracking.shared.web;

import br.com.foody.tracking.shared.exception.type.InvalidSortException;
import java.util.Set;
import org.springframework.data.domain.Sort;

public final class SortWhitelist {

  private SortWhitelist() {}

  public static void requireAllowed(Sort sort, Set<String> allowed) {
    for (Sort.Order order : sort) {
      if (!allowed.contains(order.getProperty())) {
        throw new InvalidSortException(order.getProperty());
      }
    }
  }
}
