package com.example.buggy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogicBugsTest {

    private final LogicBugs b = new LogicBugs();

    @Test
    void isAdminWithRuntimeString() { // fails: BUG L2
        assertTrue(b.isAdmin(new String("admin")));
    }

    @Test
    void ratio() { // fails: BUG L3
        assertEquals(0.5, b.ratio(1, 2), 1e-9);
    }

    @Test
    void cleanTrims() { // fails: BUG L7
        assertEquals("x", b.clean("  x  "));
    }

    @Test
    void sum() { // fails (ArrayIndexOutOfBounds): BUG L8
        assertEquals(6, b.sum(new int[] {1, 2, 3}));
    }

    @Test
    void parseOrZero() {
        assertEquals(0, b.parseOrZero("abc"));
        assertEquals(7, b.parseOrZero("7"));
    }

    @Test
    void price() { // fails: BUG L11
        assertEquals(new BigDecimal("0.1"), b.price());
    }

    @Test
    void levelLow() { // fails: BUG L14
        assertEquals("low", b.level(1));
    }

    @Test
    void millisInThirtyDays() { // fails: BUG L19
        assertEquals(2_592_000_000L, b.millisInDays(30));
    }

    @Test
    void removeEmpty() { // fails (ConcurrentModificationException): BUG L22
        List<String> l = new ArrayList<>(List.of("a", "", "b", "c"));
        b.removeEmpty(l);
        assertEquals(List.of("a", "b", "c"), l);
    }

    @Test
    void describe() { // fails (MissingFormatArgumentException): BUG L26
        assertEquals("Bob is 3 years old and lives in ?", b.describe("Bob", 3));
    }

    @Test
    void negativeOdd() { // fails: BUG L28
        assertTrue(b.isOdd(-3));
    }

    @Test
    void sameCountLarge() { // fails: BUG L30
        assertTrue(b.sameCount(1000, 1000));
    }

    @Test
    void fee() {
        assertEquals(10, b.fee(true));
        assertFalse(b.isTenPercent(0.5));
    }
}
