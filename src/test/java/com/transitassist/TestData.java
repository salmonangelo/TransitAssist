package com.transitassist;

import java.util.UUID;

/** Small helpers shared by the tests. */
final class TestData {

    private TestData() {
    }

    static String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
