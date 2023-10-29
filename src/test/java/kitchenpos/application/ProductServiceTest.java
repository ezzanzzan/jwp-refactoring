package kitchenpos.application;

import kitchenpos.ServiceTest;
import kitchenpos.product.application.ProductService;
import kitchenpos.product.dto.request.ProductCreateRequest;
import kitchenpos.product.dto.response.ProductResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@ServiceTest
class ProductServiceTest {
    @Autowired
    private ProductService productService;

    @Nested
    class 상품을_등록한다 {
        @Test
        void 상품이_정상적으로_등록된다() {
            final ProductCreateRequest productCreateRequest = new ProductCreateRequest("상품", BigDecimal.valueOf(1000));
            final ProductResponse savedProduct = productService.create(productCreateRequest);

            assertSoftly(softly -> {
                softly.assertThat(savedProduct.getId()).isNotNull();
                softly.assertThat(savedProduct.getName()).isEqualTo("상품");
                softly.assertThat(savedProduct.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(1000));
            });
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 255})
        void 상품_이름은_255자_이하이다(int length) {
            final ProductCreateRequest productCreateRequest = new ProductCreateRequest("상".repeat(length), BigDecimal.valueOf(1000));
            final ProductResponse savedProduct = productService.create(productCreateRequest);

            assertSoftly(softly -> {
                softly.assertThat(savedProduct.getId()).isNotNull();
                softly.assertThat(savedProduct.getName()).isEqualTo("상".repeat(length));
                softly.assertThat(savedProduct.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(1000));
            });
        }

        @Test
        void 상품_이름이_없으면_예외가_발생한다() {
            final ProductCreateRequest productCreateRequest = new ProductCreateRequest(null, BigDecimal.valueOf(1000));

            assertThatThrownBy(() -> productService.create(productCreateRequest))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 상품_가격이_없으면_예외가_발생한다() {
            final ProductCreateRequest productCreateRequest = new ProductCreateRequest("상품", null);

            assertThatThrownBy(() -> productService.create(productCreateRequest))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 상품_이름이_256자_이상이면_예외가_발생한다() {
            final ProductCreateRequest productCreateRequest = new ProductCreateRequest("상".repeat(256), BigDecimal.valueOf(1000));

            assertThatThrownBy(() -> productService.create(productCreateRequest))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 상품_가격이_0원_이하이면_예외가_발생한다() {
            final ProductCreateRequest productCreateRequest = new ProductCreateRequest("상품", BigDecimal.valueOf(-1));

            assertThatThrownBy(() -> productService.create(productCreateRequest))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 상품_가격이_1000조_이상이면_예외가_발생한다() {
            final ProductCreateRequest productCreateRequest = new ProductCreateRequest("상품", BigDecimal.valueOf(Math.pow(10, 17)));

            assertThatThrownBy(() -> productService.create(productCreateRequest))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void 상품의_목록을_조회한다() {
        final List<ProductResponse> expected = productService.list();
        for (int i = 0; i < 3; i++) {
            final ProductCreateRequest productCreateRequest = new ProductCreateRequest("상품" + i, BigDecimal.valueOf(1000));
            expected.add(productService.create(productCreateRequest));
        }

        final List<ProductResponse> result = productService.list();

        assertSoftly(softly -> {
            softly.assertThat(result).hasSize(expected.size());
            softly.assertThat(result)
                    .usingRecursiveComparison()
                    .comparingOnlyFields("name")
                    .isEqualTo(expected);
        });
    }
}
