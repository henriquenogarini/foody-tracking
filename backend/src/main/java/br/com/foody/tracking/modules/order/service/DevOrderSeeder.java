package br.com.foody.tracking.modules.order.service;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.helper.EmailNormalizer;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import br.com.foody.tracking.modules.auth.service.AdminBootstrap;
import br.com.foody.tracking.modules.order.entity.Customer;
import br.com.foody.tracking.modules.order.entity.DeliveryAddress;
import br.com.foody.tracking.modules.order.entity.Order;
import br.com.foody.tracking.modules.order.entity.OrderItem;
import br.com.foody.tracking.modules.order.enums.OrderStatus;
import br.com.foody.tracking.modules.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Dev-profile demo data owned by the bootstrap admin, at least one order per status, built through
// the domain so every history row is legal. Seeds only while the admin owns no order.
@Slf4j
@Component
@Profile("dev")
@org.springframework.core.annotation.Order(AdminBootstrap.RUNNER_ORDER + 1)
public class DevOrderSeeder implements ApplicationRunner {

  private final String adminEmail;
  private final UserRepository userRepository;
  private final OrderRepository orderRepository;
  private final Clock clock;

  public DevOrderSeeder(
      @Value("${app.security.admin.email:}") String adminEmail,
      UserRepository userRepository,
      OrderRepository orderRepository,
      Clock clock) {
    this.adminEmail = adminEmail;
    this.userRepository = userRepository;
    this.orderRepository = orderRepository;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Optional<User> admin =
        adminEmail.isBlank()
            ? Optional.empty()
            : userRepository
                .findByEmail(EmailNormalizer.normalize(adminEmail))
                .filter(user -> user.getRole() == Role.ADMIN);
    if (admin.isEmpty()) {
      log.info("No bootstrap admin; skipping demo orders");
      return;
    }
    User owner = admin.get();
    if (orderRepository.existsByOwnerPublicId(owner.getPublicId())) {
      log.info("Demo orders already present; skipping");
      return;
    }
    OffsetDateTime now = OffsetDateTime.now(clock);
    List<DemoOrder> demoOrders = demoOrders();
    for (DemoOrder demo : demoOrders) {
      Order order =
          Order.create(
              owner,
              demo.customer(),
              demo.items(),
              demo.address(),
              demo.notes(),
              orderRepository.nextCode(),
              now.minus(demo.createdAgo()));
      for (Step step : demo.steps()) {
        order.changeStatus(step.target(), owner.getPublicId(), now.minus(step.ago()));
      }
      // Flushed one by one: the next nextCode() must see this row.
      orderRepository.saveAndFlush(order);
    }
    log.info("Seeded {} demo orders", demoOrders.size());
  }

  private record Step(OrderStatus target, Duration ago) {}

  private record DemoOrder(
      Customer customer,
      List<OrderItem> items,
      DeliveryAddress address,
      String notes,
      Duration createdAgo,
      List<Step> steps) {}

