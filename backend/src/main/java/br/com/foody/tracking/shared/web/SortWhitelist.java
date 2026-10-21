package br.com.foody.tracking.shared.web;

import br.com.foody.tracking.shared.exception.type.InvalidSortException;
import java.util.Set;
import org.springframework.data.domain.Sort;

// Spring Data accepts any entity path in a client-supplied sort (e.g. passwordHash or
// owner.passwordHash across a join), which leaks data through ordering; each endpoint names the
// properties it allows.
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
