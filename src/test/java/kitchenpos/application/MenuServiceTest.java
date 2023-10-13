package kitchenpos.application;

import kitchenpos.dao.MenuGroupDao;
import kitchenpos.dao.ProductDao;
import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuGroup;
import kitchenpos.domain.MenuProduct;
import kitchenpos.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@SpringBootTest
@Transactional
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MenuServiceTest {
    @Autowired
    private MenuService menuService;
    @Autowired
    private MenuGroupDao menuGroupDao;
    @Autowired
    private ProductDao productDao;

    private Long savedMenuGroupId;
    private List<MenuProduct> menuProducts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        final Product product = new Product(null, "상품", BigDecimal.ONE);
        final Product savedProduct = productDao.save(product);
        final MenuGroup menuGroup = new MenuGroup(null, "메뉴 그룹");
        final MenuGroup savedMenuGroup = menuGroupDao.save(menuGroup);
        savedMenuGroupId = savedMenuGroup.getId();
        menuProducts.add(new MenuProduct(null, null, savedProduct.getId(), 2));
    }

    @Nested
    class 메뉴를_등록한다 {
        @Test
        void 메뉴가_정상적으로_등록된다() {
            final Menu menu = new Menu(null, "메뉴", BigDecimal.ONE, savedMenuGroupId, menuProducts);
            final Menu savedMenu = menuService.create(menu);

            assertSoftly(softly -> {
                softly.assertThat(savedMenu.getId()).isNotNull();
                softly.assertThat(savedMenu.getName()).isEqualTo(menu.getName());
                softly.assertThat(savedMenu.getPrice()).isEqualByComparingTo(menu.getPrice());
                softly.assertThat(savedMenu.getMenuGroupId()).isEqualTo(menu.getMenuGroupId());
            });
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 255})
        void 메뉴_이름은_255자_이하이다(int length) {
            final Menu menu = new Menu(null, "메".repeat(length), BigDecimal.ONE, savedMenuGroupId, menuProducts);
            final Menu savedMenu = menuService.create(menu);

            assertSoftly(softly -> {
                softly.assertThat(savedMenu.getId()).isNotNull();
                softly.assertThat(savedMenu.getName()).isEqualTo(menu.getName());
                softly.assertThat(savedMenu.getPrice()).isEqualByComparingTo(menu.getPrice());
                softly.assertThat(savedMenu.getMenuGroupId()).isEqualTo(menu.getMenuGroupId());
            });
        }

        @ParameterizedTest
        @ValueSource(doubles = {0, 1, 999_999_999_999_999.99})
        void 메뉴_가격은_0원_보다_크고_1000조_보다_작다(double price) {
            final Product product = productDao.save(new Product(null, "상품", BigDecimal.valueOf(999_999_999_999_999.99)));
            menuProducts.add(new MenuProduct(null, null, product.getId(), 1));

            final Menu menu = new Menu(null, "메뉴", BigDecimal.valueOf(price), savedMenuGroupId, menuProducts);
            final Menu savedMenu = menuService.create(menu);

            assertSoftly(softly -> {
                softly.assertThat(savedMenu.getId()).isNotNull();
                softly.assertThat(savedMenu.getName()).isEqualTo(menu.getName());
                softly.assertThat(savedMenu.getPrice()).isEqualByComparingTo(menu.getPrice());
                softly.assertThat(savedMenu.getMenuGroupId()).isEqualTo(menu.getMenuGroupId());
            });
        }

        @Test
        void 메뉴_이름이_없으면_예외가_발생한다() {
            final Menu menu = new Menu(null, null, BigDecimal.ONE, savedMenuGroupId, menuProducts);

            assertThatThrownBy(() -> menuService.create(menu))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 메뉴_가격이_없으면_예외가_발생한다() {
            final Menu menu = new Menu(null, "메뉴", null, savedMenuGroupId, menuProducts);

            assertThatThrownBy(() -> menuService.create(menu))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 메뉴_이름이_256자_이상이면_예외가_발생한다() {
            final Menu menu = new Menu(null, "메".repeat(256), BigDecimal.ONE, savedMenuGroupId, menuProducts);

            assertThatThrownBy(() -> menuService.create(menu))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 메뉴_가격이_0원_보다_작으면_예외가_발생한다() {
            final Menu menu = new Menu(null, "메뉴", BigDecimal.valueOf(-1), savedMenuGroupId, menuProducts);

            assertThatThrownBy(() -> menuService.create(menu))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 메뉴_가격이_1000조_이상이면_예외가_발생한다() {
            final Product product = productDao.save(new Product(null, "상품", BigDecimal.valueOf(999_999_999_999_999.99)));
            menuProducts.add(new MenuProduct(null, null, product.getId(), 1));

            final Menu menu = new Menu(null, "메뉴", BigDecimal.valueOf(Math.pow(10, 16)), savedMenuGroupId, menuProducts);

            assertThatThrownBy(() -> menuService.create(menu))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 메뉴_그룹이_없으면_예외가_발생한다() {
            final Menu menu = new Menu(null, "메뉴", BigDecimal.ONE, null, menuProducts);

            assertThatThrownBy(() -> menuService.create(menu))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 메뉴_그룹이_존재하지_않으면_예외가_발생한다() {
            final Menu menu = new Menu(null, "메뉴", BigDecimal.ONE, savedMenuGroupId + 1, menuProducts);

            assertThatThrownBy(() -> menuService.create(menu))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 메뉴_상품이_존재하지_않으면_예외가_발생한다() {
            final Product product = productDao.save(new Product(null, "상품", BigDecimal.ONE));
            product.setId(product.getId() + 1);
            menuProducts.add(new MenuProduct(null, null, product.getId(), 1));

            final Menu menu = new Menu(null, "메뉴", BigDecimal.ZERO, savedMenuGroupId, menuProducts);

            assertThatThrownBy(() -> menuService.create(menu))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void 메뉴의_목록을_조회한다() {
        final List<Menu> expected = menuService.list();
        for (int i = 0; i < 3; i++) {
            final Menu menu = new Menu(null, "메뉴" + i, BigDecimal.ONE, savedMenuGroupId, menuProducts);
            expected.add(menuService.create(menu));
        }

        final List<Menu> result = menuService.list();

        assertThat(result).hasSize(expected.size());
    }
}
