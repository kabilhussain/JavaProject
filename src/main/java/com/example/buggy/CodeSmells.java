package com.example.buggy;

// BUG C1: wildcard import
import java.util.*;
// BUG C2: unused imports
import java.io.Serializable;
import java.net.URL;

/** Maintainability problems / code smells. Each planted bug is tagged "BUG Cn". */
public class CodeSmells {

    // BUG C3: public mutable static field
    public static List<String> cache = new ArrayList<>();

    // BUG C4: constant not named in UPPER_CASE
    private static final int maxRetries = 3;

    // BUG C5: unused private field
    private String unusedField = "never read";

    // BUG C6: method name not in camelCase
    public int Compute_Total(int a, int b) {
        return a + b;
    }

    // BUG C7: too many parameters
    public String buildAddress(String street, String number, String city, String zip,
                               String state, String country, String floor, String unit) {
        return street + number + city + zip + state + country + floor + unit;
    }

    // BUG C8: deep nesting / high cognitive complexity
    public String classify(int a, int b, int c, boolean x, boolean y) {
        if (a > 0) {
            if (b > 0) {
                if (c > 0) {
                    if (x) {
                        if (y) {
                            return "all";
                        } else {
                            for (int i = 0; i < a; i++) {
                                if (i % 2 == 0 && b > i || c < i && x) {
                                    return "loop";
                                }
                            }
                        }
                    } else if (y) {
                        return "y";
                    }
                } else if (c < -10) {
                    return "neg-c";
                }
            } else {
                while (b < 0) {
                    b++;
                    if (b == -5) {
                        return "b5";
                    }
                }
            }
        }
        return "none";
    }

    // BUG C9: duplicated code block (1 of 3)
    public double invoiceTotalA(List<Double> prices, double taxRate, double shipping) {
        double subtotal = 0;
        for (Double p : prices) {
            if (p != null && p > 0) {
                subtotal += p;
            }
        }
        double tax = subtotal * taxRate;
        double total = subtotal + tax;
        if (total > 100) {
            total = total - shipping;
        } else {
            total = total + shipping;
        }
        System.out.println("Subtotal: " + subtotal + " tax: " + tax + " total: " + total);
        return Math.round(total * 100.0) / 100.0;
    }

    // BUG C10: duplicated code block (2 of 3)
    public double invoiceTotalB(List<Double> prices, double taxRate, double shipping) {
        double subtotal = 0;
        for (Double p : prices) {
            if (p != null && p > 0) {
                subtotal += p;
            }
        }
        double tax = subtotal * taxRate;
        double total = subtotal + tax;
        if (total > 100) {
            total = total - shipping;
        } else {
            total = total + shipping;
        }
        System.out.println("Subtotal: " + subtotal + " tax: " + tax + " total: " + total);
        return Math.round(total * 100.0) / 100.0;
    }

    // BUG C11: duplicated code block (3 of 3)
    public double invoiceTotalC(List<Double> prices, double taxRate, double shipping) {
        double subtotal = 0;
        for (Double p : prices) {
            if (p != null && p > 0) {
                subtotal += p;
            }
        }
        double tax = subtotal * taxRate;
        double total = subtotal + tax;
        if (total > 100) {
            total = total - shipping;
        } else {
            total = total + shipping;
        }
        System.out.println("Subtotal: " + subtotal + " tax: " + tax + " total: " + total);
        return Math.round(total * 100.0) / 100.0;
    }

    // BUG C12: magic numbers
    public double shippingCost(double weight) {
        return weight * 4.37 + 12.5 + (weight > 23 ? 17 : 0);
    }

    // BUG C13: TODO / FIXME left in code
    public void process() {
        // TODO: implement processing
        // FIXME: this is broken
    }

    // BUG C14: commented-out code
    public int legacy(int x) {
        // int y = x * 2;
        // if (y > 10) { return y; }
        // System.out.println(y);
        return x;
    }

    // BUG C15: overly long line
    public String banner() { return "This is a very long line that goes well past any reasonable line-length limit configured in a style checker, for sure"; }

    // BUG C16: System.out / printStackTrace used for logging
    public void report(Exception e) {
        System.out.println("Error happened");
        e.printStackTrace();
    }

    // BUG C17: catching generic Exception
    public int risky(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }

    // BUG C18: raw types
    public List rawList() {
        List l = new ArrayList();
        l.add("x");
        return l;
    }

    // BUG C19: redundant boolean literal comparison
    public boolean isEnabled(boolean enabled) {
        if (enabled == true) {
            return true;
        } else {
            return false;
        }
    }

    // BUG C20: string concatenation in a loop
    public String joinAll(List<String> parts) {
        String s = "";
        for (String p : parts) {
            s += p + ",";
        }
        return s;
    }

    // BUG C21: deprecated boxing constructor
    @SuppressWarnings("removal")
    public Integer boxed(int v) {
        return new Integer(v);
    }

    // BUG C22: empty method body without explanation
    public void onEvent() {
    }

    // BUG C23: unused private method
    private int helper(int v) {
        return v * maxRetries;
    }

    // BUG C24: too many return statements
    public int score(String grade) {
        if ("A".equals(grade)) {
            return 4;
        }
        if ("B".equals(grade)) {
            return 3;
        }
        if ("C".equals(grade)) {
            return 2;
        }
        if ("D".equals(grade)) {
            return 1;
        }
        if ("E".equals(grade)) {
            return 0;
        }
        return -1;
    }

    // BUG C25: switch without default
    public String dayType(int day) {
        String t = "unknown";
        switch (day) {
            case 6:
                t = "weekend";
                break;
            case 7:
                t = "weekend";
                break;
        }
        return t;
    }

    // BUG C26: unused local variable and unused parameter
    public int area(int w, int h, int depth) {
        int unused = 42;
        return w * h;
    }

    // BUG C27: collapsible if statements
    public boolean inRange(int v) {
        if (v > 0) {
            if (v < 100) {
                return true;
            }
        }
        return false;
    }
}
