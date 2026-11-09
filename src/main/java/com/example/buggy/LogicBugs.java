package com.example.buggy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/** Reliability / runtime bugs. Each planted bug is tagged "BUG Ln". */
public class LogicBugs {

    // BUG L1: static SimpleDateFormat shared between threads (not thread-safe)
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd");

    private static LogicBugs instance;
    private final Random random = new Random();
    private int counter;
    private final Object lock = new Object();

    // BUG L2: strings compared with ==
    public boolean isAdmin(String role) {
        return role == "admin";
    }

    // BUG L3: integer division assigned to double
    public double ratio(int part, int total) {
        double r = part / total;
        return r;
    }

    // BUG L4: null dereference on the null branch
    public int safeLength(String s) {
        if (s == null) {
            return s.length();
        }
        return s.length();
    }

    // BUG L5: equals() between unrelated types - always false
    public boolean sameId(String id, Integer other) {
        return id.equals(other);
    }

    // BUG L6: resource leak - stream never closed
    public int firstByte(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        return in.read();
    }

    // BUG L7: return value of String.trim() ignored
    public String clean(String s) {
        s.trim();
        return s;
    }

    // BUG L8: off-by-one - reads past the end of the array
    public int sum(int[] values) {
        int total = 0;
        for (int i = 0; i <= values.length; i++) {
            total += values[i];
        }
        return total;
    }

    // BUG L9: exception swallowed by empty catch block
    public int parseOrZero(String s) {
        int v = 0;
        try {
            v = Integer.parseInt(s);
        } catch (NumberFormatException e) {
        }
        return v;
    }

    // BUG L10: catching NullPointerException instead of checking for null
    public String upper(String s) {
        try {
            return s.toUpperCase();
        } catch (NullPointerException e) {
            return "";
        }
    }

    // BUG L11: BigDecimal(double) gives an inexact value
    public BigDecimal price() {
        return new BigDecimal(0.1);
    }

    // BUG L12: floating point equality
    public boolean isTenPercent(double d) {
        return d * 3 == 0.3;
    }

    // BUG L13: self-assignment - field is never updated
    public void setCounter(int count) {
        counter = counter;
    }

    // BUG L14: missing break - switch falls through
    public String level(int code) {
        String result;
        switch (code) {
            case 1:
                result = "low";
            case 2:
                result = "medium";
                break;
            default:
                result = "high";
        }
        return result;
    }

    // BUG L15: Thread.run() called instead of start()
    public void runAsync(Runnable r) {
        new Thread(r).run();
    }

    private final Boolean flag = Boolean.TRUE;

    // BUG L16: synchronizing on a boxed / interned value
    public void increment() {
        synchronized (flag) {
            counter++;
        }
    }

    // BUG L17: wait() not in a loop
    public void await() throws InterruptedException {
        synchronized (lock) {
            lock.wait();
        }
    }

    // BUG L18: double-checked locking on a non-volatile field
    public static LogicBugs getInstance() {
        if (instance == null) {
            synchronized (LogicBugs.class) {
                if (instance == null) {
                    instance = new LogicBugs();
                }
            }
        }
        return instance;
    }

    // BUG L19: int overflow before widening to long
    public long millisInDays(int days) {
        long ms = days * 24 * 60 * 60 * 1000;
        return ms;
    }

    // BUG L20: Math.abs(random int) can still be negative
    public int bucket(int n) {
        return Math.abs(random.nextInt()) % n;
    }

    // BUG L21: compareTo by subtraction overflows
    public int compareAges(int a, int b) {
        return a - b;
    }

    // BUG L22: removing from a list while iterating it (ConcurrentModificationException)
    public void removeEmpty(List<String> items) {
        for (String s : items) {
            if (s.isEmpty()) {
                items.remove(s);
            }
        }
    }

    // BUG L23: Optional.get() without isPresent()
    public String firstName(Optional<String> name) {
        return name.get();
    }

    // BUG L24: return inside finally hides exceptions
    @SuppressWarnings("finally")
    public int risky() {
        try {
            throw new IllegalStateException("boom");
        } finally {
            return -1;
        }
    }

    // BUG L25: identical branches
    public int fee(boolean premium) {
        if (premium) {
            return 10;
        } else {
            return 10;
        }
    }

    // BUG L26: String.format argument count mismatch
    public String describe(String name, int age) {
        return String.format("%s is %d years old and lives in %s", name, age);
    }

    // BUG L27: infinite recursion
    public int factorial(int n) {
        return n * factorial(n - 1);
    }

    // BUG L28: oddness check fails for negative numbers
    public boolean isOdd(int n) {
        return n % 2 == 1;
    }

    // BUG L29: List<String>.contains(Integer) - can never be true
    public boolean hasCode(List<String> codes, Integer code) {
        return codes.contains(code);
    }

    // BUG L30: boxed Integer compared with == (fails above 127)
    public boolean sameCount(Integer a, Integer b) {
        return a == b;
    }

    // BUG L31: InterruptedException swallowed, interrupt flag lost
    public void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    // BUG L32: File.delete() result ignored
    public void cleanup(File f) {
        f.delete();
    }

    // BUG L33: dead store - computed value overwritten
    public int discount(int price) {
        int d = price / 10;
        d = 5;
        return d;
    }

    // BUG L34: Map.get() result dereferenced without null check
    public int lengthOf(Map<String, String> map, String key) {
        return map.get(key).length();
    }

    // BUG L35: loop condition never changes - infinite loop
    public int waitForReady(boolean ready) {
        int spins = 0;
        while (!ready) {
            spins++;
        }
        return spins;
    }

    // BUG L36: collection created with wrong initial content (adds to itself)
    public List<Object> selfContaining() {
        List<Object> l = new ArrayList<>();
        l.add(l);
        return l;
    }

    public String today() {
        return FMT.format(new Date());
    }
}
