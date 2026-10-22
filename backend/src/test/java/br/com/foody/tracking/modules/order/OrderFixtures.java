package br.com.foody.tracking.modules.order;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.order.entity.Customer;
import br.com.foody.tracking.modules.order.entity.DeliveryAddress;
import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.entity.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public final class OrderFixtures {

    public static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
    public static final Instant NOW_INSTANT = Instant.parse("2026-10-21T17:13:27Z");
    public static final OffsetDateTime NOW = OffsetDateTime.ofInstant(NOW_INSTANT, APP_ZONE);

    private OrderFixtures() {}

    public static Customer customer() {
        return new Customer("José da Silva", "+5511987521144", "jose@exemplo.com");
    }

    public static DeliveryAddress address() {
        return new DeliveryAddress(
                "Rua José Neves",
                "590",
                "sala 03",
                "Vila São Paulo",
                "São Paulo",
                "SP",
                "04650-140",
                -23.6578,
                -46.6743);
    }

    public static DeliveryAddress minimalAddress() {
        return new DeliveryAddress(
                "Rua José Neves",
                "590",
                null,
                "Vila São Paulo",
                "São Paulo",
                "SP",
                "04650-140",
                null,
                null);
    }

    public static OrderItem pizza() {
        return new OrderItem("Pizza calabresa G", 1, new BigDecimal("59.90"));
    }

    public static Order newOrder(User owner, long code, OffsetDateTime at) {
        return Order.create(
                owner,
                new Customer("José da Silva", null, null),
                List.of(pizza()),
                minimalAddress(),
                null,
                code,
                at);
    }
}
