package com.se;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ProductServiceTest {


    @Autowired
    private ProductService productService;

    @Test
    void repeatedLookupsForSameProductAreAllSlow_noCachingYet(){

        int repeat = 10;
        int callCountBefore = productService.getCallCount();
        long start = System.currentTimeMillis();

        for(int i = 0; i < repeat; i++){
            Product product = productService.getById("p1");
            assertThat(product.getName()).isEqualTo("Mechanical keyboard");
        }
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isGreaterThanOrEqualTo(4500);
        assertThat(productService.getCallCount() - callCountBefore).isEqualTo(repeat);
    }
}
