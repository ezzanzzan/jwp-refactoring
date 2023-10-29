package kitchenpos.application;

import kitchenpos.ServiceTest;
import kitchenpos.menu.domain.Menu;
import kitchenpos.menu.domain.MenuName;
import kitchenpos.menu.domain.MenuPrice;
import kitchenpos.menu.domain.MenuProduct;
import kitchenpos.menu.domain.MenuProductQuantity;
import kitchenpos.menu.domain.repository.MenuProductRepository;
import kitchenpos.menu.domain.repository.MenuRepository;
import kitchenpos.menugroup.domain.MenuGroup;
import kitchenpos.menugroup.domain.MenuGroupName;
import kitchenpos.menugroup.domain.repository.MenuGroupRepository;
import kitchenpos.order.application.OrderService;
import kitchenpos.order.domain.Order;
import kitchenpos.order.domain.OrderStatus;
import kitchenpos.order.domain.repository.OrderRepository;
import kitchenpos.order.dto.request.OrderChangeStatusRequest;
import kitchenpos.order.dto.request.OrderCreateRequest;
import kitchenpos.order.dto.request.OrderLineItemCreateRequest;
import kitchenpos.order.dto.response.OrderResponse;
import kitchenpos.product.domain.Product;
import kitchenpos.product.domain.ProductName;
import kitchenpos.product.domain.ProductPrice;
import kitchenpos.product.domain.repository.ProductRepository;
import kitchenpos.table.domain.OrderTable;
import kitchenpos.table.domain.OrderTableNumberOfGuests;
import kitchenpos.table.domain.repository.OrderTableRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@ServiceTest
class OrderServiceTest {
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderTableRepository orderTableRepository;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private MenuProductRepository menuProductRepository;
    @Autowired
    private MenuGroupRepository menuGroupRepository;
    @Autowired
    private ProductRepository productRepository;

    private List<OrderLineItemCreateRequest> orderLineItems = new ArrayList<>();
    private OrderTable savedOrderTable;

    @BeforeEach
    void setUp() {
        final MenuGroup savedMenuGroup = menuGroupRepository.save(new MenuGroup(new MenuGroupName("메뉴 그룹")));
        final Product savedProduct = productRepository.save(new Product(new ProductName("상품"), new ProductPrice(BigDecimal.ONE)));
        final Menu savedMenu = menuRepository.save(new Menu(new MenuName("메뉴"), new MenuPrice(BigDecimal.ONE), savedMenuGroup));
        final MenuProduct savedMenuProduct = menuProductRepository.save(new MenuProduct(new MenuProductQuantity(2), savedMenu, savedProduct));
        orderLineItems.add(new OrderLineItemCreateRequest(savedMenu.getId(), 1));
        savedOrderTable = orderTableRepository.save(new OrderTable(null, new OrderTableNumberOfGuests(5), false));
    }

    @Nested
    class 주문을_등록한다 {
        @Test
        void 주문이_정상적으로_등록된다() {
            final OrderCreateRequest request = new OrderCreateRequest(savedOrderTable.getId(), orderLineItems);
            final OrderResponse response = orderService.create(request);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getId()).isNotNull();
                softly.assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.COOKING.name());
                softly.assertThat(response.getOrderedTime()).isNotNull();
            });
        }

        @Test
        void 존재하지_않는_테이블에_주문이_등록되면_예외가_발생한다() {
            final OrderCreateRequest request = new OrderCreateRequest(savedOrderTable.getId() + 1, orderLineItems);

            assertThatThrownBy(() -> orderService.create(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 비어있는_상태의_테이블에_주문이_등록되면_예외가_발생한다() {
            savedOrderTable.updateEmpty(true);
            orderTableRepository.save(savedOrderTable);

            final OrderCreateRequest request = new OrderCreateRequest(savedOrderTable.getId(), orderLineItems);

            assertThatThrownBy(() -> orderService.create(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 주문_항목들이_비어있으면_예외가_발생한다() {
            final OrderCreateRequest request = new OrderCreateRequest(savedOrderTable.getId(), List.of());

            assertThatThrownBy(() -> orderService.create(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 주문_항목들에_메뉴가_중복되면_예외가_발생한다() {
            orderLineItems.add(orderLineItems.get(0));
            final OrderCreateRequest request = new OrderCreateRequest(savedOrderTable.getId(), orderLineItems);

            assertThatThrownBy(() -> orderService.create(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 주문_항목이_존재하지_않으면_예외가_발생한다() {
            orderLineItems.add(new OrderLineItemCreateRequest(orderLineItems.get(0).getMenuId() + 1, 1));
            final OrderCreateRequest request = new OrderCreateRequest(savedOrderTable.getId(), orderLineItems);

            assertThatThrownBy(() -> orderService.create(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void 주문의_목록을_조회한다() {
        final List<OrderResponse> expected = orderService.list();
        for (int i = 0; i < 3; i++) {
            final OrderCreateRequest request = new OrderCreateRequest(savedOrderTable.getId(), orderLineItems);
            expected.add(orderService.create(request));
        }

        final List<OrderResponse> result = orderService.list();

        assertSoftly(softly -> {
            softly.assertThat(result).hasSize(expected.size());
            softly.assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("id")
                    .isEqualTo(expected);
        });
    }

    @Nested
    class 주문의_상태를_변경한다 {
        @ParameterizedTest
        @ValueSource(strings = {"MEAL", "COMPLETION"})
        void 주문의_상태를_정상적으로_변경한다(final String status) {
            final OrderCreateRequest request = new OrderCreateRequest(savedOrderTable.getId(), orderLineItems);
            final OrderResponse response = orderService.create(request);
            final OrderChangeStatusRequest changeStatusRequest = new OrderChangeStatusRequest(status);

            orderService.changeOrderStatus(response.getId(), changeStatusRequest);
            final Order changeOrder = orderRepository.findById(response.getId())
                    .orElseThrow(IllegalArgumentException::new);

            assertThat(changeOrder.getOrderStatus().name()).isEqualTo(status);
        }

        @Test
        void 주문이_존재하지_않으면_예외가_발생한다() {
            final OrderCreateRequest request = new OrderCreateRequest(savedOrderTable.getId(), orderLineItems);
            final OrderResponse response = orderService.create(request);

            final OrderChangeStatusRequest changeStatusRequest = new OrderChangeStatusRequest("MEAL");
            assertThatThrownBy(() -> orderService.changeOrderStatus(response.getId() + 1, changeStatusRequest))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
