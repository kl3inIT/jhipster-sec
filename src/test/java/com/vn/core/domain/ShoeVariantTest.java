package com.vn.core.domain;

import static com.vn.core.domain.ShoeTestSamples.*;
import static com.vn.core.domain.ShoeVariantTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.vn.core.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ShoeVariantTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ShoeVariant.class);
        ShoeVariant shoeVariant1 = getShoeVariantSample1();
        ShoeVariant shoeVariant2 = new ShoeVariant();
        assertThat(shoeVariant1).isNotEqualTo(shoeVariant2);

        shoeVariant2.setId(shoeVariant1.getId());
        assertThat(shoeVariant1).isEqualTo(shoeVariant2);

        shoeVariant2 = getShoeVariantSample2();
        assertThat(shoeVariant1).isNotEqualTo(shoeVariant2);
    }

    @Test
    void shoeTest() {
        ShoeVariant shoeVariant = getShoeVariantRandomSampleGenerator();
        Shoe shoeBack = getShoeRandomSampleGenerator();

        shoeVariant.setShoe(shoeBack);
        assertThat(shoeVariant.getShoe()).isEqualTo(shoeBack);

        shoeVariant.shoe(null);
        assertThat(shoeVariant.getShoe()).isNull();
    }
}
