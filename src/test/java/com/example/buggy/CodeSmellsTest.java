package com.example.buggy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CodeSmellsTest {

    private final CodeSmells c = new CodeSmells();

    @Test
    void invoiceTotal() {
        assertEquals(17.0, c.invoiceTotalA(List.of(5.0, 5.0), 0.2, 5.0), 1e-9);
    }

    @Test
    void scoreAndClassify() {
        assertEquals(4, c.score("A"));
        assertEquals("all", c.classify(1, 1, 1, true, true));
        assertEquals("a,b,", c.joinAll(List.of("a", "b")));
    }
}
