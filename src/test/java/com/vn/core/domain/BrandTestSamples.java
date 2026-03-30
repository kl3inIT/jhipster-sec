package com.vn.core.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class BrandTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Brand getBrandSample1() {
        return new Brand().id(1L).name("name1").description("description1");
    }

    public static Brand getBrandSample2() {
        return new Brand().id(2L).name("name2").description("description2");
    }

    public static Brand getBrandRandomSampleGenerator() {
        return new Brand().id(longCount.incrementAndGet()).name(UUID.randomUUID().toString()).description(UUID.randomUUID().toString());
    }
}
