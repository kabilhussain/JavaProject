package com.example.buggy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryTest {

    private Inventory inv;

    @BeforeEach
    void setUp() {
        inv = new Inventory();
        inv.addItem(new Inventory.Item("A1", "Apple", 2.0));
        inv.addItem(new Inventory.Item("B2", "Banana", 1.0));
    }

    @Test
    void addItemStartsWithZeroStock() {
        assertEquals(0, inv.stockOf("A1"));
    }

    @Test
    void restockAccumulates() { // fails: BUG I3
        inv.restock("A1", 5);
        inv.restock("A1", 3);
        assertEquals(8, inv.stockOf("A1"));
    }

    @Test
    void cannotSellMoreThanInStock() { // fails: BUG I4
        inv.restock("A1", 1);
        assertThrows(IllegalStateException.class, () -> inv.sell("A1", 5));
    }

    @Test
    void tenPercentDiscount() { // fails: BUG I5
        assertEquals(1.8, inv.discountedPrice("A1", 10), 1e-9);
    }

    @Test
    void averagePrice() {
        assertEquals(1.5, inv.averagePrice(), 1e-9);
    }

    @Test
    void averagePriceOfEmptyInventoryIsZero() { // fails: BUG I6
        assertEquals(0.0, new Inventory().averagePrice(), 1e-9);
    }

    @Test
    void lowStockIncludesThreshold() { // fails: BUG I7
        inv.restock("A1", 5);
        inv.restock("B2", 20);
        assertTrue(inv.lowStock(5).contains("A1"));
    }

    @Test
    void percentSold() { // fails: BUG I9
        assertEquals(25, inv.percentSold(1, 4));
    }

    @Test
    void totalValue() { // fails: BUG I10
        inv.restock("A1", 3);
        assertEquals(6.0, inv.totalValue(), 1e-9);
    }
}
