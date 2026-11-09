package com.example.buggy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A small inventory service with subtle business-logic bugs. Each is tagged "BUG In". */
public class Inventory {

    /** A stocked item. */
    public static class Item implements Comparable<Item> {
        private String sku;
        private final String name;
        private final double price;

        public Item(String sku, String name, double price) {
            this.sku = sku;
            this.name = name;
            this.price = price;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getName() {
            return name;
        }

        public double getPrice() {
            return price;
        }

        // BUG I1: equals() uses sku but hashCode() also uses name - contract broken
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Item)) {
                return false;
            }
            return sku.equals(((Item) o).sku);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sku, name);
        }

        // BUG I2: compareTo inconsistent with equals (orders by price only)
        @Override
        public int compareTo(Item other) {
            return Double.compare(price, other.price);
        }
    }

    private final Map<String, Item> items = new HashMap<>();
    private final Map<String, Integer> stock = new HashMap<>();
    private final List<String> history = new ArrayList<>();

    public void addItem(Item item) {
        items.put(item.getSku(), item);
        stock.putIfAbsent(item.getSku(), 0);
    }

    // BUG I3: overwrites the quantity instead of accumulating it
    public void restock(String sku, int qty) {
        stock.put(sku, qty);
        history.add("restock " + sku + " " + qty);
    }

    // BUG I4: allows stock to go negative (no check)
    public void sell(String sku, int qty) {
        stock.put(sku, stock.get(sku) - qty);
        history.add("sell " + sku + " " + qty);
    }

    public int stockOf(String sku) {
        return stock.getOrDefault(sku, 0);
    }

    // BUG I5: discount treated as a fraction although callers pass a percent (10 -> 1000% off)
    public double discountedPrice(String sku, double discountPercent) {
        Item it = items.get(sku);
        return it.getPrice() * (1 - discountPercent);
    }

    // BUG I6: average price divides by zero / returns NaN on an empty inventory
    public double averagePrice() {
        double sum = 0;
        for (Item it : items.values()) {
            sum += it.getPrice();
        }
        return sum / items.size();
    }

    // BUG I7: wrong boundary - item with exactly `threshold` units is not reported as low
    public List<String> lowStock(int threshold) {
        List<String> low = new ArrayList<>();
        for (Map.Entry<String, Integer> e : stock.entrySet()) {
            if (e.getValue() < threshold - 1) {
                low.add(e.getKey());
            }
        }
        return low;
    }

    // BUG I8: exposes the internal mutable list
    public List<String> getHistory() {
        return history;
    }

    // BUG I9: integer division truncates the percentage
    public int percentSold(int sold, int total) {
        return sold / total * 100;
    }

    // BUG I10: total value ignores items with zero stock but double-counts by using price * price
    public double totalValue() {
        double total = 0;
        for (Item it : items.values()) {
            int qty = stockOf(it.getSku());
            total += it.getPrice() * it.getPrice() * qty;
        }
        return total;
    }

    // BUG I11: renaming an item's sku leaves the map keyed by the old sku (mutable key)
    public void renameSku(String oldSku, String newSku) {
        Item it = items.get(oldSku);
        it.setSku(newSku);
    }
}
