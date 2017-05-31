package com.westernacher;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JsonComparatorTest {

    @Test
    public void testComparingEmptyObjects() {
        assertTrue(JsonComparator.compareJson("{}", "{}"));
    }

}
