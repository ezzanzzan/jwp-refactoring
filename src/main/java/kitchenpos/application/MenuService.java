package kitchenpos.application;

import kitchenpos.dao.MenuDao;
import kitchenpos.dao.MenuProductDao;
import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuProduct;
import kitchenpos.domain.Product;
import kitchenpos.dto.request.MenuProductRequest;
import kitchenpos.dto.request.MenuRequest;
import kitchenpos.repository.MenuGroupRepository;
import kitchenpos.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MenuService {
    private final MenuDao menuDao;
    private final MenuGroupRepository menuGroupRepository;
    private final MenuProductDao menuProductDao;
    private final ProductRepository productRepository;

    public MenuService(
            final MenuDao menuDao,
            final MenuGroupRepository menuGroupRepository,
            final MenuProductDao menuProductDao,
            final ProductRepository productRepository
    ) {
        this.menuDao = menuDao;
        this.menuGroupRepository = menuGroupRepository;
        this.menuProductDao = menuProductDao;
        this.productRepository = productRepository;
    }

    @Transactional
    public Menu create(final MenuRequest menu) {
        final String name = menu.getName();
        final BigDecimal price = menu.getPrice();

        if (Objects.isNull(name) || name.length() > 255) {
            throw new IllegalArgumentException();
        }

        if (Objects.isNull(price)
                || price.compareTo(BigDecimal.ZERO) < 0
                || price.compareTo(BigDecimal.valueOf(Math.pow(10, 17))) >= 0) {
            throw new IllegalArgumentException();
        }

        if (Objects.isNull(menu.getMenuGroupId())) {
            throw new IllegalArgumentException();
        }

        if (!menuGroupRepository.existsById(menu.getMenuGroupId())) {
            throw new IllegalArgumentException();
        }

        final List<MenuProductRequest> menuProducts = menu.getMenuProducts();

        BigDecimal sum = BigDecimal.ZERO;
        for (final MenuProductRequest menuProduct : menuProducts) {
            final Product product = productRepository.findById(menuProduct.getProductId())
                    .orElseThrow(IllegalArgumentException::new);
            sum = sum.add(product.getPrice().multiply(BigDecimal.valueOf(menuProduct.getQuantity())));
        }

        if (price.compareTo(sum) > 0) {
            throw new IllegalArgumentException();
        }

        final Menu savedMenu = menuDao.save(
                new Menu(
                        null,
                        menu.getName(),
                        menu.getPrice(),
                        menu.getMenuGroupId(),
                        menu.getMenuProducts().stream()
                                .map(menuProductRequest -> new MenuProduct(
                                        null,
                                        null,
                                        menuProductRequest.getProductId(),
                                        menuProductRequest.getQuantity())
                                )
                                .collect(Collectors.toList())
                )
        );

        final Long menuId = savedMenu.getId();
        final List<MenuProduct> savedMenuProducts = new ArrayList<>();
        for (final MenuProductRequest menuProductRequest : menuProducts) {
            final MenuProduct menuProduct = new MenuProduct(null, null, menuProductRequest.getProductId(), menuProductRequest.getQuantity());
            menuProduct.setMenuId(menuId);
            savedMenuProducts.add(menuProductDao.save(menuProduct));
        }
        savedMenu.setMenuProducts(savedMenuProducts);

        return savedMenu;
    }

    public List<Menu> list() {
        final List<Menu> menus = menuDao.findAll();

        for (final Menu menu : menus) {
            menu.setMenuProducts(menuProductDao.findAllByMenuId(menu.getId()));
        }

        return menus;
    }
}
