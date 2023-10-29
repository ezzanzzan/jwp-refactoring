package kitchenpos.product.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
public class ProductPrice {
    private static final BigDecimal MIN_PRICE = BigDecimal.ZERO;
    private static final BigDecimal MAX_PRICE = BigDecimal.valueOf(Math.pow(10, 17));

    @Column(nullable = false)
    private BigDecimal price;

    protected ProductPrice() {
    }

    public ProductPrice(final BigDecimal price) {
        validate(price);
        this.price = price;
    }

    private void validate(final BigDecimal price) {
        if (Objects.isNull(price) || isLessThanMinPrice(price) || isMoreThanMaxPrice(price)) {
            throw new IllegalArgumentException();
        }
    }

    private boolean isMoreThanMaxPrice(final BigDecimal price) {
        return price.compareTo(MAX_PRICE) >= 0;
    }

    private boolean isLessThanMinPrice(final BigDecimal price) {
        return price.compareTo(MIN_PRICE) < 0;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
