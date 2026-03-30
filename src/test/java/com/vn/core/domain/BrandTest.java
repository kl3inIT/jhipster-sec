package com.vn.core.domain;

import static com.vn.core.domain.BrandTestSamples.*;
import static com.vn.core.domain.ShoeTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.vn.core.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BrandTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Brand.class);
        Brand brand1 = getBrandSample1();
        Brand brand2 = new Brand();
        assertThat(brand1).isNotEqualTo(brand2);

        brand2.setId(brand1.getId());
        assertThat(brand1).isEqualTo(brand2);

        brand2 = getBrandSample2();
        assertThat(brand1).isNotEqualTo(brand2);
    }

    @Test
    void shoeTest() {
        Brand brand = getBrandRandomSampleGenerator();
        Shoe shoeBack = getShoeRandomSampleGenerator();

        brand.addShoe(shoeBack);
        assertThat(brand.getShoes()).containsOnly(shoeBack);
        assertThat(shoeBack.getBrand()).isEqualTo(brand);

        brand.removeShoe(shoeBack);
        assertThat(brand.getShoes()).doesNotContain(shoeBack);
        assertThat(shoeBack.getBrand()).isNull();

        brand.shoes(new HashSet<>(Set.of(shoeBack)));
        assertThat(brand.getShoes()).containsOnly(shoeBack);
        assertThat(shoeBack.getBrand()).isEqualTo(brand);

        brand.setShoes(new HashSet<>());
        assertThat(brand.getShoes()).doesNotContain(shoeBack);
        assertThat(shoeBack.getBrand()).isNull();
    }
}
