package com.vn.core.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ShoeTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Shoe getShoeSample1() {
        return new Shoe().id(1L).name("name1").decription("decription1");
    }

    public static Shoe getShoeSample2() {
        return new Shoe().id(2L).name("name2").decription("decription2");
    }

    public static Shoe getShoeRandomSampleGenerator() {
        return new Shoe().id(longCount.incrementAndGet()).name(UUID.randomUUID().toString()).decription(UUID.randomUUID().toString());
    }
}
