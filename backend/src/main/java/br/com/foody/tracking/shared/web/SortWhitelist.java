package br.com.foody.tracking.shared.web;

import br.com.foody.tracking.shared.exception.type.InvalidSortException;
import org.springframework.data.domain.Sort;

import java.util.Set;

// Spring Data sorts by any entity path (even owner.passwordHash), leaking data through ordering.
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