  private static List<DemoOrder> demoOrders() {
    return List.of(
        new DemoOrder(
            new Customer("José da Silva", "+5511987521144", "jose@exemplo.com"),
            List.of(item("Pizza calabresa G", 1, "59.90"), item("Refrigerante 2 L", 1, "12.00")),
            address(
                "Rua José Neves",
                "590",
                "sala 03",
                "Vila São Paulo",
                "04650-140",
                -23.6578,
                -46.6743),
            "sem cebola",
            Duration.ofMinutes(5),
            List.of()),
        new DemoOrder(
            new Customer("Mariana Costa", "+5511991234567", null),
            List.of(item("Temaki salmão", 2, "32.50")),
            address(
                "Alameda dos Maracatins",
                "1217",
                "apto 82",
                "Moema",
                "04089-014",
                -23.6003,
                -46.6655),
            null,
            Duration.ofMinutes(20),
            List.of()),
        new DemoOrder(
            new Customer("Carlos Pereira", "+5511976543210", "carlos@exemplo.com"),
            List.of(item("Hambúrguer artesanal", 2, "38.00"), item("Batata frita", 1, "18.50")),
            address("Rua dos Pinheiros", "870", null, "Pinheiros", "05422-001", null, null),
            "ponto da carne: mal passado",
            Duration.ofMinutes(45),
            List.of(new Step(OrderStatus.EM_PREPARO, Duration.ofMinutes(35)))),
        new DemoOrder(
            new Customer("Ana Beatriz Lima", "+5511965432109", null),
            List.of(item("Yakisoba de frango", 1, "44.90")),
            address(
                "Rua Domingos de Morais",
                "2187",
                "bloco B",
                "Vila Mariana",
                "04035-000",
                -23.5891,
                -46.6346),
            null,
            Duration.ofMinutes(70),
            List.of(new Step(OrderStatus.EM_PREPARO, Duration.ofMinutes(60)))),
        new DemoOrder(
            new Customer("Rafael Souza", "+5511954321098", "rafael@exemplo.com"),
            List.of(item("Pizza margherita G", 1, "54.90"), item("Pizza doce M", 1, "42.00")),
            address("Avenida Ibirapuera", "3103", null, "Moema", "04029-902", -23.6104, -46.6668),
            "troco para 100",
            Duration.ofHours(2),
            List.of(
                new Step(OrderStatus.EM_PREPARO, Duration.ofMinutes(110)),
                new Step(OrderStatus.SAIU_PARA_ENTREGA, Duration.ofMinutes(80)))),
        new DemoOrder(
            new Customer("Fernanda Oliveira", "+5511943210987", null),
            List.of(item("Açaí 500 ml", 2, "24.00")),
            address("Rua Teodoro Sampaio", "1020", "casa 2", "Pinheiros", "05406-050", null, null),
            null,
            Duration.ofHours(26),
            List.of(
                new Step(OrderStatus.EM_PREPARO, Duration.ofHours(26).minusMinutes(10)),
                new Step(OrderStatus.SAIU_PARA_ENTREGA, Duration.ofHours(25).plusMinutes(20)),
                new Step(OrderStatus.ENTREGUE, Duration.ofHours(25)))),
        new DemoOrder(
            new Customer("Lucas Martins", "+5511932109876", "lucas@exemplo.com"),
            List.of(item("Marmita executiva", 3, "27.90")),
            address("Rua Vergueiro", "3185", null, "Vila Mariana", "04101-300", -23.5880, -46.6340),
            "entregar na portaria",
            Duration.ofDays(3),
            List.of(
                new Step(OrderStatus.EM_PREPARO, Duration.ofDays(3).minusMinutes(15)),
                new Step(OrderStatus.SAIU_PARA_ENTREGA, Duration.ofDays(3).minusMinutes(40)),
                new Step(OrderStatus.ENTREGUE, Duration.ofDays(3).minusMinutes(65)))),
        new DemoOrder(
            new Customer("Patrícia Rocha", "+5511921098765", null),
            List.of(item("Combo sushi 30 peças", 1, "119.90")),
            address(
                "Rua Doutor Mário Cardim", "301", null, "Vila Mariana", "04019-000", null, null),
            "cliente desistiu",
            Duration.ofDays(2),
            List.of(
                new Step(OrderStatus.EM_PREPARO, Duration.ofDays(2).minusMinutes(5)),
                new Step(OrderStatus.CANCELADO, Duration.ofDays(2).minusMinutes(25)))));
  }

  private static OrderItem item(String description, int quantity, String unitPrice) {
    return new OrderItem(description, quantity, new BigDecimal(unitPrice));
  }

  private static DeliveryAddress address(
      String street,
      String number,
      String complement,
      String neighborhood,
      String postalCode,
      Double lat,
      Double lng) {
    return new DeliveryAddress(
        street, number, complement, neighborhood, "São Paulo", "SP", postalCode, lat, lng);
  }
}
