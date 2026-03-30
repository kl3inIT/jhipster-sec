package com.vn.core.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ShoeVariantTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static ShoeVariant getShoeVariantSample1() {
        return new ShoeVariant().id(1L).name("name1").decription("decription1");
    }

    public static ShoeVariant getShoeVariantSample2() {
        return new ShoeVariant().id(2L).name("name2").decription("decription2");
    }

    public static ShoeVariant getShoeVariantRandomSampleGenerator() {
        return new ShoeVariant()
            .id(longCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .decription(UUID.randomUUID().toString());
    }
}
