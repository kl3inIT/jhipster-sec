package com.vn.core.domain;

import static com.vn.core.domain.BrandTestSamples.*;
import static com.vn.core.domain.ShoeTestSamples.*;
import static com.vn.core.domain.ShoeVariantTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.vn.core.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShoeTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Shoe.class);
        Shoe shoe1 = getShoeSample1();
        Shoe shoe2 = new Shoe();
        assertThat(shoe1).isNotEqualTo(shoe2);

        shoe2.setId(shoe1.getId());
        assertThat(shoe1).isEqualTo(shoe2);

        shoe2 = getShoeSample2();
        assertThat(shoe1).isNotEqualTo(shoe2);
    }

    @Test
    void shoeVariantTest() {
        Shoe shoe = getShoeRandomSampleGenerator();
        ShoeVariant shoeVariantBack = getShoeVariantRandomSampleGenerator();

        shoe.addShoeVariant(shoeVariantBack);
        assertThat(shoe.getShoeVariants()).containsOnly(shoeVariantBack);
        assertThat(shoeVariantBack.getShoe()).isEqualTo(shoe);

        shoe.removeShoeVariant(shoeVariantBack);
        assertThat(shoe.getShoeVariants()).doesNotContain(shoeVariantBack);
        assertThat(shoeVariantBack.getShoe()).isNull();

        shoe.shoeVariants(new HashSet<>(Set.of(shoeVariantBack)));
        assertThat(shoe.getShoeVariants()).containsOnly(shoeVariantBack);
        assertThat(shoeVariantBack.getShoe()).isEqualTo(shoe);

        shoe.setShoeVariants(new HashSet<>());
        assertThat(shoe.getShoeVariants()).doesNotContain(shoeVariantBack);
        assertThat(shoeVariantBack.getShoe()).isNull();
    }

    @Test
    void brandTest() {
        Shoe shoe = getShoeRandomSampleGenerator();
        Brand brandBack = getBrandRandomSampleGenerator();

        shoe.setBrand(brandBack);
        assertThat(shoe.getBrand()).isEqualTo(brandBack);

        shoe.brand(null);
        assertThat(shoe.getBrand()).isNull();
    }
}
